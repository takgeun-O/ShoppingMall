package io.github.takgeun.shop.product.infra.mybatis;

import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@Profile("mybatis")
@RequiredArgsConstructor
public class MyBatisProductRepository implements ProductRepository {

    private final ProductMapper productMapper;

    @Override
    public Product save(Product product) {
        int affectedRows;

        if(product.getId() == null) {
            // 신규 생성
            affectedRows = productMapper.insert(product);
            if(affectedRows != 1) {
                throw new IllegalStateException("상품 저장에 실패했습니다.");
            }
        } else {
            affectedRows = productMapper.update(product);
            if(affectedRows != 1) {
                throw new IllegalStateException("상품 수정에 실패했습니다.");
            }
        }
        return product;
    }

    @Override
    public Optional<Product> findPublicById(Long id) {
        return productMapper.findPublicById(id);
    }

    @Override
    public List<Product> findAllPublic() {
        return productMapper.findAllPublic();
    }

    @Override
    public List<Product> findAllPublicByCategoryId(Long categoryId) {
        return productMapper.findAllPublicByCategoryId(categoryId);
    }

    @Override
    public List<Product> findAllPublicByCategoryIds(Set<Long> categoryIds) {
        if(categoryIds == null || categoryIds.isEmpty()) {
            // 비어있으면 DB까지 안 가니까 성능상 살짝 이득을 볼 수 있음.
            // DB에서 어차피 방어하지만 여기서도 한번 더 방어함으로써 안전하게 할 의도
            return List.of();
        }
        return productMapper.findAllPublicByCategoryIds(categoryIds);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(productMapper.findById(id));
    }

    @Override
    public List<Product> findAllAdmin() {
        return productMapper.findAllAdmin();
    }

    @Override
    public List<Product> findAllAdminByCategoryId(Long categoryId) {
        return productMapper.findAllAdminByCategoryId(categoryId);
    }

    @Override
    public List<Product> findAllAdminByCategoryIds(Set<Long> categoryIds) {
        if(categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }
        return productMapper.findAllAdminByCategoryIds(categoryIds);
    }

    @Override
    public boolean existsAdminByCategoryId(Long categoryId) {
        if(categoryId == null) {
            return false;
        }
        return productMapper.existsAdminByCategoryId(categoryId);
    }

    @Override
    public int countAdminByCategoryId(Long categoryId) {
        if(categoryId == null) {
            return 0;
        }
        return productMapper.countAdminByCategoryId(categoryId);
    }
}
