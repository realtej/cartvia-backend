package com.cartvia.cartvia_backend.session;

import com.cartvia.cartvia_backend.common.dto.ApiResponse;
import com.cartvia.cartvia_backend.session.dto.CreateSessionRequest;
import com.cartvia.cartvia_backend.session.dto.EndSessionResponse;
import com.cartvia.cartvia_backend.session.dto.SessionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<ApiResponse<SessionResponse>> createSession(
            @Valid @RequestBody CreateSessionRequest request) {
        SessionResponse response = sessionService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ApiResponse<SessionResponse> getSession(@PathVariable UUID id) {
        return ApiResponse.success(sessionService.getSession(id));
    }

    @PostMapping("/{id}/end")
    public ApiResponse<EndSessionResponse> endSession(@PathVariable UUID id) {
        return ApiResponse.success(sessionService.endSession(id));
    }
}
