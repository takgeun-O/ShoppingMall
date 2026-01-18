package io.github.takgeun.shop.product.application;

import io.github.takgeun.shop.category.domain.CategoryRepository;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor        // 필수 인자를 가진 생성자 자동 생성
public class ProductService {

    // CategoryRepository 주입해서 categoryId 존재 검증 예정
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // 카테고리별 상품 생성
    public Long create(Long categoryId, String name, int price, int stock, String description) {
        // 카테고리 존재 체크
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("카테고리가 존재하지 않습니다."));

        Product product = Product.create(categoryId, name, price, stock, description);
        Product saved = productRepository.save(product);

        return saved.getId();
    }

    // 단건 조회
    public Product get(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("상품이 존재하지 않습니다."));
    }

    // 카테고리별 목록 조회
    public List<Product> getByCategory(Long categoryId) {
        // 카테고리 존재 체크
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("카테고리가 존재하지 않습니다."));

        return productRepository.findAllByCategoryId(categoryId);
    }

    // 상품 수정 (부분 수정)
    // 들어온 값만 바꾸고, 안 들어온 값은 그대로 둘거니까 여기에 들어가는 모든 파라미터 타입들은 객체 타입이어야 한다.
    // 외부 입력(PATCH/DTO/Service 경계)에서는 Boolean, 도메인 내부(Entity)에서는 boolean
    public void update(Long productId, Long categoryId, String name,
                       Integer price, Integer stock, String description, Boolean active) {

        // 상품이 없을 경우 예외 처리
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("상품이 존재하지 않습니다."));
        // 카테고리 변경 시
        if(categoryId != null) {
            categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new NotFoundException("카테고리가 존재하지 않습니다."));
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
        // active 변경
        if(active != null) {
            if (active) product.activate();
            else product.deactivate();
        }

        // 메모리 저장소에서는 save 호출해줘야 덮어쓰기가 확실함
        productRepository.save(product);
    }

    // 삭제
    public void delete(Long productId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("상품이 존재하지 않습니다."));
        productRepository.deleteById(productId);
    }
}
