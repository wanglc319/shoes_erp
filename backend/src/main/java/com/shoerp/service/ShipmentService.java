package com.shoerp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.shoerp.dto.ShipmentRequest;
import com.shoerp.entity.OrderItem;
import com.shoerp.entity.SalesOrder;
import com.shoerp.entity.Shipment;
import com.shoerp.entity.ShipmentItem;
import com.shoerp.repository.SalesOrderRepository;
import com.shoerp.repository.ShipmentRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;

    private final SalesOrderRepository orderRepository;

    private final DocumentNoGenerator noGenerator;

    public ShipmentService(ShipmentRepository shipmentRepository, SalesOrderRepository orderRepository,
                           DocumentNoGenerator noGenerator) {
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
        this.noGenerator = noGenerator;
    }

    @Transactional(readOnly = true)
    public PageResult<Shipment> page(String keyword, String status, int page, int size) {
        Specification<Shipment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("shipmentNo"), like),
                        cb.like(root.get("orderNo"), like),
                        cb.like(root.get("customerName"), like),
                        cb.like(root.get("containerNo"), like)));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Shipment> result = shipmentRepository.findAll(spec, PageQuery.of(page, size));
        result.getContent().forEach(s -> s.getItems().size());
        return PageResult.of(result);
    }

    @Transactional(readOnly = true)
    public Shipment getById(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Shipment not found"));
        shipment.getItems().size();
        return shipment;
    }

    @Transactional
    public Shipment save(ShipmentRequest request) {
        Shipment shipment = request.getId() == null
                ? new Shipment()
                : shipmentRepository.findById(request.getId())
                        .orElseThrow(() -> new BusinessException(404, "Shipment not found"));

        String shipmentNo = request.getShipmentNo();
        if (shipmentNo == null || shipmentNo.isBlank()) {
            shipmentNo = noGenerator.nextShipmentNo();
        }
        if (request.getId() == null && shipmentRepository.existsByShipmentNo(shipmentNo)) {
            throw new BusinessException("Shipment number already exists: " + shipmentNo);
        }
        if (request.getId() != null
                && !shipmentNo.equals(shipment.getShipmentNo())
                && shipmentRepository.existsByShipmentNo(shipmentNo)) {
            throw new BusinessException("Shipment number already exists: " + shipmentNo);
        }

        SalesOrder order = resolveOrder(request.getOrderId());
        order.getItems().size();

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Shipment must contain at least one item");
        }

        validateShipmentQuantities(order, request.getId(), request.getItems());

        shipment.setShipmentNo(shipmentNo);
        shipment.setOrderId(order.getId());
        shipment.setOrderNo(order.getOrderNo());
        shipment.setCustomerName(order.getCustomerName());
        shipment.setShipDate(parseDate(request.getShipDate(), LocalDate.now()));
        shipment.setContainerNo(request.getContainerNo());
        shipment.setBlNo(request.getBlNo());
        shipment.setVessel(request.getVessel());
        shipment.setPortOfLoading(request.getPortOfLoading());
        shipment.setPortOfDestination(request.getPortOfDestination());
        shipment.setStatus(request.getStatus() == null ? "PREPARING" : StatusCatalog.require(request.getStatus(), StatusCatalog.SHIPMENT, "Status"));
        shipment.setRemark(request.getRemark());

        shipment.getItems().clear();
        int totalQty = 0;
        int totalCartons = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (request.getItems() != null) {
            for (ShipmentRequest.ItemPayload payload : request.getItems()) {
                ShipmentItem item = new ShipmentItem();
                item.setShipment(shipment);
                item.setStyleNo(payload.getStyleNo());
                item.setProductName(payload.getProductName());
                item.setColor(payload.getColor());
                item.setSizeRange(payload.getSizeRange());
                item.setQuantity(Numbers.nonNegative(payload.getQuantity(), "Quantity"));
                item.setUnit(payload.getUnit() == null ? "PRS" : payload.getUnit());
                item.setCartons(Numbers.nonNegative(payload.getCartons(), "Cartons"));
                item.setGrossWeight(Numbers.nonNegativeOrNull(payload.getGrossWeight(), "Gross weight"));
                item.setNetWeight(Numbers.nonNegativeOrNull(payload.getNetWeight(), "Net weight"));
                item.setUnitPrice(Numbers.nonNegative(payload.getUnitPrice(), "Unit price"));
                item.setAmount(scale(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))));
                totalQty += item.getQuantity();
                totalCartons += item.getCartons();
                totalAmount = totalAmount.add(item.getAmount());
                shipment.getItems().add(item);
            }
        }
        shipment.setTotalQuantity(totalQty);
        shipment.setTotalCartons(totalCartons);
        shipment.setTotalAmount(scale(totalAmount));

        Shipment saved = shipmentRepository.save(shipment);
        refreshOrderShippedQty(order);
        return saved;
    }

    @Transactional
    public Shipment createFromOrder(Long orderId) {
        SalesOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "Order not found"));
        order.getItems().size();

        ShipmentRequest request = new ShipmentRequest();
        request.setOrderId(order.getId());
        request.setShipDate(LocalDate.now().toString());
        request.setStatus("PREPARING");

        List<ShipmentRequest.ItemPayload> items = new ArrayList<>();
        for (OrderItem source : order.getItems()) {
            int remain = Math.max(source.getQuantity() - source.getShippedQuantity(), 0);
            if (remain <= 0) {
                continue;
            }
            ShipmentRequest.ItemPayload payload = new ShipmentRequest.ItemPayload();
            payload.setStyleNo(source.getStyleNo());
            payload.setProductName(source.getProductName());
            payload.setColor(source.getColor());
            payload.setSizeRange(source.getSizeRange());
            payload.setQuantity(remain);
            payload.setUnit(source.getUnit());
            payload.setCartons(Math.max(remain / 12, 1));
            payload.setUnitPrice(source.getUnitPrice());
            items.add(payload);
        }
        if (items.isEmpty()) {
            throw new BusinessException("All items of this order have already been shipped");
        }
        request.setItems(items);
        return save(request);
    }

    @Transactional
    public void delete(Long id) {
        Shipment shipment = getById(id);
        Long orderId = shipment.getOrderId();
        shipmentRepository.deleteById(id);
        SalesOrder order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.getItems().size();
            refreshOrderShippedQty(order);
        }
    }

    @Transactional
    public Shipment updateStatus(Long id, String status) {
        Shipment shipment = getById(id);
        shipment.setStatus(StatusCatalog.require(status, StatusCatalog.SHIPMENT, "Status"));
        return shipmentRepository.save(shipment);
    }

    private void refreshOrderShippedQty(SalesOrder order) {
        List<Shipment> shipments = shipmentRepository.findByOrderId(order.getId());
        for (OrderItem item : order.getItems()) {
            int shipped = 0;
            for (Shipment shipment : shipments) {
                shipment.getItems().size();
                for (ShipmentItem si : shipment.getItems()) {
                    if (item.getStyleNo() != null && item.getStyleNo().equals(si.getStyleNo())) {
                        shipped += si.getQuantity();
                    }
                }
            }
            item.setShippedQuantity(shipped);
        }
        boolean allShipped = order.getItems().stream()
                .allMatch(item -> item.getShippedQuantity() >= item.getQuantity());
        boolean anyShipped = order.getItems().stream()
                .anyMatch(item -> item.getShippedQuantity() > 0);
        if (allShipped && !order.getItems().isEmpty()) {
            order.setStatus("SHIPPED");
        } else if (anyShipped) {
            order.setStatus("PARTIAL_SHIPPED");
        } else if ("SHIPPED".equals(order.getStatus()) || "PARTIAL_SHIPPED".equals(order.getStatus())) {
            order.setStatus("CONFIRMED");
        }
        orderRepository.save(order);
    }

    private void validateShipmentQuantities(SalesOrder order, Long currentShipmentId,
                                            List<ShipmentRequest.ItemPayload> payloads) {
        Map<String, Integer> ordered = new HashMap<>();
        for (OrderItem item : order.getItems()) {
            if (item.getStyleNo() != null) {
                ordered.merge(item.getStyleNo(), item.getQuantity(), Integer::sum);
            }
        }
        Map<String, Integer> alreadyShipped = new HashMap<>();
        for (Shipment existing : shipmentRepository.findByOrderId(order.getId())) {
            if (currentShipmentId != null && currentShipmentId.equals(existing.getId())) {
                continue;
            }
            existing.getItems().size();
            for (ShipmentItem si : existing.getItems()) {
                if (si.getStyleNo() != null) {
                    alreadyShipped.merge(si.getStyleNo(), si.getQuantity(), Integer::sum);
                }
            }
        }
        Map<String, Integer> requested = new HashMap<>();
        for (ShipmentRequest.ItemPayload payload : payloads) {
            if (payload.getStyleNo() != null) {
                requested.merge(payload.getStyleNo(),
                        payload.getQuantity() == null ? 0 : payload.getQuantity(), Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> entry : requested.entrySet()) {
            Integer capacity = ordered.get(entry.getKey());
            if (capacity == null) {
                continue;
            }
            int shipped = alreadyShipped.getOrDefault(entry.getKey(), 0);
            if (shipped + entry.getValue() > capacity) {
                throw new BusinessException("Shipment quantity for style " + entry.getKey()
                        + " exceeds the ordered quantity (" + capacity + ")");
            }
        }
    }

    private SalesOrder resolveOrder(Long orderId) {
        if (orderId == null) {
            throw new BusinessException("Order is required");
        }
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "Order not found"));
    }

    private LocalDate parseDate(String value, LocalDate defaultValue) {
        return Dates.parse(value, defaultValue);
    }

    private BigDecimal scale(BigDecimal value) {
        return Numbers.scale(value);
    }
}
