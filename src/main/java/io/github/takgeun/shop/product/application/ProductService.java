package io.github.takgeun.shop.product.application;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductRepository;
import io.github.takgeun.shop.product.domain.ProductStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor        // 필수 인자를 가진 생성자 자동 생성
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    // 카테고리별 상품 생성 (관리자)
    // 각 메서드 파라미터에 requestDTO로 받기보다는 컨트롤러에서 풀어넘기는 것이 좋다.
    // DTO는 원래 컨트롤러 경계에 가깝다보니 서비스가 DTO를 받으면 API 형태에 서비스가 끌려다닐 가능성이 생김.
    // 말 그대로 서비스는 유스케이스 구현만 신경써야함.
    public Long create(Long categoryId, String name, int price, int stock, String description) {

        // 카테고리 존재 검증 (카테고리 서비스 책임)
        categoryService.get(categoryId);

        Product product = Product.create(categoryId, name, price, stock, description);
        Product saved = productRepository.save(product);

        return saved.getId();
    }

    // 단건 조회
    public Product get(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("상품이 존재하지 않습니다."));
    }

    // 사용자(공개) 조회
    public Product getPublic(Long productId) {
        Product product = get(productId);
        if(!product.isPublicVisible()) {
            throw new NotFoundException("상품이 존재하지 않습니다.");
        }

        return product;
    }

    // 사용자(공개) 전체 목록 조회
    public List<Product> getAllPublic() {
        return productRepository.findAllPublic();
    }

    // 카테고리별 목록 조회 (숨김/종료된 상품은 안 보여주기)
    public List<Product> getAllPublicByCategoryId(Long categoryId) {
        categoryService.get(categoryId);        // 존재 검증
        return productRepository.findAllPublicByCategoryId(categoryId);
    }
    // 카테고리별 목록 조회 (관리자는 전체 보여주기)
    public List<Product> getByCategoryAdmin(Long categoryId) {
        categoryService.get(categoryId);
        return productRepository.findAllByCategoryId(categoryId);
    }

    // 상품 수정 (부분 수정)
    // 들어온 값만 바꾸고, 안 들어온 값은 그대로 둘거니까 여기에 들어가는 모든 파라미터 타입들은 객체 타입이어야 한다.
    public void update(Long productId, Long categoryId, String name, Integer price,
                       Integer stock, String description)
    {
        // 상품이 없을 경우 예외 처리
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("상품이 존재하지 않습니다."));

        // 카테고리 변경 시
        if(categoryId != null) {
//            categoryRepository.findById(categoryId)
//                    .orElseThrow(() -> new NotFoundException("카테고리가 존재하지 않습니다."));
            // 카테고리 존재 검증은 위에처럼 하기 보다는 카테고리 서비스 책임으로 두는 게 좋다. (단일 출처)
            // 예를 들어 비활성화되거나 삭제된 카테고리 같은 것들을 여러 서비스에서 각자 repo로 검사하기 시작하면
            // 코드 중복이 발생하고 누락이 발생할 가능성이 있기 때문임.
            categoryService.get(categoryId);
            product.changeCategory(categoryId);
        }
        // 상품명 변경 시
        if(name != null) {
            product.changeName(name);
        }
        // 가격 변경 시
        if(price != null) {
            product.changePrice(price);
        }
        // 재고 변경 시
        if(stock != null) {
            product.changeStock(stock);
        }
        // 상품설명 변경 시(빈 문자열은 상품설명 삭제)
        if(description != null) {
            product.changeDescription(description);
        }

        // 메모리 저장소에서는 save 호출해줘야 덮어쓰기가 확실함
        productRepository.save(product);
    }

    // 상태 변경
    public void changeStatus(Long productId, ProductStatus status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("상품이 존재하지 않습니다."));

        if(status == null) {
            throw new IllegalArgumentException("status는 필수입니다.");
        }

        switch (status) {
            case ON_SALE -> product.onSale();
            case HIDDEN -> product.hide();
            case DISCONTINUED -> product.discontinue();
            default -> throw new IllegalArgumentException("지원하지 않는 status 입니다.");
        }

        productRepository.save(product);        // 메모리 저장소 반영
    }

    public Product save(Product product) {
        if(product == null) {
            throw new IllegalArgumentException("productId는 필수입니다.");
        }
        return productRepository.save(product);
    }
}
