package net.d53dev.geocodecache.persistence;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.extern.slf4j.Slf4j;


@Slf4j
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
	public DatabaseService getJson(String id, Handler<AsyncResult<JsonObject>> resultHandler) {
		pool.getConnection(ar -> {
			if (ar.succeeded()) {
				var connection = ar.result();
				var sql = "SELECT full_json FROM geocoding WHERE id=$1";
				connection.preparedQuery(sql, Tuple.of(id), dbResult -> {
					if (dbResult.failed()) {
						resultHandler.handle(Future.failedFuture(dbResult.cause()));
					} else {
						var rows = dbResult.result();
						if (rows.rowCount() < 1) {
							resultHandler.handle(Future.succeededFuture());
						} else {
							for (Row row : rows) {
								resultHandler.handle(Future.succeededFuture(row.get(JsonObject.class, 0)));
								break;
							}
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

	@Override
	public DatabaseService writeJson(String id, JsonObject json, Handler<AsyncResult<Void>> resultHandler) {
		pool.getConnection(ar -> {
			if (ar.succeeded()) {
				var connection = ar.result();
				var sql = "INSERT INTO geocoding (id, full_json) VALUES ($1, $2) ON CONFLICT (id) DO UPDATE SET full_json = EXCLUDED.full_json";
				connection.preparedQuery(sql, Tuple.of(id, json), result -> {
					if (result.failed()) {
						resultHandler.handle(Future.failedFuture(result.cause()));
					} else {
						resultHandler.handle(Future.succeededFuture());
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
