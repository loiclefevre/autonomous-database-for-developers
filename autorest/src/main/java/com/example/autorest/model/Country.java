package com.example.autorest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Country(@JsonProperty("country_id") String id,
					  @JsonProperty("country_code") String code,
					  String name,
					  @JsonProperty("official_name") String officialName,
					  long population,
					  @JsonProperty("area_sq_km") long areaSqKm,
					  double latitude,
					  double longitude,
					  String timezone,
					  @JsonProperty("region_id") String regionId,
					  List<Link> links ) {
}
