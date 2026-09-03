package com.cartvia.cartvia_backend.store;

import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import com.cartvia.cartvia_backend.exception.ApiException;
import com.cartvia.cartvia_backend.store.entity.Store;
import com.cartvia.cartvia_backend.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;

    public Store requireStore(UUID storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Store not found"));
    }
}
