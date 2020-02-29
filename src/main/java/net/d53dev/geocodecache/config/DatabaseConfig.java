package net.d53dev.geocodecache.config;

public class DatabaseConfig {
	public static final String HOSTNAME = "PG_HOST";
	public static final String PORT = "PG_PORT";
	public static final String DATABASE = "PG_DB";
	public static final String USER = "PG_USER";
	public static final String PASSWORD = "PG_PASS";
	public static final String SSL = "PG_SSL";
	public static final String POOL_SIZE = "PG_POOL_SIZE";

	public static final String[] values = new String[] { HOSTNAME, POOL_SIZE, PORT, DATABASE, USER, PASSWORD, SSL };
}
