package io.github.takgeun.shop.category.view.dto;

import java.util.List;

// sidebar 전용 트리 노드 DTO
public record CategoryNode(
        Long id,
        String name,
        List<CategoryNode> children
) {}
