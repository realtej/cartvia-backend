package com.cartvia.cartvia_backend.common;

import com.cartvia.cartvia_backend.cart.entity.CartItem;
import com.cartvia.cartvia_backend.offer.entity.Offer;
import com.cartvia.cartvia_backend.offer.repository.OfferRepository;
import com.cartvia.cartvia_backend.product.entity.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PricingService {

    private final OfferRepository offerRepository;

    public PricingService(OfferRepository offerRepository) {
        this.offerRepository = offerRepository;
    }

    public BigDecimal getListUnitPrice(Product product) {
        return product.getPrice().setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getEffectiveUnitPrice(Product product, UUID storeId) {
        BigDecimal listPrice = getListUnitPrice(product);
        BigDecimal productDiscount = percentOf(listPrice, product.getDiscountPct());
        BigDecimal offerDiscount = getBestOfferDiscount(product, storeId, listPrice);
        BigDecimal totalDiscount = productDiscount.max(offerDiscount);
        return listPrice.subtract(totalDiscount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateLineTotal(BigDecimal unitPrice, int quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateCartSubtotal(List<CartItem> items) {
        return items.stream()
                .map(item -> calculateLineTotal(item.getUnitPrice(), item.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public OrderTotals calculateOrderTotals(List<CartItem> items, UUID storeId) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;

        for (CartItem item : items) {
            Product product = item.getProduct();
            BigDecimal listPrice = getListUnitPrice(product);
            BigDecimal lineList = calculateLineTotal(listPrice, item.getQuantity());
            BigDecimal lineEffective = calculateLineTotal(item.getUnitPrice(), item.getQuantity());
            subtotal = subtotal.add(lineList);
            discountTotal = discountTotal.add(lineList.subtract(lineEffective));

            BigDecimal gstPct = product.getGstSlabPct() != null ? product.getGstSlabPct() : BigDecimal.ZERO;
            taxTotal = taxTotal.add(percentOf(lineEffective, gstPct));
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        discountTotal = discountTotal.setScale(2, RoundingMode.HALF_UP);
        taxTotal = taxTotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotal.subtract(discountTotal).add(taxTotal).setScale(2, RoundingMode.HALF_UP);
        return new OrderTotals(subtotal, discountTotal, taxTotal, grandTotal);
    }

    private BigDecimal getBestOfferDiscount(Product product, UUID storeId, BigDecimal listPrice) {
        if (storeId == null) {
            return BigDecimal.ZERO;
        }
        LocalDateTime now = LocalDateTime.now();
        List<Offer> offers = offerRepository.findByStore_IdAndProduct_IdAndActiveTrue(storeId, product.getId());
        offers.addAll(offerRepository.findByStore_IdAndActiveTrue(storeId).stream()
                .filter(o -> o.getProduct() == null)
                .toList());

        return offers.stream()
                .filter(o -> isOfferActive(o, now))
                .map(o -> percentOf(listPrice, o.getDiscountPct()))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private boolean isOfferActive(Offer offer, LocalDateTime now) {
        if (!offer.isActive()) {
            return false;
        }
        if (offer.getValidFrom() != null && now.isBefore(offer.getValidFrom())) {
            return false;
        }
        if (offer.getValidTo() != null && now.isAfter(offer.getValidTo())) {
            return false;
        }
        return true;
    }

    private BigDecimal percentOf(BigDecimal amount, BigDecimal pct) {
        if (pct == null || pct.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(pct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public record OrderTotals(
            BigDecimal subtotal,
            BigDecimal discountTotal,
            BigDecimal taxTotal,
            BigDecimal grandTotal) {
    }
}
