package com.shoerp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoerp.common.BusinessException;
import com.shoerp.common.Dates;
import com.shoerp.common.Numbers;
import com.shoerp.common.PageQuery;
import com.shoerp.common.PageResult;
import com.shoerp.common.StatusCatalog;
import com.shoerp.dto.OrderRequest;
import com.shoerp.entity.Customer;
import com.shoerp.entity.OrderItem;
import com.shoerp.entity.Quotation;
import com.shoerp.entity.SalesOrder;
import com.shoerp.repository.CustomerRepository;
import com.shoerp.repository.QuotationRepository;
import com.shoerp.repository.SalesOrderRepository;
import com.shoerp.repository.ShipmentRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class OrderService {

    private final SalesOrderRepository orderRepository;

    private final CustomerRepository customerRepository;

    private final QuotationRepository quotationRepository;

    private final ShipmentRepository shipmentRepository;

    private final DocumentNoGenerator noGenerator;

    public OrderService(SalesOrderRepository orderRepository, CustomerRepository customerRepository,
                        QuotationRepository quotationRepository, ShipmentRepository shipmentRepository,
                        DocumentNoGenerator noGenerator) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.quotationRepository = quotationRepository;
        this.shipmentRepository = shipmentRepository;
        this.noGenerator = noGenerator;
    }

    @Transactional(readOnly = true)
    public PageResult<SalesOrder> page(String keyword, String status, String productionStatus, Long customerId, int page, int size) {
        Specification<SalesOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("orderNo"), like),
                        cb.like(root.get("customerName"), like)));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (productionStatus != null && !productionStatus.isBlank()) {
                predicates.add(cb.equal(root.get("productionStatus"), productionStatus));
            }
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customerId"), customerId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<SalesOrder> result = orderRepository.findAll(spec, PageQuery.of(page, size));
        result.getContent().forEach(o -> o.getItems().size());
        return PageResult.of(result);
    }

    @Transactional(readOnly = true)
    public SalesOrder getById(Long id) {
        SalesOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Order not found"));
        order.getItems().size();
        return order;
    }

    @Transactional
    public SalesOrder save(OrderRequest request) {
        SalesOrder order = request.getId() == null
                ? new SalesOrder()
                : orderRepository.findById(request.getId())
                        .orElseThrow(() -> new BusinessException(404, "Order not found"));

        String orderNo = request.getOrderNo();
        if (orderNo == null || orderNo.isBlank()) {
            orderNo = noGenerator.nextOrderNo();
        }
        if (request.getId() == null && orderRepository.existsByOrderNo(orderNo)) {
            throw new BusinessException("Order number already exists: " + orderNo);
        }
        if (request.getId() != null
                && !orderNo.equals(order.getOrderNo())
                && orderRepository.existsByOrderNo(orderNo)) {
            throw new BusinessException("Order number already exists: " + orderNo);
        }

        Customer customer = resolveCustomer(request.getCustomerId());

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Order must contain at least one item");
        }

        order.setOrderNo(orderNo);
        order.setQuotationId(request.getQuotationId());
        order.setCustomerId(customer.getId());
        order.setCustomerName(customer.getName());
        order.setOrderDate(parseDate(request.getOrderDate(), LocalDate.now()));
        order.setDeliveryDate(parseDate(request.getDeliveryDate(), null));
        order.setTradeTerms(request.getTradeTerms());
        order.setCurrency(request.getCurrency() == null ? "USD" : request.getCurrency());
        order.setPaymentTerms(request.getPaymentTerms());
        order.setDepositAmount(Numbers.nonNegative(request.getDepositAmount(), "Deposit amount"));
        order.setStatus(request.getStatus() == null ? "PENDING" : StatusCatalog.require(request.getStatus(), StatusCatalog.ORDER, "Status"));
        order.setProductionStatus(request.getProductionStatus() == null ? "NOT_STARTED" : StatusCatalog.require(request.getProductionStatus(), StatusCatalog.PRODUCTION, "Production status"));
        order.setSalesOwner(request.getSalesOwner());
        order.setRemark(request.getRemark());

        List<OrderItem> existing = new ArrayList<>(order.getItems());
        order.getItems().clear();

        Map<String, Integer> shippedByStyle = new HashMap<>();
        for (OrderItem item : existing) {
            if (item.getStyleNo() != null && item.getShippedQuantity() != null) {
                shippedByStyle.merge(item.getStyleNo(), item.getShippedQuantity(), Integer::sum);
            }
        }
        Set<Long> submittedShoeIds = new HashSet<>();
        for (OrderRequest.ItemPayload payload : request.getItems()) {
            if (payload.getShoeId() != null) {
                submittedShoeIds.add(payload.getShoeId());
            }
        }
        for (OrderItem removed : existing) {
            if (removed.getShoeId() != null && !submittedShoeIds.contains(removed.getShoeId())) {
                if (removed.getShippedQuantity() != null && removed.getShippedQuantity() > 0) {
                    throw new BusinessException("Cannot remove style " + removed.getStyleNo()
                            + " because it already has a shipped quantity of " + removed.getShippedQuantity());
                }
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        if (request.getItems() != null) {
            for (OrderRequest.ItemPayload payload : request.getItems()) {
                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setShoeId(payload.getShoeId());
                item.setStyleNo(payload.getStyleNo());
                item.setProductName(payload.getProductName());
                item.setColor(payload.getColor());
                item.setSizeRange(payload.getSizeRange());
                item.setQuantity(Numbers.nonNegative(payload.getQuantity(), "Quantity"));
                item.setUnit(payload.getUnit() == null ? "PRS" : payload.getUnit());
                item.setUnitPrice(Numbers.nonNegative(payload.getUnitPrice(), "Unit price"));
                item.setCostPrice(Numbers.nonNegativeOrNull(payload.getCostPrice(), "Cost price"));
                item.setRemark(payload.getRemark());
                int shipped = shippedByStyle.getOrDefault(item.getStyleNo(), 0);
                if (item.getQuantity() < shipped) {
                    throw new BusinessException("Quantity cannot be less than the already shipped quantity ("
                            + shipped + ") for style " + payload.getStyleNo());
                }
                item.setShippedQuantity(shipped);
                BigDecimal amount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                item.setAmount(scale(amount));
                total = total.add(item.getAmount());
                order.getItems().add(item);
            }
        }
        order.setTotalAmount(scale(total));
        return orderRepository.save(order);
    }

    @Transactional
    public SalesOrder createFromQuotation(Long quotationId) {
        Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new BusinessException(404, "Quotation not found"));
        quotation.getItems().size();
        if ("LOST".equals(quotation.getStatus())) {
            throw new BusinessException("Quotation has been marked as LOST and cannot be converted to an order");
        }
        if (orderRepository.existsByQuotationId(quotation.getId())) {
            throw new BusinessException("Quotation has already been converted to a sales order");
        }

        OrderRequest request = new OrderRequest();
        request.setQuotationId(quotation.getId());
        request.setCustomerId(quotation.getCustomerId());
        request.setOrderDate(LocalDate.now().toString());
        request.setDeliveryDate(quotation.getValidUntil() == null ? null : quotation.getValidUntil().toString());
        request.setTradeTerms(quotation.getTradeTerms());
        request.setCurrency(quotation.getCurrency());
        request.setPaymentTerms(quotation.getPaymentTerms());
        request.setSalesOwner(quotation.getSalesOwner());
        request.setStatus("PENDING");

        List<OrderRequest.ItemPayload> items = new ArrayList<>();
        for (var source : quotation.getItems()) {
            OrderRequest.ItemPayload payload = new OrderRequest.ItemPayload();
            payload.setShoeId(source.getShoeId());
            payload.setStyleNo(source.getStyleNo());
            payload.setProductName(source.getProductName());
            payload.setColor(source.getColor());
            payload.setSizeRange(source.getSizeRange());
            payload.setQuantity(source.getQuantity());
            payload.setUnit(source.getUnit());
            payload.setUnitPrice(source.getUnitPrice());
            payload.setCostPrice(source.getCostPrice());
            payload.setRemark(source.getRemark());
            items.add(payload);
        }
        request.setItems(items);
        return save(request);
    }

    @Transactional
    public void delete(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new BusinessException(404, "Order not found");
        }
        if (shipmentRepository.existsByOrderId(id)) {
            throw new BusinessException("Order is referenced by shipments and cannot be deleted");
        }
        orderRepository.deleteById(id);
    }

    @Transactional
    public SalesOrder updateStatus(Long id, String status) {
        SalesOrder order = getById(id);
        order.setStatus(StatusCatalog.require(status, StatusCatalog.ORDER, "Status"));
        return orderRepository.save(order);
    }

    @Transactional
    public SalesOrder updateProductionStatus(Long id, String productionStatus) {
        SalesOrder order = getById(id);
        order.setProductionStatus(StatusCatalog.require(productionStatus, StatusCatalog.PRODUCTION, "Production status"));
        return orderRepository.save(order);
    }

    private Customer resolveCustomer(Long customerId) {
        if (customerId == null) {
            throw new BusinessException("Customer is required");
        }
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(404, "Customer not found"));
    }

    private LocalDate parseDate(String value, LocalDate defaultValue) {
        return Dates.parse(value, defaultValue);
    }

    private BigDecimal scale(BigDecimal value) {
        return Numbers.scale(value);
    }
}
