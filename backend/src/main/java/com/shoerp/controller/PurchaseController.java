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
import com.shoerp.dto.PurchaseRequest;
import com.shoerp.entity.PurchaseOrder;
import com.shoerp.service.PurchaseOrderService;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseOrderService purchaseService;

    public PurchaseController(PurchaseOrderService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping
    public ApiResponse<PageResult<PurchaseOrder>> page(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) Long supplierId,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(purchaseService.page(keyword, status, supplierId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<PurchaseOrder> get(@PathVariable Long id) {
        return ApiResponse.success(purchaseService.getById(id));
    }

    @PostMapping
    public ApiResponse<PurchaseOrder> create(@RequestBody PurchaseRequest request) {
        request.setId(null);
        return ApiResponse.success(purchaseService.save(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PurchaseOrder> update(@PathVariable Long id, @RequestBody PurchaseRequest request) {
        request.setId(id);
        return ApiResponse.success(purchaseService.save(request));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<PurchaseOrder> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.success(purchaseService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        purchaseService.delete(id);
        return ApiResponse.success();
    }
}
