package com.shoerp.controller;

import java.util.List;
import java.util.Map;

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
import com.shoerp.dto.ReceivableRow;
import com.shoerp.entity.Payment;
import com.shoerp.service.FinanceService;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/payments")
    public ApiResponse<PageResult<Payment>> payments(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Long orderId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(financeService.page(keyword, orderId, page, size));
    }

    @GetMapping("/payments/order/{orderId}")
    public ApiResponse<List<Payment>> paymentsOfOrder(@PathVariable Long orderId) {
        return ApiResponse.success(financeService.paymentsOf(orderId));
    }

    @PostMapping("/payments")
    public ApiResponse<Payment> createPayment(@RequestBody Payment payload) {
        payload.setId(null);
        return ApiResponse.success(financeService.save(payload));
    }

    @PutMapping("/payments/{id}")
    public ApiResponse<Payment> updatePayment(@PathVariable Long id, @RequestBody Payment payload) {
        payload.setId(id);
        return ApiResponse.success(financeService.save(payload));
    }

    @DeleteMapping("/payments/{id}")
    public ApiResponse<Void> deletePayment(@PathVariable Long id) {
        financeService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/receivables")
    public ApiResponse<PageResult<ReceivableRow>> receivables(@RequestParam(required = false) String keyword,
                                                              @RequestParam(required = false) String status,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(financeService.receivables(keyword, status, page, size));
    }

    @GetMapping("/receivables/summary")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.success(financeService.summary());
    }

    @GetMapping("/receivables/{orderId}")
    public ApiResponse<ReceivableRow> detail(@PathVariable Long orderId) {
        return ApiResponse.success(financeService.detail(orderId));
    }
}
