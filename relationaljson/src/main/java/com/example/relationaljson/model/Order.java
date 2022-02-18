package com.example.relationaljson.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigInteger;

public record Order(@JsonProperty("order_id") BigInteger id,
					@JsonProperty("customer_id") BigInteger customerId,
					Product[] products) {}
