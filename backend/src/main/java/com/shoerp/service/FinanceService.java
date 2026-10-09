package com.shoerp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoerp.common.BusinessException;
import com.shoerp.common.PageQuery;
import com.shoerp.common.PageResult;
import com.shoerp.dto.ReceivableRow;
import com.shoerp.entity.Payment;
import com.shoerp.entity.SalesOrder;
import com.shoerp.repository.PaymentRepository;
import com.shoerp.repository.SalesOrderRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class FinanceService {

    private final PaymentRepository paymentRepository;

    private final SalesOrderRepository orderRepository;

    private final DocumentNoGenerator noGenerator;

    public FinanceService(PaymentRepository paymentRepository, SalesOrderRepository orderRepository,
                          DocumentNoGenerator noGenerator) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.noGenerator = noGenerator;
    }

    @Transactional(readOnly = true)
    public PageResult<Payment> page(String keyword, Long orderId, int page, int size) {
        Specification<Payment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("paymentNo"), like),
                        cb.like(root.get("orderNo"), like),
                        cb.like(root.get("customerName"), like)));
            }
            if (orderId != null) {
                predicates.add(cb.equal(root.get("orderId"), orderId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Payment> result = paymentRepository.findAll(spec, PageQuery.of(page, size));
        return PageResult.of(result);
    }

    @Transactional
    public Payment save(Payment payload) {
        Payment payment;
        if (payload.getId() == null) {
            payment = new Payment();
            String no = payload.getPaymentNo();
            if (no == null || no.isBlank()) {
                no = nextAvailableNo();
            } else if (paymentRepository.existsByPaymentNo(no)) {
                throw new BusinessException("Payment number already exists: " + no);
            }
            payment.setPaymentNo(no);
        } else {
            payment = paymentRepository.findById(payload.getId())
                    .orElseThrow(() -> new BusinessException(404, "Payment not found"));
        }
        SalesOrder order = resolveOrder(payload.getOrderId());
        if ("CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("Cannot record payments for a cancelled order");
        }
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setCustomerName(order.getCustomerName());
        payment.setCurrency(order.getCurrency());
        payment.setPaymentDate(payload.getPaymentDate() == null ? LocalDate.now() : payload.getPaymentDate());
        payment.setMethod(payload.getMethod() == null ? "T/T" : payload.getMethod());
        BigDecimal amount = nonNegative(payload.getAmount(), "Amount");
        validatePaymentTotal(order, payment.getId(), amount);
        payment.setAmount(amount);
        payment.setReference(payload.getReference());
        payment.setRemark(payload.getRemark());
        return paymentRepository.save(payment);
    }

    private void validatePaymentTotal(SalesOrder order, Long currentPaymentId, BigDecimal amount) {
        BigDecimal total = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
        BigDecimal received = BigDecimal.ZERO;
        for (Payment existing : paymentRepository.findByOrderIdOrderByIdDesc(order.getId())) {
            if (currentPaymentId != null && currentPaymentId.equals(existing.getId())) {
                continue;
            }
            received = received.add(existing.getAmount() == null ? BigDecimal.ZERO : existing.getAmount());
        }
        if (received.add(amount).compareTo(total) > 0) {
            throw new BusinessException("Payment amount exceeds the order total (" + scale(total)
                    + "); remaining receivable is " + scale(total.subtract(received)));
        }
    }

    @Transactional
    public void delete(Long id) {
        if (!paymentRepository.existsById(id)) {
            throw new BusinessException(404, "Payment not found");
        }
        paymentRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Payment> paymentsOf(Long orderId) {
        return paymentRepository.findByOrderIdOrderByIdDesc(orderId);
    }

    @Transactional(readOnly = true)
    public ReceivableRow detail(Long orderId) {
        SalesOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "Order not found"));
        Map<Long, List<Payment>> grouped = paymentRepository.findByOrderIdOrderByIdDesc(orderId).stream()
                .collect(Collectors.groupingBy(Payment::getOrderId));
        return toRow(order, grouped.getOrDefault(orderId, List.of()));
    }

    @Transactional(readOnly = true)
    public PageResult<ReceivableRow> receivables(String keyword, String status, int page, int size) {
        Specification<SalesOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("orderNo"), like),
                        cb.like(root.get("customerName"), like)));
            }
            predicates.add(cb.notEqual(root.get("status"), "CANCELLED"));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<SalesOrder> result = orderRepository.findAll(spec, PageQuery.of(page, size));
        List<Long> ids = result.getContent().stream().map(SalesOrder::getId).toList();
        Map<Long, List<Payment>> grouped = ids.isEmpty()
                ? new HashMap<>()
                : paymentRepository.findByOrderIdIn(ids).stream().collect(Collectors.groupingBy(Payment::getOrderId));

        List<ReceivableRow> rows = result.getContent().stream()
                .map(order -> toRow(order, grouped.getOrDefault(order.getId(), List.of())))
                .filter(row -> status == null || status.isBlank() || status.equals(row.getStatus()))
                .toList();
        return PageResult.of(rows, result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        List<SalesOrder> orders = orderRepository.findAll().stream()
                .filter(order -> !"CANCELLED".equals(order.getStatus()))
                .toList();
        List<Long> ids = orders.stream().map(SalesOrder::getId).toList();
        Map<Long, List<Payment>> grouped = ids.isEmpty()
                ? new HashMap<>()
                : paymentRepository.findByOrderIdIn(ids).stream().collect(Collectors.groupingBy(Payment::getOrderId));

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal received = BigDecimal.ZERO;
        BigDecimal outstanding = BigDecimal.ZERO;
        int overdueCount = 0;
        Map<String, BigDecimal> byCurrency = new LinkedHashMap<>();
        for (SalesOrder order : orders) {
            ReceivableRow row = toRow(order, grouped.getOrDefault(order.getId(), List.of()));
            total = total.add(row.getTotalAmount());
            received = received.add(row.getReceivedAmount());
            outstanding = outstanding.add(row.getOutstandingAmount());
            if (row.isOverdue()) {
                overdueCount++;
            }
            byCurrency.merge(order.getCurrency(), row.getOutstandingAmount(), BigDecimal::add);
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalAmount", scale(total));
        summary.put("receivedAmount", scale(received));
        summary.put("outstandingAmount", scale(outstanding));
        summary.put("overdueCount", overdueCount);
        summary.put("orderCount", orders.size());
        summary.put("byCurrency", byCurrency);
        return summary;
    }

    private ReceivableRow toRow(SalesOrder order, List<Payment> payments) {
        BigDecimal received = payments.stream()
                .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
        BigDecimal outstanding = total.subtract(received);
        if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
            outstanding = BigDecimal.ZERO;
        }
        ReceivableRow row = new ReceivableRow();
        row.setOrderId(order.getId());
        row.setOrderNo(order.getOrderNo());
        row.setCustomerName(order.getCustomerName());
        row.setOrderDate(order.getOrderDate() == null ? null : order.getOrderDate().toString());
        row.setCurrency(order.getCurrency());
        row.setPaymentTerms(order.getPaymentTerms());
        row.setTotalAmount(scale(total));
        row.setReceivedAmount(scale(received));
        row.setOutstandingAmount(scale(outstanding));
        row.setPaymentNos(payments.stream().map(Payment::getPaymentNo).toList());

        String status;
        boolean overdue = false;
        if (outstanding.compareTo(BigDecimal.ZERO) == 0) {
            status = "PAID";
        } else if (received.compareTo(BigDecimal.ZERO) > 0) {
            status = "PARTIAL";
        } else {
            status = "UNPAID";
        }
        if (!"PAID".equals(status) && order.getDeliveryDate() != null
                && order.getDeliveryDate().isBefore(LocalDate.now())) {
            overdue = true;
            status = "OVERDUE";
        }
        row.setStatus(status);
        row.setOverdue(overdue);
        return row;
    }

    private SalesOrder resolveOrder(Long orderId) {
        if (orderId == null) {
            throw new BusinessException("Order is required");
        }
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "Order not found"));
    }

    private String nextAvailableNo() {
        String candidate = noGenerator.nextPaymentNo();
        while (paymentRepository.existsByPaymentNo(candidate)) {
            candidate = noGenerator.nextPaymentNo();
        }
        return candidate;
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal nonNegative(BigDecimal value, String field) {
        BigDecimal scaled = scale(value);
        if (scaled.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(field + " must not be negative");
        }
        return scaled;
    }
}
