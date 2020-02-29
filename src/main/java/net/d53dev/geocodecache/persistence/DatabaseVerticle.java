package net.d53dev.geocodecache.persistence;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.pgclient.SslMode;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import lombok.extern.log4j.Log4j2;
import net.d53dev.geocodecache.config.DatabaseConfig;

@Log4j2
public class DatabaseVerticle extends AbstractVerticle {

	public static final String ADDRESS = "geocode-storage";

	@Override
	public void start(Promise<Void> promise) {
		var conf = config();
		log.debug("Connecting to database with conf: {}", conf);

		var connectOptions = new PgConnectOptions();
		var poolOptions = new PoolOptions();
		try {
			connectOptions
					.setPort(conf.getInteger(DatabaseConfig.PORT))
					.setHost(conf.getString(DatabaseConfig.HOSTNAME))
					.setDatabase(conf.getString(DatabaseConfig.DATABASE))
					.setUser(conf.getString(DatabaseConfig.USER))
					.setPassword(conf.getString(DatabaseConfig.PASSWORD));

			if (conf.getBoolean(DatabaseConfig.SSL)) {
				connectOptions = connectOptions.setSslMode(SslMode.REQUIRE);
			}

			poolOptions.setMaxSize(conf.getInteger(DatabaseConfig.POOL_SIZE));
		} catch (NullPointerException e) {
			throw new NullPointerException("Failed reading database credentials.");
		}

		var client = PgPool.pool(vertx, connectOptions, poolOptions);

		DatabaseService.create(client, ready -> {
			if (ready.succeeded()) {
				log.info("ListingService created successfully");
				new ServiceBinder(vertx)
						.setAddress(ADDRESS)
						.register(DatabaseService.class, ready.result());
				promise.complete();
			} else {
				log.error("Failed to create DatabaseService", ready.cause());
				promise.fail(ready.cause());
			}
		});
	}
}
