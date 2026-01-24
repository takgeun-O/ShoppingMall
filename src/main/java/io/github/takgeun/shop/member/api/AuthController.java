package io.github.takgeun.shop.member.api;

import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.application.AuthService;
import io.github.takgeun.shop.member.dto.request.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 로그인 (세션 생성)
    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request,
                                      HttpServletRequest httpRequest
    ) {
        Long memberId = authService.login(request.getEmail(), request.getPassword());

        // 세션 생성(없을 때) -> 로그인 때는 세션이 없으면 만들어야 함.
        HttpSession session = httpRequest.getSession(true);
        // true : 세션이 있으면 기존 세션을 반환한다. 세션이 없으면 새로운 세션을 생성해서 반환한다.
        // false : 세션이 있으면 기존 세션을 반환한다. 세션이 없으면 새로운 세션을 생성하지 않고 null을 반환한다.
        session.setAttribute(SessionConst.LOGIN_MEMBER_ID, memberId);

        return ResponseEntity.ok().build();
    }

    // 로그아웃 (세션 삭제)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);        // 세션이 없으면 null (로그아웃할 때는 세션이 없을 떄 만들 필요 없음)
        if(session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }
}
