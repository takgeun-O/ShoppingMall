package io.github.takgeun.shop.category.infra.mybatis;

import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.domain.CategoryStatus;
import io.github.takgeun.shop.product.infra.mybatis.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@MybatisTest
@ActiveProfiles({"test", "mybatis"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CategoryMapperTest {

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
    @DisplayName("카테고리 저장 후 ID가 생성되고 다시 조회된다")
    void insertAndFindById() {
        // given
        Category category = Category.create("전자", "전자", null);

        // when
        // TODO: H2에서 ck_category_status 제약조건과 충돌 발생
        // 실제 애플리케이션 테스트 또는 MySQL 환경에서 다시 검증 예정
        int result = categoryMapper.insert(category);

        // then
        assertThat(result).isEqualTo(1);
        assertThat(category.getId()).isNotNull();

        Category found = categoryMapper.findById(category.getId());
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(category.getId());
        assertThat(found.getName()).isEqualTo("전자");
        assertThat(found.getNameKey()).isEqualTo("전자");
        assertThat(found.getSlug()).isEqualTo("전자");
        assertThat(found.getParentId()).isNull();
        assertThat(found.getStatus()).isEqualTo(CategoryStatus.ACTIVE); // enum 매핑 성공 확인
    }

    @Test
    @DisplayName("전체 카테고리를 조회한다")
    void findAll() {
        // given
        Category c1 = Category.create("전자", "전자", null);
        Category c2 = Category.create("의류", "의류", null);

        categoryMapper.insert(c1);
        categoryMapper.insert(c2);

        // when
        List<Category> result = categoryMapper.findAll();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("name")
                .containsExactly("전자", "의류");
    }

    @Test
    @DisplayName("nameKey 중복 여부 확인")
    void existsByNameKey() {
        // given
        Category category = Category.create("전자", "전자", null);
        categoryMapper.insert(category);

        // when & then
        assertThat(categoryMapper.existsByNameKey("전자")).isTrue();
        assertThat(categoryMapper.existsByNameKey("dddd")).isFalse();
    }

    @Test
    @DisplayName("카테고리 삭제")
    void deletedById() {
        // given
        Category category = Category.create("전자", "전자", null);
        categoryMapper.insert(category);

        // when
        int result = categoryMapper.deleteById(category.getId());

        // then
        assertThat(result).isEqualTo(1);
        assertThat(categoryMapper.findById(category.getId())).isNull();
    }

    @Test
    @DisplayName("카테고리 수정")
    void update() {
        // given
        Category category = Category.create("전자", "전자", null);
        categoryMapper.insert(category);

        category.changeName("가전");
        category.changeSlug("가전");
        category.deactivate();

        // when
        int result = categoryMapper.update(category);

        // then
        assertThat(result).isEqualTo(1);

        Category found = categoryMapper.findById(category.getId());
        assertThat(found.getName()).isEqualTo("가전");
        assertThat(found.getNameKey()).isEqualTo("가전");
        assertThat(found.getSlug()).isEqualTo("가전");
        assertThat(found.getStatus()).isEqualTo(CategoryStatus.INACTIVE);
    }
}
