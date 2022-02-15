package com.example.autorest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ORDSOAuth2Token(@JsonProperty("access_token") String token,
							  @JsonProperty("token_type") String type,
							  @JsonProperty("expires_in") int expiresInSeconds) {
}
