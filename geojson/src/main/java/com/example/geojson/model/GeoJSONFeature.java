package com.example.geojson.model;

import com.example.geojson.json.GeometryDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class GeoJSONFeature {
	private String type;

	private FeatureProperties properties;

	@JsonDeserialize(using = GeometryDeserializer.class)
	private String geometry;

	public GeoJSONFeature() {
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public FeatureProperties getProperties() {
		return properties;
	}

	public void setProperties(FeatureProperties properties) {
		this.properties = properties;
	}

	public String getGeometry() {
		return geometry;
	}

	public void setGeometry(String geometry) {
		this.geometry = geometry;
	}
}
