package io.github.takgeun.shop.product.infra.mybatis;

import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.infra.mybatis.CategoryMapper;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductStatus;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@MybatisTest
class ProductMapperTest {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Test
    void insert() {

        // given
        Category category = initCategoryData();
        categoryMapper.insert(category);

        Product product = initProductData1(category.getId(), ProductStatus.ON_SALE);

        // when
        int result = productMapper.insert(product);

        // then
        assertThat(result).isEqualTo(1);
        assertThat(product.getId()).isNotNull();

        Optional<Product> found = productMapper.findById(product.getId());

        assertThat(found).isPresent();

        Product savedProduct = found.get();
        
        assertThat(savedProduct).isNotNull();
        assertThat(savedProduct.getId()).isEqualTo(product.getId());
        assertThat(savedProduct.getCategoryId()).isEqualTo(category.getId());
        assertThat(savedProduct.getName()).isEqualTo("맥북 프로");
        assertThat(savedProduct.getPrice()).isEqualTo(2500000);
        assertThat(savedProduct.getStock()).isEqualTo(10);
        assertThat(savedProduct.getDescription()).isEqualTo("애플 노트북");
        assertThat(savedProduct.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(savedProduct.getOriginalPrice()).isEqualTo(2700000);
        assertThat(savedProduct.getImageUrl()).isEqualTo("macbook.jpg");
        assertThat(savedProduct.getRating()).isEqualTo(0.0);
    }

    @Test
    void findPublicById() {

        // given
        Category category = initCategoryData();
        categoryMapper.insert(category);

        Product product = initProductData2(category.getId(), ProductStatus.ON_SALE);
        productMapper.insert(product);

        // when
        Optional<Product> found = productMapper.findPublicById(product.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("아이폰");
        assertThat(found.get().getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    void findPublicById_hiddenProduct() {

        // given
        Category category = initCategoryData();
        categoryMapper.insert(category);
        Product product = initProductData1(category.getId(), ProductStatus.HIDDEN);
        productMapper.insert(product);

        // when
        Optional<Product> found = productMapper.findPublicById(product.getId());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void findAllPublic() {

        // given
        Category category = initCategoryData();
        categoryMapper.insert(category);

        Product product1 = initProductData1(category.getId(), ProductStatus.ON_SALE);
        Product product2 = initProductData2(category.getId(), ProductStatus.ON_SALE);
        productMapper.insert(product1);
        productMapper.insert(product2);

        // when
        List<Product> result = productMapper.findAllPublic();

        // then
        assertThat(result).extracting(Product::getName)
                .containsExactly("아이폰", "맥북 프로");
    }

    @Test
    void findAllPublicByCategoryId() {

        // given
        Category category1 = initCategoryData();
        Category category2 = initCategoryData2();
        categoryMapper.insert(category1);
        categoryMapper.insert(category2);

        Product p1 = Product.create(category1.getId(), "전자1", 1000, 10, "설명", ProductStatus.ON_SALE, 1200, "1.jpg");
        Product p2 = Product.create(category1.getId(), "전자2", 2000, 0, "설명", ProductStatus.SOLD_OUT, 2200, "2.jpg");
        Product p3 = Product.create(category2.getId(), "의류1", 3000, 10, "설명", ProductStatus.ON_SALE, 3200, "3.jpg");
        Product p4 = Product.create(category1.getId(), "전자숨김", 4000, 10, "설명", ProductStatus.HIDDEN, 4200, "4.jpg");

        productMapper.insert(p1);
        productMapper.insert(p2);
        productMapper.insert(p3);
        productMapper.insert(p4);

        // when
        List<Product> result = productMapper.findAllPublicByCategoryId(category1.getId());

        // then
        assertThat(result).extracting(Product::getName)
                .contains("전자1", "전자2")
                .doesNotContain("의류1", "전자숨김");
    }

    @Test
    void findAllPublicByCategoryIds() {
        // given
        Category category1 = initCategoryData();
        Category category2 = initCategoryData2();
        Category category3 = initCategoryData3();
        categoryMapper.insert(category1);
        categoryMapper.insert(category2);
        categoryMapper.insert(category3);

        Product p1 = Product.create(category1.getId(), "전자1", 1000, 10, "설명", ProductStatus.ON_SALE, 1200, "1.jpg");
        Product p2 = Product.create(category2.getId(), "의류1", 2000, 5, "설명", ProductStatus.ON_SALE, 2200, "2.jpg");
        Product p3 = Product.create(category3.getId(), "식품1", 3000, 10, "설명", ProductStatus.ON_SALE, 3200, "3.jpg");
        Product p4 = Product.create(category1.getId(), "전자숨김", 4000, 10, "설명", ProductStatus.HIDDEN, 4200, "4.jpg");

        productMapper.insert(p1);
        productMapper.insert(p2);
        productMapper.insert(p3);
        productMapper.insert(p4);

        // when
        List<Product> result = productMapper.findAllPublicByCategoryIds(Set.of(category1.getId(), category3.getId()));

        // then
        assertThat(result).extracting(Product::getName)
                .contains("전자1", "식품1")
                .doesNotContain("의류1", "전자숨김");
    }

    @Test
    void findById() {
        // given
        Category category = initCategoryData();
        categoryMapper.insert(category);
        Product product = initProductData1(category.getId(), ProductStatus.HIDDEN);
        productMapper.insert(product);

        // when
        Optional<Product> found = productMapper.findById(product.getId());

        // then
        assertThat(found).isPresent();
    }

    @Test
    void findAllAdmin() {
        // given
        Category category = initCategoryData();
        categoryMapper.insert(category);

        Product product1 = initProductData1(category.getId(), ProductStatus.DISCONTINUED);
        Product product2 = initProductData2(category.getId(), ProductStatus.HIDDEN);
        productMapper.insert(product1);
        productMapper.insert(product2);

        // when
        List<Product> result = productMapper.findAllAdmin();

        // then
        assertThat(result).extracting(Product::getName)
                .containsExactly("아이폰", "맥북 프로");
    }

    @Test
    void findAllAdminByCategoryId() {
        // given
        Category category1 = initCategoryData();
        Category category2 = initCategoryData2();
        categoryMapper.insert(category1);
        categoryMapper.insert(category2);

        Product p1 = Product.create(category1.getId(), "전자1", 1000, 10, "설명", ProductStatus.ON_SALE, 1200, "1.jpg");
        Product p2 = Product.create(category1.getId(), "전자2", 2000, 0, "설명", ProductStatus.SOLD_OUT, 2200, "2.jpg");
        Product p3 = Product.create(category2.getId(), "의류1", 3000, 10, "설명", ProductStatus.ON_SALE, 3200, "3.jpg");
        Product p4 = Product.create(category1.getId(), "전자숨김", 4000, 10, "설명", ProductStatus.HIDDEN, 4200, "4.jpg");

        productMapper.insert(p1);
        productMapper.insert(p2);
        productMapper.insert(p3);
        productMapper.insert(p4);

        // when
        List<Product> result = productMapper.findAllAdminByCategoryId(category1.getId());

        // then
        assertThat(result).extracting(Product::getName)
                .contains("전자1", "전자2", "전자숨김");
    }

    @Test
    void findAllAdminByCategoryIds() {
        // given
        Category category1 = initCategoryData();
        Category category2 = initCategoryData2();
        Category category3 = initCategoryData3();
        categoryMapper.insert(category1);
        categoryMapper.insert(category2);
        categoryMapper.insert(category3);

        Product p1 = Product.create(category1.getId(), "전자1", 1000, 10, "설명", ProductStatus.ON_SALE, 1200, "1.jpg");
        Product p2 = Product.create(category2.getId(), "의류1", 2000, 5, "설명", ProductStatus.DISCONTINUED, 2200, "2.jpg");
        Product p3 = Product.create(category3.getId(), "식품1", 3000, 10, "설명", ProductStatus.ON_SALE, 3200, "3.jpg");
        Product p4 = Product.create(category1.getId(), "전자숨김", 4000, 10, "설명", ProductStatus.HIDDEN, 4200, "4.jpg");

        productMapper.insert(p1);
        productMapper.insert(p2);
        productMapper.insert(p3);
        productMapper.insert(p4);

        // when
        List<Product> result = productMapper.findAllAdminByCategoryIds(Set.of(category1.getId(), category3.getId()));

        // then
        assertThat(result).extracting(Product::getName)
                .contains("전자1", "식품1", "전자숨김");
    }

    private Category initCategoryData() {
        return Category.create("전자", "electronics", null);
    }
    private Category initCategoryData2() {
        return Category.create("의류", "wear", null);
    }
    private Category initCategoryData3() {
        return Category.create("식품", "food", null);
    }

    private Product initProductData1(Long categoryId, ProductStatus status) {

        return Product.create(
                categoryId,
                "맥북 프로",
                2500000,
                10,
                "애플 노트북",
                status,
                2700000,
                "macbook.jpg"
        );
    }

    private Product initProductData2(Long categoryId, ProductStatus status) {

        return Product.create(
                categoryId,
                "아이폰",
                1500000,
                5,
                "애플 스마트폰",
                status,
                1700000,
                "iphone.jpg"
        );
    }
}
