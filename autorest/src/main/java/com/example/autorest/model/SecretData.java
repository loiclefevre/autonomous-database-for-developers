package com.example.autorest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SecretData(@JsonProperty("country_name") String countryName) {
}
