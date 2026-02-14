package io.github.takgeun.shop.category.api;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.api.dto.response.CategoryResponse;
import io.github.takgeun.shop.category.domain.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Stream;

/* HTTP 요청을 받아 Service에 위임하고 HTTP 응답으로 변환해서 돌려주는 역할
* 비즈니스 로직 X
* 검증/흐름 제어 O
* 도메인 조작 X*/

//생성 생공 -> 201 Created + body
//조회 성공 -> 200 OK + body
//수정 성공 -> 204 No Content
//삭제 성공 -> 204 No Content

@Validated          // @RequestParam 이나 @PathVariable 검증할 때 필요
@RestController                     // HTTP 요청을 처리하는데 반환값을 View가 아니라 JSON(Response Body) 로 보내고자 하는 의도
@RequiredArgsConstructor            // 필수 의존성만 받는 생성자를 자동으로 만들어주는 어노테이션
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getPublic(@PathVariable Long id) {
        return ResponseEntity.ok(CategoryResponse.from(categoryService.getPublic(id)));
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<CategoryResponse> getAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(CategoryResponse.from(categoryService.getAdmin(id)));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllPublic() {
        List<CategoryResponse> result = categoryService.getAllPublic().stream()   // stream() : List -> Stream<Category>
                .map(CategoryResponse::from)
                // .map(category -> CategoryResponse.from(category))
                // Category를 CategoryResponse 객체로 바꾼다.
                // Entity를 DTO 로 변환하는 책임을 DTO에 둔 설계임. (from() 메서드는 비즈니스 규칙보다는 표현/전송 관점의 변환 로직에 가까움)
                // 따라서 Entity를 DTO로 변환하는 책임은 DTO에 두는 설계가 좋음.
                .toList();
        // 컨트롤러는 Entity를 그대로 반환해서는 안되며
        // 응답 DTO로 변환해서 반환해야 한다. (Entity를 외부로 직접 노출하지 않도록 하기 위함)
        return ResponseEntity.ok(result);
    }

    @GetMapping("/admin")
    public ResponseEntity<List<CategoryResponse>> getAllAdmin() {
        List<CategoryResponse> result = categoryService.getAllAdmin().stream()
                .map(CategoryResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }
}
