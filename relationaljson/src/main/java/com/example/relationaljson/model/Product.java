package com.example.relationaljson.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record Product(@JsonProperty("prod_id") String id,
					  String name,
					  BigDecimal price) {}
