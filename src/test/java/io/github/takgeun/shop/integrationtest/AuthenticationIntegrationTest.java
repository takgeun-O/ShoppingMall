package io.github.takgeun.shop.integrationtest;

import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.order.view.OrderViewController;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthenticationIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 미로그인_사용자는_주문_페이지_접근_시_로그인으로_이동한다() throws Exception {

        mockMvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/login?next=/orders&reason=LOGIN_REQUIRED"));
    }

    @Test
    void 로그인_이동_시_원래_주소를_next로_전달한다() throws Exception {

        mockMvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> {
                    String location = result.getResponse()
                            .getRedirectedUrl();

                    assertThat(location).isNotNull();

                    String encodedNext = UriComponentsBuilder
                            .fromUriString(location)
                            .build()
                            .getQueryParams()
                            .getFirst("next");

                    assertThat(encodedNext).isNotNull();

                    String next = UriUtils.decode(
                            encodedNext,
                            StandardCharsets.UTF_8
                    );

                    assertThat(location).startsWith("/login");
                    assertThat(next).isEqualTo("/orders");
                });
    }

    @Test
    void 로그인한_일반_사용자는_주문_페이지에_접근할_수_있다() throws Exception {

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_MEMBER_ID, 1L);
        session.setAttribute(SessionConst.LOGIN_ROLE, MemberRole.USER);
        session.setAttribute(SessionConst.LOGIN_MEMBER_NAME, "일반회원");

        mockMvc.perform(get("/orders").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(handler().handlerType(OrderViewController.class))
                .andExpect(handler().methodName("orderRoot"))
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute(
                        "error",
                        "장바구니가 비어있습니다."
                ));
    }
}
