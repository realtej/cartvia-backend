package com.cartvia.cartvia_backend.offer;

import com.cartvia.cartvia_backend.common.dto.ApiResponse;
import com.cartvia.cartvia_backend.offer.dto.OfferRequest;
import com.cartvia.cartvia_backend.offer.dto.OfferResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @GetMapping
    public ApiResponse<List<OfferResponse>> listOffers(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID productId) {
        return ApiResponse.success(offerService.listOffers(storeId, productId));
    }

    @GetMapping("/{id}")
    public ApiResponse<OfferResponse> getOffer(@PathVariable UUID id) {
        return ApiResponse.success(offerService.getOffer(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OfferResponse>> createOffer(@Valid @RequestBody OfferRequest request) {
        OfferResponse response = offerService.createOffer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ApiResponse<OfferResponse> updateOffer(@PathVariable UUID id, @Valid @RequestBody OfferRequest request) {
        return ApiResponse.success(offerService.updateOffer(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteOffer(@PathVariable UUID id) {
        offerService.deleteOffer(id);
        return ApiResponse.successResponse();
    }
}
