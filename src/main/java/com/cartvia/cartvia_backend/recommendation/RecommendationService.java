package com.cartvia.cartvia_backend.recommendation;

import com.cartvia.cartvia_backend.common.enums.OrderStatus;
import com.cartvia.cartvia_backend.common.enums.RecommendationType;
import com.cartvia.cartvia_backend.common.enums.Role;
import com.cartvia.cartvia_backend.order.entity.Order;
import com.cartvia.cartvia_backend.order.entity.OrderItem;
import com.cartvia.cartvia_backend.order.repository.OrderItemRepository;
import com.cartvia.cartvia_backend.product.entity.Product;
import com.cartvia.cartvia_backend.product.repository.ProductRepository;
import com.cartvia.cartvia_backend.recommendation.dto.RecommendationDto;
import com.cartvia.cartvia_backend.recommendation.entity.RecommendationEntry;
import com.cartvia.cartvia_backend.recommendation.repository.RecommendationRepository;
import com.cartvia.cartvia_backend.security.AuthenticationService;
import com.cartvia.cartvia_backend.security.AuthorizationService;
import com.cartvia.cartvia_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reads (personalized/fbt/basic) are CUSTOMER-only per Authorization Spec
 * §6/§7.5/role matrix. For "personalized", the spec explicitly prefers
 * deriving userId from the JWT rather than trusting a client-supplied query
 * param — same fix already applied to Notifications (§7.3) — so no userId
 * query param is accepted here.
 *
 * recompute() is ADMIN-only (§9.1) and is the only write path for this
 * module; nothing previously populated RecommendationEntry rows, so this is
 * new rule-based logic, not a refactor of existing computation:
 *   - BESTSELLER: top products per category by total quantity sold across
 *     PAID orders.
 *   - FREQUENTLY_BOUGHT_TOGETHER: top co-purchased product pairs, counted
 *     from distinct products appearing together within the same PAID order.
 *   - PERSONALIZED_CATEGORY_MATCH: per user, the bestsellers in that user's
 *     single most-purchased category (by quantity), excluding products the
 *     user has already bought.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int MAX_RESULTS = 5;
    private static final int BESTSELLERS_PER_CATEGORY = 5;
    private static final int FBT_PER_PRODUCT = 5;
    private static final int PERSONALIZED_PER_USER = 5;

    private final RecommendationRepository recommendationRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public List<RecommendationDto> getFrequentlyBoughtTogether(UUID productId) {
        return recommendationRepository
                .findByProduct_IdAndTypeOrderByScoreDesc(productId, RecommendationType.FREQUENTLY_BOUGHT_TOGETHER)
                .stream()
                .limit(MAX_RESULTS)
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendationDto> getPersonalized() {
        authorizationService.requireRole(Role.CUSTOMER);
        UUID userId = authenticationService.getAuthenticatedUserId();
        return recommendationRepository
                .findByUser_IdAndTypeOrderByScoreDesc(userId, RecommendationType.PERSONALIZED_CATEGORY_MATCH)
                .stream()
                .limit(MAX_RESULTS)
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendationDto> getFbt(UUID productId) {
        authorizationService.requireRole(Role.CUSTOMER);
        return getFrequentlyBoughtTogether(productId);
    }

    @Transactional(readOnly = true)
    public List<RecommendationDto> getBasic(String category) {
        authorizationService.requireRole(Role.CUSTOMER);
        List<RecommendationEntry> entries = (category == null || category.isBlank())
                ? recommendationRepository.findByTypeOrderByScoreDesc(RecommendationType.BESTSELLER)
                : recommendationRepository.findByCategoryAndTypeOrderByScoreDesc(category, RecommendationType.BESTSELLER);
        return entries.stream()
                .limit(MAX_RESULTS)
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void recompute() {
        authorizationService.requireAdmin();

        List<OrderItem> paidItems = orderItemRepository.findByOrder_Status(OrderStatus.PAID);

        recommendationRepository.deleteAll();

        List<RecommendationEntry> entries = new ArrayList<>();
        entries.addAll(buildBestsellerEntries(paidItems));
        entries.addAll(buildFbtEntries(paidItems));
        entries.addAll(buildPersonalizedEntries(paidItems, entries));

        recommendationRepository.saveAll(entries);
    }

    private List<RecommendationEntry> buildBestsellerEntries(List<OrderItem> paidItems) {
        // category -> productId -> total quantity sold
        Map<String, Map<UUID, Integer>> byCategory = new HashMap<>();
        Map<UUID, Product> productsById = new HashMap<>();

        for (OrderItem item : paidItems) {
            Product product = item.getProduct();
            String category = product.getCategory() != null ? product.getCategory() : "UNCATEGORIZED";
            productsById.put(product.getId(), product);
            byCategory.computeIfAbsent(category, k -> new HashMap<>())
                    .merge(product.getId(), item.getQuantity(), Integer::sum);
        }

        List<RecommendationEntry> result = new ArrayList<>();
        for (Map.Entry<String, Map<UUID, Integer>> categoryEntry : byCategory.entrySet()) {
            categoryEntry.getValue().entrySet().stream()
                    .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                    .limit(BESTSELLERS_PER_CATEGORY)
                    .forEach(productQty -> {
                        RecommendationEntry entry = new RecommendationEntry();
                        entry.setType(RecommendationType.BESTSELLER);
                        entry.setProduct(productsById.get(productQty.getKey()));
                        entry.setCategory(categoryEntry.getKey());
                        entry.setScore(productQty.getValue());
                        result.add(entry);
                    });
        }
        return result;
    }

    private List<RecommendationEntry> buildFbtEntries(List<OrderItem> paidItems) {
        // orderId -> distinct products purchased in that order
        Map<UUID, Map<UUID, Product>> productsByOrder = new HashMap<>();
        for (OrderItem item : paidItems) {
            Order order = item.getOrder();
            productsByOrder.computeIfAbsent(order.getId(), k -> new HashMap<>())
                    .put(item.getProduct().getId(), item.getProduct());
        }

        // productId -> relatedProductId -> co-occurrence count
        Map<UUID, Map<UUID, Integer>> coOccurrence = new HashMap<>();
        Map<UUID, Product> productsById = new HashMap<>();

        for (Map<UUID, Product> productsInOrder : productsByOrder.values()) {
            List<Product> distinctProducts = new ArrayList<>(productsInOrder.values());
            productsById.putAll(productsInOrder);
            for (int i = 0; i < distinctProducts.size(); i++) {
                for (int j = 0; j < distinctProducts.size(); j++) {
                    if (i == j) continue;
                    UUID productId = distinctProducts.get(i).getId();
                    UUID relatedId = distinctProducts.get(j).getId();
                    coOccurrence.computeIfAbsent(productId, k -> new HashMap<>())
                            .merge(relatedId, 1, Integer::sum);
                }
            }
        }

        List<RecommendationEntry> result = new ArrayList<>();
        for (Map.Entry<UUID, Map<UUID, Integer>> productEntry : coOccurrence.entrySet()) {
            Product product = productsById.get(productEntry.getKey());
            productEntry.getValue().entrySet().stream()
                    .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                    .limit(FBT_PER_PRODUCT)
                    .forEach(relatedQty -> {
                        RecommendationEntry entry = new RecommendationEntry();
                        entry.setType(RecommendationType.FREQUENTLY_BOUGHT_TOGETHER);
                        entry.setProduct(product);
                        entry.setRelatedProduct(productsById.get(relatedQty.getKey()));
                        entry.setScore(relatedQty.getValue());
                        result.add(entry);
                    });
        }
        return result;
    }

    private List<RecommendationEntry> buildPersonalizedEntries(List<OrderItem> paidItems, List<RecommendationEntry> bestsellersSoFar) {
        // userId -> category -> quantity purchased (to find each user's top category)
        Map<UUID, Map<String, Integer>> categoryQtyByUser = new HashMap<>();
        // userId -> set of productIds already purchased
        Map<UUID, Map<UUID, Boolean>> purchasedByUser = new HashMap<>();
        Map<UUID, User> usersById = new HashMap<>();

        for (OrderItem item : paidItems) {
            User user = item.getOrder().getUser();
            Product product = item.getProduct();
            String category = product.getCategory() != null ? product.getCategory() : "UNCATEGORIZED";

            usersById.put(user.getId(), user);
            categoryQtyByUser.computeIfAbsent(user.getId(), k -> new HashMap<>())
                    .merge(category, item.getQuantity(), Integer::sum);
            purchasedByUser.computeIfAbsent(user.getId(), k -> new HashMap<>())
                    .put(product.getId(), Boolean.TRUE);
        }

        // category -> bestseller products in score order (computed in this same recompute pass)
        Map<String, List<RecommendationEntry>> bestsellersByCategory = new HashMap<>();
        for (RecommendationEntry entry : bestsellersSoFar) {
            bestsellersByCategory.computeIfAbsent(entry.getCategory(), k -> new ArrayList<>()).add(entry);
        }

        List<RecommendationEntry> result = new ArrayList<>();
        for (Map.Entry<UUID, Map<String, Integer>> userEntry : categoryQtyByUser.entrySet()) {
            UUID userId = userEntry.getKey();
            String topCategory = userEntry.getValue().entrySet().stream()
                    .max(Comparator.comparingInt(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (topCategory == null) continue;

            List<RecommendationEntry> categoryBestsellers = bestsellersByCategory.getOrDefault(topCategory, List.of());
            Map<UUID, Boolean> alreadyPurchased = purchasedByUser.getOrDefault(userId, Map.of());

            categoryBestsellers.stream()
                    .filter(bestseller -> !alreadyPurchased.containsKey(bestseller.getProduct().getId()))
                    .sorted(Comparator.comparingDouble(RecommendationEntry::getScore).reversed())
                    .limit(PERSONALIZED_PER_USER)
                    .forEach(bestseller -> {
                        RecommendationEntry entry = new RecommendationEntry();
                        entry.setType(RecommendationType.PERSONALIZED_CATEGORY_MATCH);
                        entry.setUser(usersById.get(userId));
                        entry.setProduct(bestseller.getProduct());
                        entry.setCategory(topCategory);
                        entry.setScore(bestseller.getScore());
                        result.add(entry);
                    });
        }
        return result;
    }

    private RecommendationDto toDto(RecommendationEntry entry) {
        Product product = entry.getRelatedProduct() != null
                ? entry.getRelatedProduct()
                : entry.getProduct();
        return new RecommendationDto(product.getId(), product.getName(), entry.getType());
    }
}
