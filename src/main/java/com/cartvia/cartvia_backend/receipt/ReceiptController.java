package com.cartvia.cartvia_backend.receipt;

import com.cartvia.cartvia_backend.common.dto.ApiResponse;
import com.cartvia.cartvia_backend.receipt.dto.ReceiptResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @GetMapping("/{orderId}")
    public ApiResponse<ReceiptResponse> getReceipt(@PathVariable UUID orderId) {
        return ApiResponse.success(receiptService.getReceipt(orderId));
    }
}
