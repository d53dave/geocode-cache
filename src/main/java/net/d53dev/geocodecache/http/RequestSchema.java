package net.d53dev.geocodecache.http;

public class RequestSchema {

	// Autoformat screwed this up
	public static final String SCHEMA =
			"{\n" + "  \"definitions\": {},\n" + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n"
					+ "  \"$id\": \"http://example.com/root.json\",\n" + "  \"type\": \"object\",\n"
					+ "  \"title\": \"The Root Schema\",\n" + "  \"required\": [\n" + "    \"strings\"\n" + "  ],\n"
					+ "  \"properties\": {\n" + "    \"strings\": {\n" + "      \"$id\": \"#/properties/strings\",\n"
					+ "      \"type\": \"array\",\n" + "      \"title\": \"The Strings Schema\",\n"
					+ "      \"items\": {\n" + "        \"$id\": \"#/properties/strings/items\",\n"
					+ "        \"type\": \"string\",\n" + "        \"title\": \"The Items Schema\",\n"
					+ "        \"default\": \"\",\n" + "        \"examples\": [\n"
					+ "          \"San Francisco, CA\",\n" + "          \"Paris, France\"\n" + "        ],\n"
					+ "        \"pattern\": \"^(.*)$\"\n" + "      }\n" + "    }\n" + "  }\n" + "}\n" + "\n" + "\n";
}
