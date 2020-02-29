package net.d53dev.geocodecache;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;
import net.d53dev.geocodecache.config.DatabaseConfig;
import net.d53dev.geocodecache.persistence.DatabaseVerticle;

@Log4j2
public class MainVerticle extends AbstractVerticle {

	private static ConfigRetrieverOptions getConfigRetrieverOptions() {
		JsonObject classpathFileConfiguration = new JsonObject().put("path", "default.properties");
		ConfigStoreOptions classpathFile = new ConfigStoreOptions().setType("file")
																   .setFormat("properties")
																   .setConfig(classpathFileConfiguration);

		JsonArray envVarKeys = new JsonArray();
		for (String key : DatabaseConfig.values) {
			envVarKeys.add(key);
		}
		JsonObject envVarConfiguration = new JsonObject().put("keys", envVarKeys);
		ConfigStoreOptions environment = new ConfigStoreOptions()
				.setType("env")
				.setConfig(envVarConfiguration)
				.setOptional(true);

		JsonObject envFileConfiguration = new JsonObject().put("path", "/etc/geocache/.env");
		ConfigStoreOptions envFile = new ConfigStoreOptions().setType("file")
															 .setFormat("properties")
															 .setConfig(envFileConfiguration)
															 .setOptional(true);

		return new ConfigRetrieverOptions().addStore(classpathFile)
										   .addStore(envFile)
										   .addStore(environment)
										   .setScanPeriod(5000);
	}

	@Override
	public void start(Promise<Void> promise) throws Exception {
		var retrieverOpts = getConfigRetrieverOptions();
		ConfigRetriever retriever = ConfigRetriever.create(vertx, retrieverOpts);

		retriever.getConfig(confReadyHandler -> {
			if (confReadyHandler.failed()) {
				throw new IllegalStateException("Couldn't load configuration.");
			}
			JsonObject config = confReadyHandler.result();
			Promise<String> dbVerticleDeployment = Promise.promise();

			log.info("Starting storage verticle");
			vertx.deployVerticle(new DatabaseVerticle(), new DeploymentOptions().setConfig(config),
					dbVerticleDeployment);

			dbVerticleDeployment.future().compose(id -> {
				log.info("Starting http verticle");
				Promise<String> httpVerticleDeployment = Promise.promise();
				Promise<String> analyticsVerticleDeployment = Promise.promise();
				vertx.deployVerticle("net.d53dev.geocache.http.HttpServerVerticle",
						new DeploymentOptions().setInstances(1).setConfig(config), httpVerticleDeployment);

				return CompositeFuture.all(httpVerticleDeployment.future(), analyticsVerticleDeployment.future());

			}).setHandler(ar -> {
				if (ar.succeeded()) {
					log.info("Startup complete");
					promise.complete();
				} else {
					promise.fail(ar.cause());
				}
			});
		});
	}
}
