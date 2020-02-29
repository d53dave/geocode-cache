package net.d53dev.geocodecache.persistence;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;

@ProxyGen
@VertxGen
public  interface DatabaseService {
	@GenIgnore
	static DatabaseService create(PgPool pool, Handler<AsyncResult<DatabaseService>> readyHandler) {
		return new DatabaseServiceImpl(pool, readyHandler);
	}

	@GenIgnore
	static DatabaseService createProxy(Vertx vertx, String address) {
		return new DatabaseServiceVertxEBProxy(vertx, address);
	}

	@Fluent
	DatabaseService getJson(String key, Handler<AsyncResult<JsonObject>> resultHandler);
}
