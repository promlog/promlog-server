package com.promlog.promlog.prompt.platform.controller;

import com.promlog.promlog.global.response.ApiResponse;
import com.promlog.promlog.prompt.platform.dto.PlatformResponse;
import com.promlog.promlog.prompt.platform.service.PlatformService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/platforms")
public class PlatformController {

    private final PlatformService platformService;

    public PlatformController(PlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping
    public ApiResponse<List<PlatformResponse>> list() {
        return ApiResponse.ok(platformService.list());
    }
}
