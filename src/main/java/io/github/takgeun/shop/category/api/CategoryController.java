package io.github.takgeun.shop.category.api;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.dto.request.CategoryCreateRequest;
import io.github.takgeun.shop.category.dto.request.CategoryUpdateRequest;
import io.github.takgeun.shop.category.dto.response.CategoryCreateResponse;
import io.github.takgeun.shop.category.dto.response.CategoryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;
    // 컨트롤러는 서비스에만 의존한다.
    // Repository/Entity 접근 X

    @PostMapping
    public ResponseEntity<CategoryCreateResponse> create(@Valid @RequestBody CategoryCreateRequest request) {
        /* @RequestBody : HTTP 요청 Body(JSON)를 자바 객체로 변환
         * 작동 과정
         * 1. 요청이 들어옴
         * 2. @RequestBody 발견
         * 3. HttpMessageConverter 작동
         * 4. Jackson(ObjectMapper)이 JSON 읽음
         * 5. CategoryCreateRequest 객체 생성
         * 6. 필드 주입 */
        // Service 호출 시 컨트롤러는 값만 전달한다.
        // 로직 판단이나 규칙을 넣지 않음.
        Long id = categoryService.create(request.getName(), request.getParentId());

        // ResponseEntity : HTTP 응답 전체(상태코드 + 헤더 + 바디)를 표현하는 객체
        // ResponseEntity 없이 쓰면
        // return new CategoryCreateResponse(id); --> 상태코드: 200 OK (기본값)으로 되어 헤더 제어 불가
        return ResponseEntity.status(HttpStatus.CREATED)        // 상태코드 직접 지정
                .body(new CategoryCreateResponse(id));          // 응답바디(JSON)
        // 응답DTO --> JSON 변환한다. (Jackson이 직렬화함)
        // @RestController + @ResponseBody(@RestController에 이미 포함) 의 역할 덕분임.
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(CategoryResponse.from(categoryService.get(id)));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll() {
        List<CategoryResponse> result = categoryService.getAll().stream()   // stream() : List -> Stream<Category>
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


    // Void : 응답 바디가 없다는 것을 명시한다. (JSON 응답 X, 데이터 반환 X, 오직 상태코드만 전달) 스프링은 기본적으로 200 반환하는데 다른 상태코드 반환하기 위해서
    // 수정 API에서 Void를 사용하는 이유 : PATCH / PUT의 관례
    // 리소스 수정, 성공 여부만 중요, 수정된 데이터 전체를 다시 줄 필요 없음.
    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryUpdateRequest request) {
        categoryService.update(id, request.getName(), request.getParentId(), request.getActive());
        return ResponseEntity.noContent().build();
        // HTTP 상태 코드는 204 No Content (요청은 성공했고 응답 본문은 없다.)
        // .build() : ResponseEntity 객체 생성 완료
    }

    // Void : 응답 바디가 없다는 것을 명시한다. (JSON 응답 X, 데이터 반환 X, 오직 상태코드만 전달) 스프링은 기본적으로 200 반환하는데 다른 상태코드 반환하기 위해서
    @DeleteMapping("/{id}")     // HTTP DELETE 메서드 처리
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);     // 컨트롤러는 삭제하라고 지시만 하고 실제 정책은 서비스가 책임진다. (컨트롤러는 정책을 모름)
        return ResponseEntity.noContent().build();
        // HTTP 상태 코드는 204 No Content (요청은 성공했고 응답 본문은 없다.)
        // .build() : ResponseEntity 객체 생성 완료
    }
}
