package com.shoerp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shoerp.common.ApiResponse;
import com.shoerp.dto.PublicAlbum;
import com.shoerp.service.SampleShareService;

/**
 * 免登录只读接口：给客户手机打开样品相册用。
 */
@RestController
@RequestMapping("/api/public")
public class PublicAlbumController {

    private final SampleShareService sampleShareService;

    public PublicAlbumController(SampleShareService sampleShareService) {
        this.sampleShareService = sampleShareService;
    }

    @GetMapping("/sample-shares/{token}")
    public ApiResponse<PublicAlbum> album(@PathVariable String token) {
        return ApiResponse.success(sampleShareService.publicView(token));
    }
}
