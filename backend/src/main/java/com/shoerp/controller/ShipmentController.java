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
import com.shoerp.dto.ShipmentRequest;
import com.shoerp.entity.Shipment;
import com.shoerp.service.ShipmentService;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping
    public ApiResponse<PageResult<Shipment>> page(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(shipmentService.page(keyword, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Shipment> get(@PathVariable Long id) {
        return ApiResponse.success(shipmentService.getById(id));
    }

    @PostMapping
    public ApiResponse<Shipment> create(@RequestBody ShipmentRequest request) {
        request.setId(null);
        return ApiResponse.success(shipmentService.save(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Shipment> update(@PathVariable Long id, @RequestBody ShipmentRequest request) {
        request.setId(id);
        return ApiResponse.success(shipmentService.save(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        shipmentService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/status")
    public ApiResponse<Shipment> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.success(shipmentService.updateStatus(id, status));
    }
}
