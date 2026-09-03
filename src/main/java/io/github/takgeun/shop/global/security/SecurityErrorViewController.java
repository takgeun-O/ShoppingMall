package io.github.takgeun.shop.global.security;

import io.github.takgeun.shop.global.view.ViewController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@ViewController
public class SecurityErrorViewController {

    @GetMapping("/security/forbidden")
    public String forbidden(
            HttpServletRequest request,
            HttpServletResponse response,
            Model model
    ) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        model.addAttribute("status", 403);
        model.addAttribute("error", "Forbidden");
        model.addAttribute(
                "message",
                request.getAttribute("securityErrorMessage"
                )
        );
        model.addAttribute(
                "path",
                request.getAttribute("securityErrorPath"
                )
        );

        return "error/403";
    }
}
