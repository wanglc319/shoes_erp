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
import com.shoerp.entity.Customer;
import com.shoerp.service.CustomerService;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ApiResponse<PageResult<Customer>> page(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String country,
                                                  @RequestParam(required = false) String level,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(customerService.page(keyword, country, level, page, size));
    }

    @GetMapping("/options")
    public ApiResponse<List<Customer>> options() {
        return ApiResponse.success(customerService.listEnabled());
    }

    @GetMapping("/{id}")
    public ApiResponse<Customer> get(@PathVariable Long id) {
        return ApiResponse.success(customerService.getById(id));
    }

    @PostMapping
    public ApiResponse<Customer> create(@RequestBody Customer payload) {
        payload.setId(null);
        return ApiResponse.success(customerService.save(payload));
    }

    @PutMapping("/{id}")
    public ApiResponse<Customer> update(@PathVariable Long id, @RequestBody Customer payload) {
        payload.setId(id);
        return ApiResponse.success(customerService.save(payload));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ApiResponse.success();
    }
}
