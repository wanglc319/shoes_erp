package com.shoerp.controller;

import java.util.List;

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
import com.shoerp.dto.InquiryRequest;
import com.shoerp.entity.Inquiry;
import com.shoerp.service.ExcelExportService;
import com.shoerp.service.InquiryService;

@RestController
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    private final ExcelExportService excelExportService;

    public InquiryController(InquiryService inquiryService, ExcelExportService excelExportService) {
        this.inquiryService = inquiryService;
        this.excelExportService = excelExportService;
    }

    @GetMapping
    public ApiResponse<PageResult<Inquiry>> page(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) Long supplierId,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(inquiryService.page(keyword, status, supplierId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Inquiry> get(@PathVariable Long id) {
        return ApiResponse.success(inquiryService.getById(id));
    }

    @PostMapping
    public ApiResponse<Inquiry> create(@RequestBody InquiryRequest request) {
        request.setId(null);
        return ApiResponse.success(inquiryService.save(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Inquiry> update(@PathVariable Long id, @RequestBody InquiryRequest request) {
        request.setId(id);
        return ApiResponse.success(inquiryService.save(request));
    }

    @PostMapping("/from-shoes")
    public ApiResponse<Inquiry> fromShoes(@RequestBody FromShoesRequest request) {
        return ApiResponse.success(inquiryService.createFromShoes(
                request.getSupplierId(), request.getShoeIds(), request.getRemark()));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<Inquiry> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.success(inquiryService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        inquiryService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/export")
    public void export(@PathVariable Long id, jakarta.servlet.http.HttpServletResponse response)
            throws java.io.IOException {
        Inquiry inquiry = inquiryService.getById(id);
        com.shoerp.common.ExcelResponse.write(response, excelExportService.exportInquiry(inquiry),
                "RFQ-" + inquiry.getInquiryNo() + ".xlsx");
    }

    @lombok.Data
    public static class FromShoesRequest {
        private Long supplierId;
        private List<Long> shoeIds;
        private String remark;
    }
}
