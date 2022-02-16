package com.example.geojson.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FeatureProperties(@JsonProperty("ADMIN") String country, @JsonProperty("ISO_A3") String id) {
}
