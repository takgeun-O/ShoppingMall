package io.github.takgeun.shop.product.application;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.domain.CategoryRepository;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductRepository;
import io.github.takgeun.shop.product.domain.ProductStatus;
import io.github.takgeun.shop.product.dto.request.ProductUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor        // 필수 인자를 가진 생성자 자동 생성
public class ProductService {

    // CategoryRepository 주입해서 categoryId 존재 검증 예정
    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    // 카테고리별 상품 생성
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
        if(product.getStatus() == ProductStatus.HIDDEN || product.getStatus() == ProductStatus.DISCONTINUED) {
            throw new NotFoundException("상품이 존재하지 않습니다.");
        }
        return product;
    }

    // 관리자 조회
    public Product getAdmin(Long productId) {
        return get(productId);
    }

    // 카테고리별 목록 조회
    public List<Product> getByCategory(Long categoryId) {
        // 카테고리 존재 검증 (카테고리 서비스 책임)
        categoryService.get(categoryId);

        return productRepository.findAllByCategoryId(categoryId);
    }

    // 상품 수정 (부분 수정)
    // 들어온 값만 바꾸고, 안 들어온 값은 그대로 둘거니까 여기에 들어가는 모든 파라미터 타입들은 객체 타입이어야 한다.
    public void update(Long productId, ProductUpdateRequest request)
    {
        // 상품이 없을 경우 예외 처리
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("상품이 존재하지 않습니다."));

        Long categoryId = request.getCategoryId();
        String name = request.getName();
        Integer price = request.getPrice();
        Integer stock = request.getStock();
        String description = request.getDescription();

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

    // 숨김 처리
    public void hide(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("상품이 존재하지 않습니다."));

        product.hide();
        productRepository.save(product);        // 메모리 저장소 반영
    }
}
