package net.d53dev.geocodecache.http;

public class RequestSchema {

	// Autoformat screwed this up
	public static final String SCHEMA =
			"{\n" + "  \"$schema\": \"http://json-schema.org/draft-04/schema#\",\n" + "  \"type\": \"object\",\n"
					+ "  \"properties\": {\n" + "    \"strings\": {\n" + "      \"type\": \"array\",\n"
					+ "      \"items\": [\n" + "        {\n" + "          \"type\": \"string\"\n" + "        },\n"
					+ "        {\n" + "          \"type\": \"string\"\n" + "        }\n" + "      ]\n" + "    }\n"
					+ "  },\n" + "  \"required\": [\n" + "    \"strings\"\n" + "  ]\n" + "}";

}
