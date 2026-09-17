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
import com.shoerp.entity.Supplier;
import com.shoerp.service.SupplierService;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public ApiResponse<PageResult<Supplier>> page(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String type,
                                                  @RequestParam(required = false) Boolean enabled,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(supplierService.page(keyword, type, enabled, page, size));
    }

    @GetMapping("/options")
    public ApiResponse<List<Supplier>> options() {
        return ApiResponse.success(supplierService.listEnabled());
    }

    @GetMapping("/{id}")
    public ApiResponse<Supplier> get(@PathVariable Long id) {
        return ApiResponse.success(supplierService.getById(id));
    }

    @PostMapping
    public ApiResponse<Supplier> create(@RequestBody Supplier payload) {
        payload.setId(null);
        return ApiResponse.success(supplierService.save(payload));
    }

    @PutMapping("/{id}")
    public ApiResponse<Supplier> update(@PathVariable Long id, @RequestBody Supplier payload) {
        payload.setId(id);
        return ApiResponse.success(supplierService.save(payload));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ApiResponse.success();
    }
}
