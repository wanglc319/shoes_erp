package com.shoerp.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 单据编号生成器：{前缀}{yyyyMMdd}{4 位流水}。
 * 流水号取自数据库当日最大值，避免应用重启后与历史单号冲突。
 */
@Component
public class DocumentNoGenerator {

    private static final Logger log = LoggerFactory.getLogger(DocumentNoGenerator.class);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JdbcTemplate jdbcTemplate;

    public DocumentNoGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private String next(String prefix, String table, String column) {
        String datePart = LocalDate.now().format(DATE_FORMAT);
        String pattern = prefix + datePart + "%";
        int start = prefix.length() + datePart.length() + 1;
        int sequence = 1;
        try {
            Integer max = jdbcTemplate.queryForObject(
                    "SELECT MAX(CAST(SUBSTRING(" + column + " FROM " + start + ") AS UNSIGNED)) FROM " + table
                            + " WHERE " + column + " LIKE ?",
                    Integer.class, pattern);
            if (max != null) {
                sequence = max + 1;
            }
        } catch (Exception ex) {
            log.warn("Fallback sequence for {} due to {}", prefix, ex.getMessage());
        }
        return prefix + datePart + String.format("%04d", sequence);
    }

    public synchronized String nextQuotationNo() {
        return next("QT", "quotation", "quote_no");
    }

    public synchronized String nextOrderNo() {
        return next("SO", "sales_order", "order_no");
    }

    public synchronized String nextShipmentNo() {
        return next("SH", "shipment", "shipment_no");
    }

    public synchronized String nextSupplierCode() {
        return next("SUP", "supplier", "code");
    }

    public synchronized String nextCustomerCode() {
        return next("CUS", "customer", "code");
    }

    public synchronized String nextPurchaseNo() {
        return next("PO", "purchase_order", "purchase_no");
    }

    public synchronized String nextPaymentNo() {
        return next("RC", "payment", "payment_no");
    }

    public synchronized String nextInquiryNo() {
        return next("RFQ", "inquiry", "inquiry_no");
    }
}
