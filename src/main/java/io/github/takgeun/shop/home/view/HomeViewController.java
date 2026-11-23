package io.github.takgeun.shop.home.view;

import io.github.takgeun.shop.global.view.ViewController;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.ProductStatus;
import io.github.takgeun.shop.product.view.dto.ProductCardView;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@ViewController
@RequiredArgsConstructor
public class HomeViewController {

    private final ProductService productService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featuredProducts", productService.findPublicList(null, "best").stream()
                .filter(product -> product.getStatus() == ProductStatus.ON_SALE && product.getStock() > 0)
                .limit(4)
                .map(ProductCardView::from)
                .toList());
        return "public/index";
    }
}
