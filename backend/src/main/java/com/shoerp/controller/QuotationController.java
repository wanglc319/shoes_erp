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
import org.springframework.web.multipart.MultipartFile;

import com.shoerp.common.ApiResponse;
import com.shoerp.common.ExcelResponse;
import com.shoerp.common.PageResult;
import com.shoerp.dto.ImportPreview;
import com.shoerp.dto.QuotationRequest;
import com.shoerp.entity.Quotation;
import com.shoerp.entity.SalesOrder;
import com.shoerp.service.ExcelExportService;
import com.shoerp.service.OrderService;
import com.shoerp.service.PiImportService;
import com.shoerp.service.QuotationService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/quotations")
public class QuotationController {

    private final QuotationService quotationService;

    private final OrderService orderService;

    private final ExcelExportService excelExportService;

    private final PiImportService piImportService;

    public QuotationController(QuotationService quotationService, OrderService orderService,
                               ExcelExportService excelExportService, PiImportService piImportService) {
        this.quotationService = quotationService;
        this.orderService = orderService;
        this.excelExportService = excelExportService;
        this.piImportService = piImportService;
    }

    @GetMapping("/{id}/export")
    public void export(@PathVariable Long id, HttpServletResponse response) throws java.io.IOException {
        Quotation quotation = quotationService.getById(id);
        ExcelResponse.write(response, excelExportService.exportQuotation(quotation),
                "PI-" + quotation.getQuoteNo() + ".xlsx");
    }

    @GetMapping
    public ApiResponse<PageResult<Quotation>> page(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Long customerId,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(quotationService.page(keyword, status, customerId, page, size));
    }

    /**
     * 拖入 Excel/CSV 报价单，解析成明细供预览。
     */
    @PostMapping("/import-preview")
    public ApiResponse<ImportPreview> importPreview(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(piImportService.preview(file));
    }

    /**
     * 拖入报价图片，走 OCR 解析成明细。
     */
    @PostMapping("/ocr-preview")
    public ApiResponse<ImportPreview> ocrPreview(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(piImportService.ocrPreview(file));
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
