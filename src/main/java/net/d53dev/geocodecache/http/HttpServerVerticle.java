package net.d53dev.geocodecache.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.impl.LRUCache;
import lombok.extern.log4j.Log4j2;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public class HttpServerVerticle extends AbstractVerticle {
	public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
	public static final String JSON_CONTENT_TYPE = "application/json";
	private static final LRUCache<String, JsonObject> locationCache = new LRUCache<>(2048);

	@Override
	public void start(Promise<Void> promise) {

		HttpServer server = vertx.createHttpServer();

		HTTPRequestValidationHandler validationHandler = HTTPRequestValidationHandler.create()
																					 .addJsonBodySchema(
																							 RequestSchema.SCHEMA);

		Router router = Router.router(vertx);
		// Routes that need the Request body need to be after this route
		router.route().handler(BodyHandler.create());
		router.post("")
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
		var body = context.getBodyAsJson();
		JsonArray strings = body.getJsonArray("strings");

		List<Future<JsonObject>> futures = strings.stream().map(Object::toString).map(key -> {
			if (locationCache.containsKey(key)) {
				return Future.succeededFuture(locationCache.get(key));
			} else {
				return Future.<JsonObject>failedFuture("Not implemented yet");
			}
		}).collect(Collectors.toList());

		CompositeFuture.all(Collections.unmodifiableList(futures))
					   .onComplete(ar -> context.response()
												.setStatusCode(200)
												.end(buildResult(ar.result().list()).toBuffer()));
	}
}
