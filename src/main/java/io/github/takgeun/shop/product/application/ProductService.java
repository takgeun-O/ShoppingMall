package io.github.takgeun.shop.product.application;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.global.error.ConflictException;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductRepository;
import io.github.takgeun.shop.product.domain.ProductStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor        // 필수 인자를 가진 생성자 자동 생성
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    /**
     * 목록 조회
     * admin=true : 전체
     * admin=false : 공개 상품만 보여주기
     */
    public List<Product> findForList(boolean admin, Long categoryId, String sort) {

        // base 조회
        List<Product> base = findBaseProducts(admin, categoryId);


        log.info("base={}", base.stream()
                .map(Product::getName)
                .toList()
        );

        // sort 적용
        return applySort(base, normalizeSort(sort));
    }

    /**
     * 상품 상세보기 (조회용) - 주문 로직에서 쓰지 말기..
     * productService.getForDetail(true, productId) 로 잘못 쓸 수 있음.
     */
    public Product getForDetail(boolean admin, Long productId) {
        Product product = findById(productId);

        if(!admin && !product.isPublicVisible()) {
            throw new NotFoundException("판매 중인 상품만 조회할 수 있습니다.");
        }

        return product;
    }

    /**
     * 일반 사용자 주문용 조회
     */
    public Product getForOrderPublic(Long productId) {
        return getForDetail(false, productId);
    }

    /**
     * 주문 가능한 상품만 반환
     */
    public Product getForOrder(Long productId) {
        Product product = findById(productId);

        if(!product.isOrderable()) {
            throw new ConflictException("주문할 수 없는 상품입니다.");
        }

        return product;
    }

    /**
     * 상품 생성 (관리자)
     */
    @Transactional
    public Long create(
            Long categoryId,
            String name,
            Integer price,
            Integer stock,
            String description,
            ProductStatus status,
            Integer originalPrice,
            String imageUrl
    ) {

        validateCreateArgs(price, stock);       // 생성 시 가격, 재고 검증
        validateOriginalPrice(price, originalPrice);

        Product product = Product.create(
                categoryId,
                name,
                price,
                stock,
                description,
                status,
                originalPrice,
                imageUrl
        );

        Product saved = productRepository.save(product);
        return saved.getId();
    }

    /**
     * 상품 수정 (관리자)
     */
    @Transactional
    public void update(Long productId,
                       Long categoryId,
                       String name,
                       Integer price,
                       Integer stock,
                       String description,
                       ProductStatus status,
                       Integer originalPrice,
                       String imageUrl) {

        Product product = findById(productId);

        // 값이 들어온 것만 변경
        if (categoryId != null) product.changeCategory(categoryId);
        if (name != null) product.changeName(name);
        if (price != null) product.changePrice(price);
        if (description != null) product.changeDescription(description);
        if (originalPrice != null) product.changeOriginalPrice(originalPrice);
        if (imageUrl != null) product.changeImageUrl(imageUrl);
        if (stock != null) product.changeStock(stock);
        if (status != null) applyStatus(product, status);        // SOLD_OUT은 직접 변경 불가. (재고를 통해서만)

        productRepository.save(product);
    }

    /**
     * 상품 삭제 (관리자)
     */
    @Transactional
    public void delete(Long productId) {
        Product product = findById(productId);

        // 삭제 가능 여부 검증
        validateDeletable(product);

        productRepository.deleteById(productId);
    }


    // 상태 변경(관리자)
    @Transactional
    public void changeStatus(Long productId, ProductStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status는 필수입니다.");
        }

        Product product = findById(productId);

        applyStatus(product, status);

        productRepository.save(product);
    }

    @Transactional
    public void save(Product product) {
        productRepository.save(product);
    }

    @Transactional
    public void increaseStock(Long productId, int quantity) {
        Product product = findById(productId);
        product.increaseStock(quantity);
        productRepository.save(product);
    }

    /**
     * originalPrice 변경
     * originalPrice == null : 정가 제거(할인 없음)
     * originalPrice != null : price 이상이어야 함
     */
    @Transactional
    public void changeOriginalPrice(Long productId, Integer originalPrice) {
        Product product = findById(productId);

        product.changeOriginalPrice(originalPrice);

        productRepository.save(product);
    }


    private List<Product> findBaseProducts(boolean admin, Long categoryId) {
        if (categoryId == null) {
            return admin
                    ? productRepository.findAllAdmin()
                    : productRepository.findAllPublic();
        }

        // 자손 포함 카테고리 id 구하기
        Set<Long> categoryIds = admin
                ? categoryService.findAdminDescendantIdsIncludingSelf(categoryId)
                : categoryService.findPublicDescendantIdsIncludingSelf(categoryId);

        // IN 조회
        return admin
                ? productRepository.findAllAdminByCategoryIds(categoryIds)
                : productRepository.findAllPublicByCategoryIds(categoryIds);
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) return "latest";
        return sort.trim();
    }

    private List<Product> applySort(List<Product> products, String sort) {
        List<Product> base = new ArrayList<>(products);

        if("sale".equals(sort)) {
            base = new ArrayList<>(
                    base.stream()
                    .filter(p -> p.discountPercent() > 0)
                    .toList()
            );
        }

        Comparator<Product> cmp = switch (sort) {
            case "latest" ->
                    Comparator.comparingLong((Product p) -> safeLong(p.getId())).reversed();       // 상품id가 높으면 최근에 만들어진 것.
            case "price-low" -> Comparator.comparingInt(Product::getPrice)
                    .thenComparingLong((Product p) -> safeLong(p.getId()));     // 1순위 : 가격 오름차순, 2순위 : id 오름차순
            case "price-high" -> Comparator.comparingInt(Product::getPrice).reversed()
                    .thenComparingLong((Product p) -> safeLong(p.getId()));
            case "best", "rating" -> Comparator.comparingDouble(Product::getRatingValue).reversed()
                    .thenComparingLong((Product p) -> safeLong(p.getId()));
            case "sale" -> Comparator.comparingInt(Product::discountPercent).reversed()
                    .thenComparingLong((Product p) -> safeLong(p.getId()));
            default -> Comparator.comparingLong((Product p) -> safeLong(p.getId())).reversed();
        };

        base.sort(cmp);
        return base;
    }

    private long safeLong(Long v) {
        // Comparator는 null 비교에서 에러날 수 있음.
        return v == null ? 0L : v;
    }

    private Product findById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 상품입니다."));
    }

    private void validateCreateArgs(Integer price, Integer stock) {
        if (price == null) {
            throw new IllegalArgumentException("price는 필수입니다.");
        }
        if (stock == null) {
            throw new IllegalArgumentException("stock은 필수입니다.");
        }
    }

    private void validateOriginalPrice(Integer price, Integer originalPrice) {

        if (originalPrice == null) {
            return;
        }

        if (originalPrice <= 0) {
            throw new IllegalArgumentException("정가는 0보다 커야 합니다.");
        }

        if (price != null && originalPrice < price) {
            throw new IllegalArgumentException("정가는 판매가 이상이어야 합니다.");
        }
    }

    private void applyStatus(Product product, ProductStatus status) {
        switch (status) {
            case READY -> product.ready();
            case ON_SALE -> product.onSale();
            case HIDDEN -> product.hide();
            case DISCONTINUED -> product.discontinue();
            case SOLD_OUT -> throw new IllegalArgumentException("SOLD_OUT은 직접 변경할 수 없습니다. 재고를 통해 자동 반영됩니다.");
        }
    }

    private void validateDeletable(Product product) {
        // 판매 중 상품은 삭제 금지
        if(product.getStatus() == ProductStatus.ON_SALE) {
            throw new ConflictException("판매 중인 상품은 삭제할 수 없습니다. 먼저 숨김 또는 판매종료 처리해주세요.");
        }
    }
}
