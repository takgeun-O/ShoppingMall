package io.github.takgeun.shop.product.view.dto;

import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductStatus;
import lombok.Getter;

@Getter
public class ProductCardView {

    private final Long id;
    private final String name;
    private final int price;
    private final Integer originalPrice;

    private final String imageUrl;     // 템플릿에서 p.imageUrl로 사용
    private final double rating;       // 템플릿에서 p.rating
    private final int reviews;         // 템플릿에서 p.reviews
    private final String badge;        // 템플릿에서 p.badge (BEST/NEW/SALE/HOT 등)
    private final ProductStatus status;

    private ProductCardView(Long id,
                            String name,
                            int price,
                            Integer originalPrice,
                            String imageUrl,
                            double rating,
                            int reviews,
                            String badge,
                            ProductStatus status) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.originalPrice = originalPrice;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.reviews = reviews;
        this.badge = badge;
        this.status = status;
    }

    /**
     * 기본 카드 변환
     * - 아직 이미지/리뷰/뱃지 도메인이 없으니 일단 합리적인 기본값으로 채움
     * - imageUrl이 없으면 기본 이미지 경로로 채우는 전략
     */
    public static ProductCardView from(Product p) {
        return from(p, p.getImageUrl(), 0, null);
    }

    /**
     * 확장용 팩토리
     * - 나중에 이미지/리뷰/뱃지/프로모션 로직이 생기면 여기에서 주입
     * 외부에서 반드시 from()을 거쳐야만 객체 생성 가능
     */
    public static ProductCardView from(Product p, String imageUrl, int reviews, String badge) {

        String resolvedImageUrl = (imageUrl == null || imageUrl.isBlank())
                ? "/images/no-image.png"   // static 경로에 넣어두기
                : imageUrl;

        int resolvedReviews = Math.max(reviews, 0);

        return new ProductCardView(
                p.getId(),
                p.getName(),
                p.getPrice(),
                p.getOriginalPrice(),
                resolvedImageUrl,
                p.getRating(),
                resolvedReviews,
                badge,
                p.getStatus()
        );
    }

    // 할인율 계산
    public int discountPercent() {
        if (originalPrice == null || originalPrice <= 0 || originalPrice <= price) {
            return 0;
        }
        return (int) Math.round((1 - (double) price / originalPrice) * 100);
    }

    // 레이팅
    public double ratingKey() {
        return rating;
    }
}