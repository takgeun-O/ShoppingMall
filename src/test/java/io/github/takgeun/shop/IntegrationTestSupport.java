package io.github.takgeun.shop;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.order.domain.OrderRepository;
import io.github.takgeun.shop.order.infra.MemoryOrderRepository;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(Jackson2TestConfig.class)
public abstract class IntegrationTestSupport {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;

    @Autowired protected MemberService memberService;
    @Autowired protected CategoryService categoryService;
    @Autowired protected ProductService productService;

    @Autowired protected OrderRepository orderRepository;

    protected MockHttpSession sessionAsMember(Long memberId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_MEMBER_ID, memberId);
        return session;
    }

    protected Long givenActiveMember() {
        return memberService.signup(
                "user" + System.nanoTime() + "@Test.com",
                "pw12341234!",
                "테스트",
                "010-1111-2222"
        );
    }

    protected Long givenOnSaleProduct(int price, int stock) {
        Long categoryId = categoryService.create("전자" + System.nanoTime(), null);
        Long productId = productService.create(categoryId, "노트북", price, stock, "튼튼한 노트북");
        productService.changeStatus(productId, ProductStatus.ON_SALE);
        return productId;
    }

    @BeforeEach
    void setUp() {
        if(orderRepository instanceof MemoryOrderRepository mem) {
            mem.clear();
        }
    }
}
