package com.cartvia.cartvia_backend.trolley;

import com.cartvia.cartvia_backend.common.dto.ApiResponse;
import com.cartvia.cartvia_backend.config.CartviaProperties;
import com.cartvia.cartvia_backend.trolley.dto.CreateTrolleyRequest;
import com.cartvia.cartvia_backend.trolley.dto.HeartbeatRequest;
import com.cartvia.cartvia_backend.trolley.dto.TrolleyProvisionResponse;
import com.cartvia.cartvia_backend.trolley.dto.TrolleyResponse;
import com.cartvia.cartvia_backend.trolley.dto.UpdateTrolleyStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/trolleys")
@RequiredArgsConstructor
public class TrolleyController {

    private final TrolleyService trolleyService;
    private final CartviaProperties cartviaProperties;

    @PostMapping
    public ResponseEntity<ApiResponse<TrolleyProvisionResponse>> createTrolley(
            @Valid @RequestBody CreateTrolleyRequest request) {
        TrolleyProvisionResponse response = trolleyService.createTrolley(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{code}")
    public ApiResponse<TrolleyResponse> getTrolley(@PathVariable String code) {
        return ApiResponse.success(trolleyService.getByCode(code));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<TrolleyResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTrolleyStatusRequest request) {
        return ApiResponse.success(trolleyService.updateStatus(id, request));
    }

    @PostMapping("/{code}/heartbeat")
    public ApiResponse<Void> heartbeat(
            @PathVariable String code,
            @RequestHeader(name = "X-Device-Token", required = false) String deviceToken,
            @Valid @RequestBody(required = false) HeartbeatRequest request) {
        HeartbeatRequest body = request != null ? request : new HeartbeatRequest();
        String headerName = cartviaProperties.getDevice().getTokenHeader();
        if (deviceToken == null) {
            deviceToken = "";
        }
        trolleyService.heartbeat(code, body, deviceToken);
        return ApiResponse.successResponse();
    }
}
