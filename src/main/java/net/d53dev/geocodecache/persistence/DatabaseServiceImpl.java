package net.d53dev.geocodecache.persistence;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.extern.log4j.Log4j2;

import java.util.NoSuchElementException;

@Log4j2
public class DatabaseServiceImpl implements DatabaseService {
	private final PgPool pool;

	DatabaseServiceImpl(PgPool pool, Handler<AsyncResult<DatabaseService>> readyHandler) {
		this.pool = pool;

		pool.getConnection(ar -> {
			if (ar.failed()) {
				log.error("Could not open database connection", ar.cause());
				readyHandler.handle(Future.failedFuture(ar.cause()));
			} else {
				var connection = ar.result();
				connection.query("SELECT 1", sqlAr -> {
					connection.close();
					if (sqlAr.failed()) {
						log.error("Database preparation error: could not run query.", sqlAr.cause());
						readyHandler.handle(Future.failedFuture(sqlAr.cause()));
					} else {
						readyHandler.handle(Future.succeededFuture(this));
					}
				});
			}
		});
	}

	@Override
	public DatabaseService getJson(String key, Handler<AsyncResult<JsonObject>> resultHandler) {
		pool.getConnection(ar -> {
			if (ar.succeeded()) {
				var connection = ar.result();
				var sql = "SELECT full_json FROM geolocation WHERE id=$1";
				log.info("Running query: {}", sql);
				connection.preparedQuery(sql, Tuple.of(key), result -> {
					if (result.failed()) {
						resultHandler.handle(Future.failedFuture(result.cause()));
					} else {
						var rows = result.result();
						if (rows.rowCount() < 1) {
							resultHandler.handle(Future.failedFuture(new NoSuchElementException()));
						}
						for (Row row : rows) {
							resultHandler.handle(Future.succeededFuture(row.get(JsonObject.class, 0)));
							break;
						}
					}
					connection.close();
				});
			} else {
				log.error("Could not acquire connection", ar.cause());
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
		return this;
	}
}
