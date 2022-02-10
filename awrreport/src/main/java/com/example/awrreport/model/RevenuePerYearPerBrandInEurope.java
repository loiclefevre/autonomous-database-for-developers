package com.example.awrreport.model;

import java.math.BigDecimal;

public record RevenuePerYearPerBrandInEurope(BigDecimal revenue, int year, String brand) {
}
