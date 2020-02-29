package net.d53dev.geocodecache.geoclient;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.PendingResult;
import com.google.maps.model.GeocodingResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GoogleGeolocationClient {

	private static GoogleGeolocationClient INSTANCE;
	private GeoApiContext context;

	public static final String API_KEY_KEY = "MAPS_API_KEY";

	public static GoogleGeolocationClient getInstance(JsonObject config) {
		if (INSTANCE == null) {
			INSTANCE = new GoogleGeolocationClient(config);
		}
		return INSTANCE;
	}

	private GoogleGeolocationClient(JsonObject config) {
		context = new GeoApiContext.Builder().apiKey(config.getString(API_KEY_KEY)).build();
	}

	public void resolveLocationKey(String id, Handler<AsyncResult<JsonObject>> resultHandler) {
		GeocodingApi.geocode(context, id).setCallback(new PendingResult.Callback<>() {
			@Override
			public void onResult(GeocodingResult[] result) {
				var resultArray = new JsonArray();
				for(var res: result) {
					resultArray.add(JsonObject.mapFrom(res));
				}
				resultHandler.handle(Future.succeededFuture(new JsonObject().put(id, resultArray)));
			}

			@Override
			public void onFailure(Throwable e) {
				resultHandler.handle(Future.failedFuture(e));
			}
		});
	}
}
