package com.shoerp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoerp.dto.DashboardStats;
import com.shoerp.entity.Customer;
import com.shoerp.entity.OrderItem;
import com.shoerp.entity.Payment;
import com.shoerp.entity.Quotation;
import com.shoerp.entity.SalesOrder;
import com.shoerp.entity.Shipment;
import com.shoerp.repository.CustomerRepository;
import com.shoerp.repository.PaymentRepository;
import com.shoerp.repository.QuotationRepository;
import com.shoerp.repository.SalesOrderRepository;
import com.shoerp.repository.ShipmentRepository;
import com.shoerp.repository.ShoeRepository;
import com.shoerp.repository.SupplierRepository;

@Service
public class DashboardService {

    private static final List<String> ACTIVE_ORDER_STATUS = List.of("PENDING", "CONFIRMED", "PARTIAL_SHIPPED");

    private final ShoeRepository shoeRepository;

    private final SupplierRepository supplierRepository;

    private final CustomerRepository customerRepository;

    private final QuotationRepository quotationRepository;

    private final SalesOrderRepository orderRepository;

    private final ShipmentRepository shipmentRepository;

    private final PaymentRepository paymentRepository;

    public DashboardService(ShoeRepository shoeRepository, SupplierRepository supplierRepository,
                            CustomerRepository customerRepository, QuotationRepository quotationRepository,
                            SalesOrderRepository orderRepository, ShipmentRepository shipmentRepository,
                            PaymentRepository paymentRepository) {
        this.shoeRepository = shoeRepository;
        this.supplierRepository = supplierRepository;
        this.customerRepository = customerRepository;
        this.quotationRepository = quotationRepository;
        this.orderRepository = orderRepository;
        this.shipmentRepository = shipmentRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStats stats() {
        DashboardStats stats = new DashboardStats();
        stats.setShoeCount(shoeRepository.count());
        stats.setSupplierCount(supplierRepository.count());
        stats.setCustomerCount(customerRepository.count());
        stats.setQuotationCount(quotationRepository.count());
        stats.setOrderCount(orderRepository.count());
        stats.setShipmentCount(shipmentRepository.count());
        stats.setPendingQuotationCount(quotationRepository.countByStatus("DRAFT")
                + quotationRepository.countByStatus("SENT"));
        stats.setPendingOrderCount(orderRepository.countByStatus("PENDING")
                + orderRepository.countByStatus("CONFIRMED")
                + orderRepository.countByStatus("PARTIAL_SHIPPED"));

        List<SalesOrder> orders = orderRepository.findAll();
        orders.forEach(o -> o.getItems().size());

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        Map<String, Long> statusCount = new HashMap<>();
        Map<String, BigDecimal> monthly = new HashMap<>();
        Map<String, DashboardStats.TopProduct> productMap = new HashMap<>();
        Map<String, BigDecimal> countryMap = new HashMap<>();
        Map<Long, Customer> customers = customerRepository.findAll().stream()
                .collect(Collectors.toMap(Customer::getId, c -> c));

        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (SalesOrder order : orders) {
            totalAmount = totalAmount.add(nvl(order.getTotalAmount()));
            statusCount.merge(order.getStatus(), 1L, Long::sum);
            if (order.getOrderDate() != null) {
                monthly.merge(order.getOrderDate().format(monthFmt), nvl(order.getTotalAmount()), BigDecimal::add);
            }
            Customer customer = customers.get(order.getCustomerId());
            String country = customer == null || customer.getCountry() == null ? "Unknown" : customer.getCountry();
            countryMap.merge(country, nvl(order.getTotalAmount()), BigDecimal::add);

            for (OrderItem item : order.getItems()) {
                BigDecimal cost = nvl(item.getCostPrice()).multiply(BigDecimal.valueOf(item.getQuantity()));
                totalProfit = totalProfit.add(nvl(item.getAmount()).subtract(cost));
                String key = item.getStyleNo() == null ? item.getProductName() : item.getStyleNo();
                DashboardStats.TopProduct existing = productMap.get(key);
                if (existing == null) {
                    productMap.put(key, new DashboardStats.TopProduct(
                            item.getStyleNo(), item.getProductName(), item.getQuantity(), nvl(item.getAmount())));
                } else {
                    existing.setQuantity(existing.getQuantity() + item.getQuantity());
                    existing.setAmount(existing.getAmount().add(nvl(item.getAmount())));
                }
            }
        }

        List<Shipment> shipments = shipmentRepository.findAll();
        BigDecimal shippedAmount = shipments.stream()
                .map(s -> nvl(s.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.setTotalOrderAmount(totalAmount);
        stats.setTotalShippedAmount(shippedAmount);
        stats.setTotalProfit(totalProfit);
        stats.setOrderStatusDistribution(statusCount.entrySet().stream()
                .map(e -> new DashboardStats.StatusCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(DashboardStats.StatusCount::getStatus))
                .collect(Collectors.toList()));
        stats.setMonthlyTrend(monthly.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new DashboardStats.MonthlyAmount(e.getKey(), e.getValue()))
                .collect(Collectors.toList()));
        stats.setTopProducts(productMap.values().stream()
                .sorted(Comparator.comparing(DashboardStats.TopProduct::getAmount).reversed())
                .limit(8)
                .collect(Collectors.toList()));
        stats.setCountryDistribution(countryMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(e -> new DashboardStats.CountryAmount(e.getKey(), e.getValue()))
                .collect(Collectors.toList()));
        stats.setAlerts(buildAlerts(orders));
        return stats;
    }

    /** Business risk alerts: expiring quotations, overdue/due-soon orders, overdue receivables. */
    private List<DashboardStats.Alert> buildAlerts(List<SalesOrder> orders) {
        List<DashboardStats.Alert> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate dueSoonLimit = today.plusDays(3);
        LocalDate quoteSoonLimit = today.plusDays(7);

        for (Quotation quotation : quotationRepository.findAll()) {
            if (!"DRAFT".equals(quotation.getStatus()) && !"SENT".equals(quotation.getStatus())) {
                continue;
            }
            if (quotation.getValidUntil() == null) {
                continue;
            }
            LocalDate until = quotation.getValidUntil();
            if (!until.isAfter(quoteSoonLimit)) {
                String level = until.isBefore(today) ? "HIGH" : "MEDIUM";
                alerts.add(new DashboardStats.Alert("QUOTATION_EXPIRING", level, quotation.getId(),
                        quotation.getQuoteNo(), quotation.getCustomerName(), until.toString(), null));
            }
        }

        Map<Long, List<Payment>> paymentsByOrder = paymentRepository.findAll().stream()
                .collect(Collectors.groupingBy(Payment::getOrderId));
        for (SalesOrder order : orders) {
            if (ACTIVE_ORDER_STATUS.contains(order.getStatus()) && order.getDeliveryDate() != null) {
                LocalDate delivery = order.getDeliveryDate();
                if (delivery.isBefore(today)) {
                    alerts.add(new DashboardStats.Alert("ORDER_OVERDUE", "HIGH", order.getId(),
                            order.getOrderNo(), order.getCustomerName(), delivery.toString(), null));
                } else if (!delivery.isAfter(dueSoonLimit)) {
                    alerts.add(new DashboardStats.Alert("ORDER_DUE_SOON", "MEDIUM", order.getId(),
                            order.getOrderNo(), order.getCustomerName(), delivery.toString(), null));
                }
            }
            if (!"CANCELLED".equals(order.getStatus()) && order.getDeliveryDate() != null
                    && order.getDeliveryDate().isBefore(today)) {
                BigDecimal received = paymentsByOrder.getOrDefault(order.getId(), List.of()).stream()
                        .map(Payment::getAmount)
                        .filter(a -> a != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal outstanding = nvl(order.getTotalAmount()).subtract(received);
                if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
                    alerts.add(new DashboardStats.Alert("RECEIVABLE_OVERDUE", "HIGH", order.getId(),
                            order.getOrderNo(), order.getCustomerName(), order.getDeliveryDate().toString(),
                            outstanding));
                }
            }
        }

        alerts.sort(Comparator.comparing((DashboardStats.Alert a) -> "HIGH".equals(a.getLevel()) ? 0 : 1)
                .thenComparing(DashboardStats.Alert::getDate));
        return alerts.size() > 30 ? new ArrayList<>(alerts.subList(0, 30)) : alerts;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
