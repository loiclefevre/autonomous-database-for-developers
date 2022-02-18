package com.example.relationaljson.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Device(@JsonProperty("id_machine") String id,
					 String country,
					 String state,
					 String city,
					 Geometry geometry) {
}
