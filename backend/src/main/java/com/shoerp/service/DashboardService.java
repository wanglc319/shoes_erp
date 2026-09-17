package com.shoerp.service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
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
import com.shoerp.entity.SalesOrder;
import com.shoerp.entity.Shipment;
import com.shoerp.repository.CustomerRepository;
import com.shoerp.repository.QuotationRepository;
import com.shoerp.repository.SalesOrderRepository;
import com.shoerp.repository.ShipmentRepository;
import com.shoerp.repository.ShoeRepository;
import com.shoerp.repository.SupplierRepository;

@Service
public class DashboardService {

    private final ShoeRepository shoeRepository;

    private final SupplierRepository supplierRepository;

    private final CustomerRepository customerRepository;

    private final QuotationRepository quotationRepository;

    private final SalesOrderRepository orderRepository;

    private final ShipmentRepository shipmentRepository;

    public DashboardService(ShoeRepository shoeRepository, SupplierRepository supplierRepository,
                            CustomerRepository customerRepository, QuotationRepository quotationRepository,
                            SalesOrderRepository orderRepository, ShipmentRepository shipmentRepository) {
        this.shoeRepository = shoeRepository;
        this.supplierRepository = supplierRepository;
        this.customerRepository = customerRepository;
        this.quotationRepository = quotationRepository;
        this.orderRepository = orderRepository;
        this.shipmentRepository = shipmentRepository;
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
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new DashboardStats.CountryAmount(e.getKey(), e.getValue()))
                .collect(Collectors.toList()));
        return stats;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
