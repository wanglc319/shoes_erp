package com.shoerp.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shoerp.common.ApiResponse;
import com.shoerp.service.FxService;

@RestController
@RequestMapping("/api/fx")
public class FxController {

    private final FxService fxService;

    public FxController(FxService fxService) {
        this.fxService = fxService;
    }

    @GetMapping("/rates")
    public ApiResponse<Map<String, Object>> rates(@RequestParam(defaultValue = "USD") String base) {
        return ApiResponse.success(fxService.rates(base));
    }

    @GetMapping("/convert")
    public ApiResponse<Map<String, Object>> convert(@RequestParam(defaultValue = "USD") String from,
                                                    @RequestParam(defaultValue = "CNY") String to,
                                                    @RequestParam(required = false) BigDecimal amount) {
        return ApiResponse.success(fxService.convert(from, to, amount));
    }
}
