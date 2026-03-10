package io.github.takgeun.shop.product.view.dto;

import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 상품 상세 화면 전용 View DTO (도메인을 뷰에 직접 노출시키지 않기 위함)
 * 리뷰/이미지/상세스펙은 추후 기능 추가 시 확장 예정
 */
@Getter
public class ProductDetailView {

    private static final String FALLBACK_IMAGE_URL = "/images/no-image.png";

    private final Long id;
    private final Long categoryId;

    private final String name;
    private final int price;
    private final Integer originalPrice;
    private final int discountPercent;

    private final int stock;
    private final ProductStatus status;

    private final double rating;
    private final int reviews;

    private final String badge;             // BEST, NEW, SALE 등
    private final String imageUrl;          // 항상 null 아님. (기본 이미지 포함)

    private final String description;
    private final List<String> details;         // 불릿 리스트

    public ProductDetailView(Long id, Long categoryId, String name, int price, Integer originalPrice, int discountPercent, int stock, ProductStatus status, double rating, int reviews, String badge, String imageUrl, String description, List<String> details) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = safeText(name);
        this.price = Math.max(price, 0);
        this.originalPrice = originalPrice;
        this.discountPercent = Math.max(discountPercent, 0);
        this.stock = Math.max(stock, 0);
        this.status = status;
        this.rating = rating;
        this.reviews = Math.max(reviews, 0);
        this.badge = blankToNull(badge);
        this.imageUrl = resolveImageUrl(imageUrl);
        this.description = safeText(description);
        this.details = (details == null) ? List.of() : List.copyOf(details);
    }

    /**
     * 최소 버전 (imageUrl은 fallback으로 강제함)
     */
    public static ProductDetailView from(Product p) {
        String imageUrl = p.getImageUrl();
        return from(p, 0, imageUrl, List.of(), null);
    }

    /**
     * 확장 버전 (리뷰, 이미지, 스펙 추가용)
     */
    public static ProductDetailView from(Product p, int reviews, String imageUrl, List<String> details, String badgeOverride) {

        // 도메인 정책에 덜 의존하도록 구현 (DTO 레벨에서 계산)
        int discount = computeDiscountPercent(p.getPrice(), p.getOriginalPrice());
        String badge = (blankToNull(badgeOverride) != null)
                ? badgeOverride.trim()
                : resolveBadge(discount, reviews);

        return new ProductDetailView(
                p.getId(),
                p.getCategoryId(),
                p.getName(),
                p.getPrice(),
                p.getOriginalPrice(),
                discount,
                p.getStock(),
                p.getStatus(),
                p.getRating(),
                reviews,
                badge,
                imageUrl,
                p.getDescription(),
                details
        );
    }






    private static String safeText(String s) {
        return (s == null) ? "" : s;
    }

    private static String blankToNull(String s) {
        if(s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String resolveImageUrl(String imageUrl) {
        String v = blankToNull(imageUrl);
        return (v == null) ? FALLBACK_IMAGE_URL : v;
    }

    private static int computeDiscountPercent(int price, Integer originalPrice) {
        if(originalPrice == null) return 0;
        if(originalPrice <= 0) return 0;
        if(price < 0) return 0;
        if(price >= originalPrice) return 0;

        double ratio = 1.0 - (price * 1.0) / originalPrice;
        return (int) Math.round(ratio * 100);
    }

    /**
     * 뱃지 정책 (지금은 최소 구현)
     * 할인 있으면 SALE
     * 리뷰나 평점은 나중에
     */
    private static String resolveBadge(int discountPercent, int reviews) {
        if(discountPercent > 0) return "SALE";

//        if (reviews >= 100) return "BEST";
        return null;
    }
}
