package com.cartvia.cartvia_backend.recommendation;

import com.cartvia.cartvia_backend.common.dto.ApiResponse;
import com.cartvia.cartvia_backend.recommendation.dto.RecommendationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    // No userId query param, by design — see RecommendationService javadoc
    // and Authorization Spec §7.5. The authenticated user is derived from
    // the JWT, same fix already applied to /api/notifications (§7.3).
    @GetMapping("/personalized")
    public ApiResponse<List<RecommendationDto>> getPersonalized() {
        return ApiResponse.success(recommendationService.getPersonalized());
    }

    @GetMapping("/fbt")
    public ApiResponse<List<RecommendationDto>> getFbt(@RequestParam UUID productId) {
        return ApiResponse.success(recommendationService.getFbt(productId));
    }

    @GetMapping("/basic")
    public ApiResponse<List<RecommendationDto>> getBasic(@RequestParam(required = false) String category) {
        return ApiResponse.success(recommendationService.getBasic(category));
    }

    @PostMapping("/recompute")
    public ApiResponse<Void> recompute() {
        recommendationService.recompute();
        return ApiResponse.successResponse();
    }
}
