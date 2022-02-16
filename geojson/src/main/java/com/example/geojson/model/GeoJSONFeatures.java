package com.example.geojson.model;

import java.util.List;

public record GeoJSONFeatures(String type, List<GeoJSONFeature> features ) {
}
