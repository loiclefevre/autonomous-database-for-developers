package com.example.geojson.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Custom JSON deserializer instantiating a String from a JSonNode.
 *
 * @author Loïc Lefèvre
 */
public class GeometryDeserializer extends StdDeserializer<String> {
	private static final Logger LOG = LoggerFactory.getLogger(GeometryDeserializer.class);

	public GeometryDeserializer() {
		this(null);
	}

	public GeometryDeserializer(Class<?> c) {
		super(c);
	}

	@Override
	public String deserialize(JsonParser jsonparser, DeserializationContext context) throws IOException {
		return jsonparser.readValueAsTree().toString();
	}
}
