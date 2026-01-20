# 프로젝트 소개

의류, 식품, 전자제품 등 광범위한 상품을 판매할 수 있는 쇼핑몰 프로젝트

# 핵심 기능 목록

핵심 기능은 추후 프로젝트를 점진적으로 발전시켜나갈 때마다 필요 시 추가한다.

회원

- 회원가입
- 로그인 / 로그아웃
- 내 정보 조회
- 회원 목록 조회 (관리자)

카테고리

- 카테고리 등록 (관리자)
- 카테고리 수정 (관리자)
- 카테고리 삭제 (관리자)
- 카테고리 목록 조회

상품

- 카테고리별 상품 등록 (관리자)
- 카테고리별 상품 목록 조회
- 상품 상세 조회

주문

- 주문 생성
- 주문 조회
- 주문 상세 조회

# 개발 흐름

프로젝트 기획부터 구현 단계까지 개발 흐름은 애자일 형태로 스프린트를 쪼개서 진행한다. 하나의 작업을 완벽하게 끝내고 다음 작업을 하는 게 아니라 앞뒤로 반복하면서 빠르게 작은 결과물을 만드는 식으로 해서 리팩토링 타이밍을 쉽게 잡도록 한다.

대략적인 작업 흐름은

1. 요구사항 작성
2. 유스케이스 작성 (동사 위주)
3. 도메인 모델 러프하게 작성
    
    엔티티명 + 핵심 속성
    
4. API 스펙 구성
5. DTO 정의
6. 유스케이스를 서비스 메서드로 구체화
7. 단위 테스트
8. 에러코드 및 검증 규칙 정의
9. 프론트 작업

이며 이 과정은 필요에 따라 서로 왕복한다.

# 기술 스택

- Java
- Java Spring
- ThymeLeaf
- HTML, CSS, JavaScript
- Swagger UI
- GitHub

# 실행 방법

# 아키텍처 / 패키지 구조

# API 문서 링크(Swagger)

# 테스트 방법

# 트러블 슈팅 / 회고

## v1.0 (메모리 리파지토리 구현)

### 카테고리 생성 시 name 입력을 하지 않았을 때 500 에러가 나는 경우

- 문제 상황
    - 카테고리생성요청DTO 에서 검증 애노테이션을 적용했음에도 불구하고 포스트맨으로 비어있는 name 필드로 생성 요청을 하였을 때 500 에러가 발생하였음.
    
    ![image.png](attachment:46d7f5dc-3786-437a-9af4-205ebe1e5601:image.png)
    
    - 기대값
        
        에러 코드 : 400
        
        에러 메시지 : “카테고리명은 필수입니다.”
        
    - 실제값
        
        에러 코드 : INTERNAL_SERVER_ERROR (500)
        
- 500 에러가 나는 원인 (흐름)
    1. 요청 “name”: “” 은 원래 컨트롤러에서 @Valid로 막혀야 정상임
    2. 그런데 이게 안 막히고 서비스로 내려가고 있음
    (=validation 의존성/설정 문제일 가능성이 큼)
    3. 서비스에서 existsByName(name) 을 호출하는데 “”는 MemoryCategoryRepository가 false를 반환 (이게 문제일 것으로 예상했으나 이게 문제는 아님…)
    4. 이후 Category.create(name, parentId) 혹은 changeName(name) 쪽에서 빈 문자열이면 IllegalArgumentException이 터질 가능성이 높고 이게 500 에러로 변환되고 있음.
        
        ```java
        // Category 도메인
        
        public static Category create(String name, Long parentId) {
                return new Category(name, parentId, true);
            }
        
            public void changeName(String name) {
                if(name == null || name.isBlank()) {
                    throw new IllegalArgumentException("카테고리 이름은 필수입니다.");
                }
                if(name.length() > 50) {
                    throw new IllegalArgumentException("카테고리 이름은 50자 이하입니다.");
                }
                this.name = name;
            }
        ```
        
        **→ 근본적인 원인은 @Valid가 먼저 막아야 하는 케이스가 서비스까지 내려오는 게 원인임을 파악하였음.**
        
    
- 기대 흐름 (정상 흐름)
    
    컨트롤러 @Valid 에서 @NotBlank로 걸러서 MethodArgumentNotValidException 이 터져서 400 에러로 응답하는 게 기대하는 흐름임.
    
- 해결방법
    - @Valid 가 정상 동작하지 않는 것 같음 → validation 의존성 추가 의심
    - 확인 결과 build.gradle 에서 validation 의존성이 빠짐을 확인
    - 의존성 추가
        
        ```java
        implementation 'org.springframework.boot:spring-boot-starter-validation'
        ```
        
    - 포스트맨 @Valid 작동 확인
        
        ![image.png](attachment:2e8a0aeb-5466-4bba-996b-934c35660e56:image.png)
        

해결방법은 너무나 허무했다… 의존성만 추가하면 해결이 되는 문제를 한동안 고생을 좀 했다.

그래도 @Valid의 작동 과정을 한번 더 정리하는 기회가 됐고 MemoryCategoryRepository에서 existsByName() 메서드의 로직 모순을 발견하여 개선하게 된 기회가 됐다.
