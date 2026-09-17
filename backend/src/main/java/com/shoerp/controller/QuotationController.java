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
import com.shoerp.dto.QuotationRequest;
import com.shoerp.entity.Quotation;
import com.shoerp.entity.SalesOrder;
import com.shoerp.service.OrderService;
import com.shoerp.service.QuotationService;

@RestController
@RequestMapping("/api/quotations")
public class QuotationController {

    private final QuotationService quotationService;

    private final OrderService orderService;

    public QuotationController(QuotationService quotationService, OrderService orderService) {
        this.quotationService = quotationService;
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<PageResult<Quotation>> page(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Long customerId,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(quotationService.page(keyword, status, customerId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Quotation> get(@PathVariable Long id) {
        return ApiResponse.success(quotationService.getById(id));
    }

    @PostMapping
    public ApiResponse<Quotation> create(@RequestBody QuotationRequest request) {
        request.setId(null);
        return ApiResponse.success(quotationService.save(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Quotation> update(@PathVariable Long id, @RequestBody QuotationRequest request) {
        request.setId(id);
        return ApiResponse.success(quotationService.save(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        quotationService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/status")
    public ApiResponse<Quotation> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.success(quotationService.updateStatus(id, status));
    }

    @PostMapping("/{id}/to-order")
    public ApiResponse<SalesOrder> toOrder(@PathVariable Long id) {
        SalesOrder order = orderService.createFromQuotation(id);
        quotationService.updateStatus(id, "WON");
        return ApiResponse.success(order);
    }
}
