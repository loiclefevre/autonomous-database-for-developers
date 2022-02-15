package com.example.autorest.model;

import java.util.List;

public record PageOfSecretData(List<SecretData> items,
							   boolean hasMore,
							   int limit,
							   int offset,
							   int count,
							   List<Link> links) {
}
