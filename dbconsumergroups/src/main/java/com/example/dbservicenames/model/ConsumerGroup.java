package com.example.dbservicenames.model;

public record ConsumerGroup(String name, int concurrencyLimit, Integer degreeOfParallelism,
							Integer elapsedTimeLimitInSeconds, Integer IOMegabytesLimit) {}
