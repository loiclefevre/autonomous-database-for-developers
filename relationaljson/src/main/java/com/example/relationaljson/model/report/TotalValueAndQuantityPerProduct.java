package com.example.relationaljson.model.report;

import java.math.BigDecimal;

public record TotalValueAndQuantityPerProduct(String productId, String productName, BigDecimal totalValue, int quantity) {
}
