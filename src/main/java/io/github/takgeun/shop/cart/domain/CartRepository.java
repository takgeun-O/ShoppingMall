package io.github.takgeun.shop.cart.domain;

import jakarta.servlet.http.HttpSession;

import java.util.Map;

public interface CartRepository {

    Map<Long, Integer> findAll(HttpSession session);
    int getQuantity(HttpSession session, Long productId);
    void put(HttpSession session, Long productId, int quantity);
    void remove(HttpSession session, Long productId);
    void clear(HttpSession session);
}
