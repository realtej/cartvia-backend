package com.cartvia.cartvia_backend.offer;

import com.cartvia.cartvia_backend.common.enums.ErrorCode;
import com.cartvia.cartvia_backend.common.enums.Role;
import com.cartvia.cartvia_backend.exception.ApiException;
import com.cartvia.cartvia_backend.offer.dto.OfferRequest;
import com.cartvia.cartvia_backend.offer.dto.OfferResponse;
import com.cartvia.cartvia_backend.offer.entity.Offer;
import com.cartvia.cartvia_backend.offer.repository.OfferRepository;
import com.cartvia.cartvia_backend.product.entity.Product;
import com.cartvia.cartvia_backend.product.repository.ProductRepository;
import com.cartvia.cartvia_backend.security.AuthorizationService;
import com.cartvia.cartvia_backend.store.StoreService;
import com.cartvia.cartvia_backend.store.entity.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Read endpoints (GET /api/offers, GET /api/offers/{id}) are open to any
 * authenticated CUSTOMER/STORE_STAFF/ADMIN per the role matrix — PUBLIC and
 * DEVICE are denied, but there's no store-scoping requirement on reads in
 * the spec (§8.5 only covers POST/PUT/DELETE), so staff can browse offers
 * across stores the same as a customer, filtered by the optional storeId/
 * productId query params.
 * <p>
 * Write endpoints enforce §8.5 Offer Store Authorization: STORE_STAFF may
 * only create/update/delete offers for their own store
 * (request.storeId/offer.storeId == authenticatedStaff.storeId), ADMIN is
 * unrestricted.
 */
@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final StoreService storeService;
    private final ProductRepository productRepository;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public List<OfferResponse> listOffers(UUID storeId, UUID productId) {
        authorizationService.requireRole(Role.CUSTOMER, Role.STORE_STAFF, Role.ADMIN);

        List<Offer> offers;
        if (storeId != null && productId != null) {
            offers = offerRepository.findByStore_IdAndProduct_Id(storeId, productId);
        } else if (storeId != null) {
            offers = offerRepository.findByStore_Id(storeId);
        } else if (productId != null) {
            offers = offerRepository.findByProduct_Id(productId);
        } else {
            offers = offerRepository.findAll();
        }

        return offers.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OfferResponse getOffer(UUID offerId) {
        authorizationService.requireRole(Role.CUSTOMER, Role.STORE_STAFF, Role.ADMIN);
        Offer offer = requireOffer(offerId);
        return toResponse(offer);
    }

    @Transactional
    public OfferResponse createOffer(OfferRequest request) {
        authorizationService.requireStoreStaffForStore(request.getStoreId());

        Store store = storeService.requireStore(request.getStoreId());
        Product product = resolveProduct(request.getProductId());

        Offer offer = new Offer();
        offer.setStore(store);
        offer.setProduct(product);
        applyRequest(offer, request);
        offer = offerRepository.save(offer);
        return toResponse(offer);
    }

    @Transactional
    public OfferResponse updateOffer(UUID offerId, OfferRequest request) {
        Offer offer = requireOffer(offerId);
        authorizationService.requireStoreStaffForStore(offer.getStore().getId());

        Product product = resolveProduct(request.getProductId());
        offer.setProduct(product);
        applyRequest(offer, request);
        offer = offerRepository.save(offer);
        return toResponse(offer);
    }

    @Transactional
    public void deleteOffer(UUID offerId) {
        Offer offer = requireOffer(offerId);
        authorizationService.requireStoreStaffForStore(offer.getStore().getId());
        offerRepository.delete(offer);
    }

    private void applyRequest(Offer offer, OfferRequest request) {
        offer.setTitle(request.getTitle());
        offer.setDiscountPct(request.getDiscountPct());
        offer.setValidFrom(request.getValidFrom());
        offer.setValidTo(request.getValidTo());
        offer.setActive(request.isActive());
    }

    private Product resolveProduct(UUID productId) {
        if (productId == null) {
            return null;
        }
        return productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND, "Product not found"));
    }

    private Offer requireOffer(UUID offerId) {
        return offerRepository.findById(offerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Offer not found"));
    }

    private OfferResponse toResponse(Offer offer) {
        return new OfferResponse(
                offer.getId(),
                offer.getStore().getId(),
                offer.getProduct() != null ? offer.getProduct().getId() : null,
                offer.getTitle(),
                offer.getDiscountPct(),
                offer.getValidFrom(),
                offer.getValidTo(),
                offer.isActive());
    }
}
