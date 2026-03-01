package io.github.takgeun.shop.cart.infra;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class SessionCartRepository {

    private static final String CART_KEY = "SESSION_CART";  // 세션 저장 키

    public Map<Long, Integer> findAll(HttpSession session) {
        return new LinkedHashMap<>(getOrCreateCart(session));
    }

    public int getQuantity(HttpSession session, Long productId) {
        return getOrCreateCart(session).getOrDefault(productId, 0);
    }

    public void put(HttpSession session, Long productId, int quantity) {
        Map<Long, Integer> cart = getOrCreateCart(session);

        if(quantity <= 0) {
            cart.remove(productId);
        } else {
            cart.put(productId, quantity);
        }
        session.setAttribute(CART_KEY, cart);
    }

    public void remove(HttpSession session, Long productId) {
        Map<Long, Integer> cart = getOrCreateCart(session);
        cart.remove(productId);
        session.setAttribute(CART_KEY, cart);
    }

    public void clear(HttpSession session) {
        session.setAttribute(CART_KEY, new LinkedHashMap<Long, Integer>());
    }

    /**
     * 세션에서 cart 맵을 가져오거나 없으면 생성하기
     * session --> cart
     * Map<Long, Integer> --> [productId, quantity] : 어떤 상품을 몇 개만 담았는가? 에 대한 정보만
     */
    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getOrCreateCart(HttpSession session) {
        // session.getAttribute의 반환타입은 무조건 Object 임
        // 해당 세션에 이미 장바구니(Map)가 저장되어 있으면 그걸 그대로 사용한다.
        Object obj = session.getAttribute(CART_KEY);

        // 그냥 obj가 Map인지만 확인하는 방법.
        // <?, ?> : 제네릭 타입은 런타임에 사라지기 때문에 ? 을 사용해서 Map인지까지만 확인하고 넘어가기
        if(obj instanceof Map<?, ?>) {
            //
            return (Map<Long, Integer>) obj;        // 세션에 기존 존재하는 장바구니(obj)는 그대로 반환
        }
        Map<Long, Integer> cart = new LinkedHashMap<>();
        session.setAttribute(CART_KEY, cart);
        return cart;
    }
}
