package net.d53dev.geocodecache.http;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.impl.LRUCache;
import lombok.extern.slf4j.Slf4j;
import net.d53dev.geocodecache.geoclient.GoogleGeolocationClient;
import net.d53dev.geocodecache.persistence.DatabaseService;
import net.d53dev.geocodecache.persistence.DatabaseVerticle;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
public class HttpServerVerticle extends AbstractVerticle {
	public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
	public static final String JSON_CONTENT_TYPE = "application/json";
	public static final String STATS_PRINT_INTERVAL = "STATS_PRINT_INTERVAL";
	private static final LRUCache<String, JsonObject> locationCache = new LRUCache<>(1024);

	GoogleGeolocationClient geolocationClient;
	DatabaseService databaseService;
	Stats stats;

	@Override
	public void start(Promise<Void> promise) {
		stats = new Stats(config().getInteger(STATS_PRINT_INTERVAL, 1000));
		geolocationClient = GoogleGeolocationClient.getInstance(config());
		log.info("Getting DatabaseServiceProxy@{}", DatabaseVerticle.ADDRESS);
		databaseService = DatabaseService.createProxy(vertx, DatabaseVerticle.ADDRESS);
		HttpServer server = vertx.createHttpServer();

		HTTPRequestValidationHandler validationHandler = HTTPRequestValidationHandler.create()
																					 .addJsonBodySchema(
																							 RequestSchema.SCHEMA);

		Router router = Router.router(vertx);
		// Routes that need the Request body need to be after this route
		router.route().handler(BodyHandler.create());
		router.post("/geocode")
			  .consumes(JSON_CONTENT_TYPE)
			  .produces(JSON_CONTENT_TYPE)
			  .handler(validationHandler)
			  .handler(this::geocode);

		int portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT, 8888);
		server.requestHandler(router).listen(portNumber, ar -> {
			if (ar.succeeded()) {
				log.info("HTTP server running on port " + portNumber);
				promise.complete();
			} else {
				promise.fail(ar.cause());
			}
		});
	}

	private JsonObject buildResult(List<JsonObject> locations) {
		return new JsonObject().put("locations", locations);
	}

	private void geocode(RoutingContext context) {
		stats.registerRequest();
		var body = context.getBodyAsJson();
		JsonArray strings = body.getJsonArray("strings");

		List<Future<JsonObject>> futures = strings.stream().map(Object::toString).map(id -> {
			Promise<JsonObject> p = Promise.promise();
			if (locationCache.containsKey(id)) {
				log.debug("Cache hit for '{}'", id);
				stats.registerCacheHit();
				p.complete(locationCache.get(id));
			} else {
				databaseService.getJson(id, databaseResult -> {
					if (databaseResult.succeeded()) {
						if (databaseResult.result() != null) {
							log.debug("DB hit for '{}'", id);
							stats.registerDbHit();
							var res = databaseResult.result();
							locationCache.put(id, res);
							p.complete(res);
						} else {
							log.debug("Getting '{}' from Geocoding API", id);
							stats.registerApiHit();
							geolocationClient.resolveLocationKey(id, geocodeResult -> {
								if (geocodeResult.succeeded()) {
									locationCache.put(id, geocodeResult.result());
									databaseService.writeJson(id, geocodeResult.result(), insertResult -> {
										if (insertResult.failed()) {
											log.error("Database insert failed", insertResult.cause());
											stats.registerDbError();
										}
									});
								} else {
									log.error("Geocoding lookup failed.", geocodeResult.cause());
									stats.registerApiError();
								}
								p.complete(geocodeResult.result());
							});
						}
					} else {
						log.error("Could not get Geolocation from database for id: " + id, databaseResult.cause());
						stats.registerDbError();
					}
				});
			}
			return p.future();
		}).collect(Collectors.toList());

		log.debug("Waiting for {} futures.", futures.size());
		CompositeFuture.all(Collections.unmodifiableList(futures))
					   .onSuccess(ar -> context.response()
											   .setStatusCode(200)
											   .putHeader(HttpHeaders.CONTENT_TYPE, JSON_CONTENT_TYPE)
											   .end(buildResult(ar.result().list()).toBuffer()))
					   .onFailure(failure -> {
						   log.error("Returning 500 due to unhandled error. ", failure.getCause());
						   context.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
					   });
	}
}
