package com.cartvia.cartvia_backend.product;

import com.cartvia.cartvia_backend.product.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> withFilters(UUID storeId, String search, String category, Boolean activeOnly) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (storeId != null) {
                predicates.add(cb.equal(root.get("store").get("id"), storeId));
            }
            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
            }
            if (Boolean.TRUE.equals(activeOnly)) {
                predicates.add(cb.isTrue(root.get("active")));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("barcode")), pattern)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
