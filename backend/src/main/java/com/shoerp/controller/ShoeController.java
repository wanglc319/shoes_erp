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
import com.shoerp.entity.Shoe;
import com.shoerp.service.ShoeService;

@RestController
@RequestMapping("/api/shoes")
public class ShoeController {

    private final ShoeService shoeService;

    public ShoeController(ShoeService shoeService) {
        this.shoeService = shoeService;
    }

    @GetMapping
    public ApiResponse<PageResult<Shoe>> page(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String category,
                                              @RequestParam(required = false) String gender,
                                              @RequestParam(required = false) Long supplierId,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(shoeService.page(keyword, category, gender, supplierId, page, size));
    }

    @GetMapping("/options")
    public ApiResponse<List<Shoe>> options() {
        return ApiResponse.success(shoeService.listEnabled());
    }

    @GetMapping("/{id}")
    public ApiResponse<Shoe> get(@PathVariable Long id) {
        return ApiResponse.success(shoeService.getById(id));
    }

    @PostMapping
    public ApiResponse<Shoe> create(@RequestBody Shoe payload) {
        payload.setId(null);
        return ApiResponse.success(shoeService.save(payload));
    }

    @PutMapping("/{id}")
    public ApiResponse<Shoe> update(@PathVariable Long id, @RequestBody Shoe payload) {
        payload.setId(id);
        return ApiResponse.success(shoeService.save(payload));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        shoeService.delete(id);
        return ApiResponse.success();
    }
}
