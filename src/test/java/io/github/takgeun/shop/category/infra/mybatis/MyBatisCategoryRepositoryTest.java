package io.github.takgeun.shop.category.infra.mybatis;

import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.domain.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

//@SpringBootTest
@ActiveProfiles("mybatis")              // 없어도 되긴 한데 그래도 명시적으로 하는 게 좋음
@MybatisTest        // Mapper만 로딩
@Import(MyBatisCategoryRepository.class)    // Repository 수동 등록
class MyBatisCategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("save 후 findById 가능")
    void saveAndFindById() {
        // given
        Category category = Category.create("전자", "전자", null);

        // when
        Category saved = categoryRepository.save(category);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(categoryRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void findAllAdmin() {

        // given
        Category c1 = Category.create("전자", "전자", null);
        Category c2 = Category.create("의류", "의류", null);

        categoryRepository.save(c1);
        categoryRepository.save(c2);

        // when
        List<Category> result = categoryRepository.findAllAdmin();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("name")
                .containsExactly("전자", "의류");
    }

    @Test
    void findAllPublic() {

        // given
        Category root1 = Category.create("전자", "전자", null);
        Category root2 = Category.create("의류", "의류", null);

        categoryRepository.save(root1);
        categoryRepository.save(root2);

        Category child1 = Category.create("노트북", "노트북", root1.getId());
        Category child2 = Category.create("휴대폰", "휴대폰", root2.getId());

        categoryRepository.save(child1);
        categoryRepository.save(child2);

        // child2 비공개 처리
        child2.deactivate();
        categoryRepository.save(child2);

        // when
        List<Category> result = categoryRepository.findAllPublic();

        // then
        assertThat(result).extracting("name")
                .containsExactly("전자", "노트북", "의류");
    }

    @Test
    void existsByNameKeyExceptId() {

        // given
        Category c1 = Category.create("전자", "전자", null);
        Category c2 = Category.create("의류", "의류", null);

        categoryRepository.save(c1);
        categoryRepository.save(c2);

        // when & then
        assertThat(categoryRepository.existsByNameKeyExceptId("전자", c1.getId()))
                .isFalse();
        assertThat(categoryRepository.existsByNameKeyExceptId("전자", c2.getId()))
                .isTrue();
    }
}