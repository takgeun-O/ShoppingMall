package io.github.takgeun.shop.order;

import io.github.takgeun.shop.IntegrationTestSupport;
import io.github.takgeun.shop.order.dto.request.OrderCreateRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.*;

class OrderE2EIntegrationTest extends IntegrationTestSupport {

    @Test
    void 주문_생성_후_상세조회_E2E_성공() throws Exception {

        // given
        Long memberId = givenActiveMember();
        Long productId = givenOnSaleProduct(1000, 10);
        MockHttpSession session = sessionAsMember(memberId);

        OrderCreateRequest request = new OrderCreateRequest();

        // 주문 생성 API에 보낼 요청 JSON 바디를 만들기
        String json = """
                {
                "productId": %d,
                "quantity": 2,
                "recipientName": "테스트",
                "recipientPhone": "010-1111-2222",
                "shippingZipCode": "12345",
                "shippingAddress": "서울시 영등포구",
                "requestMessage": "문앞"
                }
                """.formatted(productId);

        // when : MockMvc로 POST 요청 수행
        String createBody = mockMvc.perform(post("/orders")     // POST /orders 엔드포인트 호출
                        .session(session)                                   // 테스트용 HttpSession을 요청에 포함
                        .contentType(MediaType.APPLICATION_JSON)            // 요청바디가 JSON임을 명시, Controller의 @RequestBody가 정상 작동하게 함
                        .content(json))                                     // 위에서 만든 JSON 문자열을 HTTP 요청 body로 넣음
        // then : 응답 검증
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())        // $ : JSON 루트, .orderId : 해당 필드
                .andReturn()                                        // MvcResult 반환
                .getResponse()                  // MockHttpServletResponse
                .getContentAsString();          // 응답 body(JSON)를 String으로 추출


        // 주문 ID
        Long orderId = objectMapper.readTree(createBody).get("orderId").asLong();

        // when: 주문 상세 조회
        mockMvc.perform(get("/orders/{orderId}", orderId)
                .session(session))
        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.totalPrice").value(2000));
    }

    @Test
    void 주문_생성_실패_로그인없음_401() throws Exception {

        // given
        Long productId = givenOnSaleProduct(1000, 10);

        String json = """
                {
                    "productId": %d,
                    "quantity": 1,
                    "recipientName": "테스트",
                    "recipientPhone": "010-1111-2222",
                    "shippingZipCode": "12345",
                    "shippingAddress": "서울시 영등포구",
                    "requestMessage": null
                }
                """.formatted(productId);

        // when


        // then
        mockMvc.perform(post("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 주문_생성_실패_재고부족_409_그리고_재고변화없음() throws Exception {

        // given
        Long memberId = givenActiveMember();
        Long productId = givenOnSaleProduct(1000, 2);
        MockHttpSession session = sessionAsMember(memberId);

        int beforeStock = productService.get(productId).getStock();

        String json = """
                {
                    "productId": %d,
                    "quantity": 10,
                    "recipientName": "테스트",
                    "recipientPhone": "010-1111-2222",
                    "shippingZipCode": "12345",
                    "shippingAddress": "서울시 영등포구",
                    "requestMessage": null
                }
                """.formatted(productId);

        // when : MockMvc로 POST 요청 수행
        mockMvc.perform(post("/orders")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());

        // then
        int afterStock = productService.get(productId).getStock();
        assertEquals(beforeStock, afterStock);
    }
}
