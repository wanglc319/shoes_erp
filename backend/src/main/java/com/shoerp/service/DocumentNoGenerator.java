package com.shoerp.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

@Component
public class DocumentNoGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AtomicInteger sequence = new AtomicInteger(1);

    private String datePart() {
        return LocalDate.now().format(DATE_FORMAT);
    }

    private String next(String prefix) {
        return prefix + datePart() + String.format("%04d", sequence.getAndIncrement());
    }

    public String nextQuotationNo() {
        return next("QT");
    }

    public String nextOrderNo() {
        return next("SO");
    }

    public String nextShipmentNo() {
        return next("SH");
    }

    public String nextSupplierCode() {
        return next("SUP");
    }

    public String nextCustomerCode() {
        return next("CUS");
    }
}
