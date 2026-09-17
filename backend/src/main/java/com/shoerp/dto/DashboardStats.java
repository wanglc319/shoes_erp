package com.shoerp.dto;

import lombok.Data;

@Data
public class DashboardStats {

    private long shoeCount;

    private long supplierCount;

    private long customerCount;

    private long quotationCount;

    private long orderCount;

    private long shipmentCount;

    private long pendingQuotationCount;

    private long pendingOrderCount;

    private java.math.BigDecimal totalOrderAmount = java.math.BigDecimal.ZERO;

    private java.math.BigDecimal totalShippedAmount = java.math.BigDecimal.ZERO;

    private java.math.BigDecimal totalProfit = java.math.BigDecimal.ZERO;

    private java.util.List<StatusCount> orderStatusDistribution = new java.util.ArrayList<>();

    private java.util.List<MonthlyAmount> monthlyTrend = new java.util.ArrayList<>();

    private java.util.List<TopProduct> topProducts = new java.util.ArrayList<>();

    private java.util.List<CountryAmount> countryDistribution = new java.util.ArrayList<>();

    @Data
    public static class StatusCount {
        private String status;
        private long count;

        public StatusCount(String status, long count) {
            this.status = status;
            this.count = count;
        }
    }

    @Data
    public static class MonthlyAmount {
        private String month;
        private java.math.BigDecimal amount;

        public MonthlyAmount(String month, java.math.BigDecimal amount) {
            this.month = month;
            this.amount = amount;
        }
    }

    @Data
    public static class TopProduct {
        private String styleNo;
        private String productName;
        private long quantity;
        private java.math.BigDecimal amount;

        public TopProduct(String styleNo, String productName, long quantity, java.math.BigDecimal amount) {
            this.styleNo = styleNo;
            this.productName = productName;
            this.quantity = quantity;
            this.amount = amount;
        }
    }

    @Data
    public static class CountryAmount {
        private String country;
        private java.math.BigDecimal amount;

        public CountryAmount(String country, java.math.BigDecimal amount) {
            this.country = country;
            this.amount = amount;
        }
    }
}
