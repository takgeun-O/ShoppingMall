package io.github.takgeun.shop.product.infra.mybatis;

import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.infra.mybatis.CategoryMapper;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductRepository;
import io.github.takgeun.shop.product.domain.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@MybatisTest
@Import(MyBatisProductRepository.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MyBatisProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private ProductMapper productMapper;

    @BeforeEach
    void clear() {
        productMapper.deleteAll();
        categoryMapper.deleteAll();
    }

    @Test
    void saveAndFindById() {

        // given
        Category category = Category.create("전자", "electronics", null);
        categoryMapper.insert(category);

        Product product = Product.create(
                category.getId(),
                "맥북 프로",
                2500000,
                10,
                "애플 노트북",
                ProductStatus.ON_SALE,
                3000000,
                "macbook.jpg"
        );

        // when
        Product saved = productRepository.save(product);

        // then
        assertThat(saved.getId()).isNotNull();

        Optional<Product> found = productRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("맥북 프로");
        assertThat(found.get().getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    void findAllPublicByCategoryId() {

        // given
        Category electronic = Category.create("전자", "electronics", null);
        Category fashion = Category.create("의류", "fashion", null);
        categoryMapper.insert(electronic);
        categoryMapper.insert(fashion);

        Product product1 = Product.create(
                electronic.getId(),
                "맥북 프로",
                2500000,
                10,
                "애플 노트북",
                ProductStatus.ON_SALE,
                3000000,
                "macbook.jpg"
        );
        Product product2 = Product.create(
                electronic.getId(),
                "아이폰",
                1500000,
                5,
                "애플 휴대폰",
                ProductStatus.HIDDEN,
                2000000,
                "iphone.jpg"
        );
        Product product3 = Product.create(
                electronic.getId(),
                "셔츠",
                25000,
                15,
                "의류",
                ProductStatus.ON_SALE,
                30000,
                "shirt.jpg"
        );
        productRepository.save(product1);
        productRepository.save(product2);
        productRepository.save(product3);

        // when
        List<Product> result = productRepository.findAllPublicByCategoryId(electronic.getId());

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Product::getName)
                .containsExactly("셔츠", "맥북 프로");
    }

    @Test
    void findAllPublicByCategoryIds() {
        // given
        Category electronic = Category.create("전자", "electronics", null);
        Category fashion = Category.create("의류", "fashion", null);
        Category books = Category.create("도서", "books", null);
        categoryMapper.insert(electronic);
        categoryMapper.insert(fashion);
        categoryMapper.insert(books);

        Product product1 = Product.create(
                electronic.getId(),
                "맥북 프로",
                2500000,
                10,
                "애플 노트북",
                ProductStatus.ON_SALE,
                3000000,
                "macbook.jpg"
        );
        Product product2 = Product.create(
                fashion.getId(),
                "셔츠",
                25000,
                15,
                "의류",
                ProductStatus.HIDDEN,
                30000,
                "shirt.jpg"
        );
        Product product3 = Product.create(
                books.getId(),
                "만화",
                15000,
                0,
                "만화책",
                ProductStatus.SOLD_OUT,
                null,
                "book.jpg"
        );
        Product product4 = Product.create(
                books.getId(),
                "소설",
                15000,
                50,
                "소설책",
                ProductStatus.ON_SALE,
                null,
                "book2.jpg"
        );
        productRepository.save(product1);
        productRepository.save(product2);
        productRepository.save(product3);
        productRepository.save(product4);

        // when
        List<Product> result = productRepository.findAllPublicByCategoryIds(
                Set.of(
                        electronic.getId(), books.getId())
        );

        // then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(Product::getName)
                .containsExactly("소설", "만화", "맥북 프로");
    }

    @Test
    void update() {
        // given
        Category category = Category.create("전자", "electronics", null);
        categoryMapper.insert(category);

        Product product = Product.create(
                category.getId(),
                "맥북 프로",
                2500000,
                10,
                "애플 노트북",
                ProductStatus.ON_SALE,
                3000000,
                "macbook.jpg"
        );

        // when
        product.changeName("맥북 에어");
        product.changePrice(1500000);
        product.changeStock(5);
        product.hide();

        productRepository.save(product);

        Product found = productRepository.findById(product.getId()).get();

        // then
        assertThat(found.getName()).isEqualTo("맥북 에어");
        assertThat(found.getStatus()).isEqualTo(ProductStatus.HIDDEN);
    }

    @Test
    @DisplayName("카테고리 상품 존재 여부")
    void existsByCategoryId() {
        // given
        Category category = Category.create("전자", "electronics", null);
        categoryMapper.insert(category);

        Product product = Product.create(
                category.getId(),
                "맥북 프로",
                2500000,
                10,
                "애플 노트북",
                ProductStatus.ON_SALE,
                3000000,
                "macbook.jpg"
        );

        productRepository.save(product);

        assertThat(productRepository.existsAdminByCategoryId(category.getId()));
        assertThat(productRepository.existsAdminByCategoryId(999L)).isFalse();
    }
}