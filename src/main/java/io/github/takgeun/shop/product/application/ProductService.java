package io.github.takgeun.shop.product.application;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductRepository;
import io.github.takgeun.shop.product.domain.ProductStatus;

import io.github.takgeun.shop.product.view.dto.ProductCardView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor        // 필수 인자를 가진 생성자 자동 생성
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    // 목록 : categoryId(단일) + sort + role
    public List<Product> findForList(boolean admin, Long categoryId, String sort) {

        // base 조회
        List<Product> base;
        if(categoryId == null) {
            base = productRepository.findAll();
        } else {
            // 자손 포함 카테고리 id 구하기
            List<Long> categoryIds = admin
                    ? categoryService.findPublicDescendantIdsIncludingSelf(categoryId)
                    : categoryService.findAdminDescendantIdsIncludingSelf(categoryId);

            // IN 조회
            base = productRepository.findAllByCategoryIdIn(categoryIds);
        }

        log.info("base={}", base);

        // role 기반 노출 필터
        List<Product> visible = admin
                ? base
                : base.stream()
                .filter(Product::isPublicVisible)
                .toList();

        log.info("visible={}", visible);

        // sort 적용
        return applySort(visible, normalizeSort(sort));
    }

    // 상세 : role 기반 접근
    public Product getForDetail(boolean admin, Long productId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 상품입니다."));

        if(!admin && !p.isPublicVisible()) {
            throw new NotFoundException("존재하지 않는 상품입니다.");
        }

        return p;
    }

    // public 사용자 주문
    public Product getForOrderPublic(Long productId) {
        return getForDetail(false, productId);
    }

    // 생성 (관리자)
    public Long create(Long categoryId, String name, int price, int stock, String description) {
        Product product = Product.create(categoryId, name, price, stock, description);
        productRepository.save(product);
        return product.getId();
    }

    // 수정 (부분 수정)
    // null 인 항목은 수정하지 않음
    public void update(Long productId,
                       Long categoryId,
                       String name,
                       Integer price,
                       Integer stock,
                       String description) {

        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 상품입니다."));

        if(categoryId != null) p.changeCategory(categoryId);
        if(name != null) p.changeName(name);
        if(price != null) p.changePrice(price);
        if(stock != null) p.changeStock(stock);
        if(description != null) p.changeDescription(description);

        productRepository.save(p);
    }

    // 상태 변경(관리자)
    public void changeStatus(Long productId, ProductStatus status) {
        if(status == null) {
            throw new IllegalArgumentException("status는 필수입니다.");
        }

        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 상품입니다."));

        switch (status) {
            case ON_SALE -> p.onSale();
            case READY -> p.ready();
            case HIDDEN -> p.hide();
            case DISCONTINUED -> p.discontinue();
            case SOLD_OUT -> p.changeStock(0);  // 재고 0으로 만들기
        }

        productRepository.save(p);
    }

    public void save(Product product) {
        productRepository.save(product);
    }

    public void increaseStock(Long productId, int quantity) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 상품입니다."));
        p.increaseStock(quantity);
        productRepository.save(p);
    }

    /**
     * originalPrice 변경
     * originalPrice == null : 정가 제거(할인 없음)
     * originalPrice != null : price 이상이어야 함
     */
    public void changeOriginalPrice(Long productId, Integer originalPrice) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 상품입니다."));

        product.changeOriginalPrice(originalPrice);

        productRepository.save(product);
    }


    private String normalizeSort(String sort) {
        if(sort == null || sort.isBlank()) return "latest";
        return sort.trim();
    }

    private List<Product> applySort(List<Product> products, String sort) {
        List<Product> result = new ArrayList<>(products);

        Comparator<Product> cmp = switch (sort) {
            case "latest" -> Comparator.comparingLong((Product p) -> safeLong(p.getId())).reversed();       // 상품id가 높으면 최근에 만들어진 것.
            case "price-low" -> Comparator.comparingInt(Product::getPrice)
                    .thenComparingLong((Product p) -> safeLong(p.getId()));
            case "price-high" -> Comparator.comparingInt(Product::getPrice).reversed()
                    .thenComparingLong((Product p) -> safeLong(p.getId()));
            case "best", "rating" -> Comparator.comparingDouble(Product::ratingKey).reversed()
                    .thenComparingLong((Product p) -> safeLong(p.getId()));
            case "sale" -> Comparator.comparingInt(Product::discountPercent).reversed()
                    .thenComparingLong((Product p) -> safeLong(p.getId()));
            default -> Comparator.comparingLong((Product p) -> safeLong(p.getId())).reversed();
        };

        result.sort(cmp);
        return result;
    }

    private long safeLong(Long v) {
        return v == null ? 0L : v;
    }
}
