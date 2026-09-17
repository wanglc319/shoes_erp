package com.shoerp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoerp.common.BusinessException;
import com.shoerp.common.PageResult;
import com.shoerp.dto.OrderRequest;
import com.shoerp.entity.Customer;
import com.shoerp.entity.OrderItem;
import com.shoerp.entity.Quotation;
import com.shoerp.entity.SalesOrder;
import com.shoerp.repository.CustomerRepository;
import com.shoerp.repository.QuotationRepository;
import com.shoerp.repository.SalesOrderRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class OrderService {

    private final SalesOrderRepository orderRepository;

    private final CustomerRepository customerRepository;

    private final QuotationRepository quotationRepository;

    private final DocumentNoGenerator noGenerator;

    public OrderService(SalesOrderRepository orderRepository, CustomerRepository customerRepository,
                        QuotationRepository quotationRepository, DocumentNoGenerator noGenerator) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.quotationRepository = quotationRepository;
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
        Page<SalesOrder> result = orderRepository.findAll(spec,
                PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "id")));
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

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new BusinessException(404, "Customer not found"));

        order.setOrderNo(orderNo);
        order.setQuotationId(request.getQuotationId());
        order.setCustomerId(customer.getId());
        order.setCustomerName(customer.getName());
        order.setOrderDate(parseDate(request.getOrderDate(), LocalDate.now()));
        order.setDeliveryDate(parseDate(request.getDeliveryDate(), null));
        order.setTradeTerms(request.getTradeTerms());
        order.setCurrency(request.getCurrency() == null ? "USD" : request.getCurrency());
        order.setPaymentTerms(request.getPaymentTerms());
        order.setDepositAmount(scale(request.getDepositAmount()));
        order.setStatus(request.getStatus() == null ? "PENDING" : request.getStatus());
        order.setProductionStatus(request.getProductionStatus() == null ? "NOT_STARTED" : request.getProductionStatus());
        order.setSalesOwner(request.getSalesOwner());
        order.setRemark(request.getRemark());

        List<OrderItem> existing = new ArrayList<>(order.getItems());
        order.getItems().clear();

        BigDecimal total = BigDecimal.ZERO;
        if (request.getItems() != null) {
            int index = 0;
            for (OrderRequest.ItemPayload payload : request.getItems()) {
                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setShoeId(payload.getShoeId());
                item.setStyleNo(payload.getStyleNo());
                item.setProductName(payload.getProductName());
                item.setColor(payload.getColor());
                item.setSizeRange(payload.getSizeRange());
                item.setQuantity(payload.getQuantity() == null ? 0 : payload.getQuantity());
                item.setUnit(payload.getUnit() == null ? "PRS" : payload.getUnit());
                item.setUnitPrice(scale(payload.getUnitPrice()));
                item.setCostPrice(payload.getCostPrice());
                item.setRemark(payload.getRemark());
                item.setShippedQuantity(index < existing.size()
                        ? existing.get(index).getShippedQuantity() : 0);
                BigDecimal amount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                item.setAmount(scale(amount));
                total = total.add(item.getAmount());
                order.getItems().add(item);
                index++;
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
        orderRepository.deleteById(id);
    }

    @Transactional
    public SalesOrder updateStatus(Long id, String status) {
        SalesOrder order = getById(id);
        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Transactional
    public SalesOrder updateProductionStatus(Long id, String productionStatus) {
        SalesOrder order = getById(id);
        order.setProductionStatus(productionStatus);
        return orderRepository.save(order);
    }

    private LocalDate parseDate(String value, LocalDate defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return LocalDate.parse(value.substring(0, 10));
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
