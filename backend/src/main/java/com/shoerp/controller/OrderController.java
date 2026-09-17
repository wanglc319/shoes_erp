package com.shoerp.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shoerp.common.ApiResponse;
import com.shoerp.common.PageResult;
import com.shoerp.dto.OrderRequest;
import com.shoerp.entity.SalesOrder;
import com.shoerp.entity.Shipment;
import com.shoerp.service.OrderService;
import com.shoerp.service.ShipmentService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    private final ShipmentService shipmentService;

    public OrderController(OrderService orderService, ShipmentService shipmentService) {
        this.orderService = orderService;
        this.shipmentService = shipmentService;
    }

    @GetMapping
    public ApiResponse<PageResult<SalesOrder>> page(@RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String productionStatus,
                                                    @RequestParam(required = false) Long customerId,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(orderService.page(keyword, status, productionStatus, customerId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<SalesOrder> get(@PathVariable Long id) {
        return ApiResponse.success(orderService.getById(id));
    }

    @PostMapping
    public ApiResponse<SalesOrder> create(@RequestBody OrderRequest request) {
        request.setId(null);
        return ApiResponse.success(orderService.save(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<SalesOrder> update(@PathVariable Long id, @RequestBody OrderRequest request) {
        request.setId(id);
        return ApiResponse.success(orderService.save(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/status")
    public ApiResponse<SalesOrder> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.success(orderService.updateStatus(id, status));
    }

    @PostMapping("/{id}/production-status")
    public ApiResponse<SalesOrder> updateProductionStatus(@PathVariable Long id, @RequestParam String productionStatus) {
        return ApiResponse.success(orderService.updateProductionStatus(id, productionStatus));
    }

    @PostMapping("/{id}/to-shipment")
    public ApiResponse<Shipment> toShipment(@PathVariable Long id) {
        return ApiResponse.success(shipmentService.createFromOrder(id));
    }
}
