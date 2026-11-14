package com.shoerp.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shoerp.common.ApiResponse;
import com.shoerp.dto.PublicAlbum;
import com.shoerp.dto.SampleShareRequest;
import com.shoerp.entity.SampleShare;
import com.shoerp.service.SampleShareService;

@RestController
@RequestMapping("/api/sample-shares")
public class SampleShareController {

    private final SampleShareService sampleShareService;

    public SampleShareController(SampleShareService sampleShareService) {
        this.sampleShareService = sampleShareService;
    }

    @GetMapping
    public ApiResponse<List<SampleShare>> list() {
        return ApiResponse.success(sampleShareService.list());
    }

    @PostMapping
    public ApiResponse<SampleShare> create(@RequestBody SampleShareRequest request) {
        return ApiResponse.success(sampleShareService.create(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        sampleShareService.delete(id);
        return ApiResponse.success();
    }
}
