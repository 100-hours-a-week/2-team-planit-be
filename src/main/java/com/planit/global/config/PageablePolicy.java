package com.planit.global.config;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class PageablePolicy {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private PageablePolicy() {
    }

    public static Pageable clamp(Pageable pageable, Sort defaultSort) {
        int safePage = pageable.getPageNumber() < 0 ? 0 : pageable.getPageNumber();
        int requestedSize = pageable.getPageSize() <= 0 ? DEFAULT_PAGE_SIZE : pageable.getPageSize();
        int safeSize = Math.min(requestedSize, MAX_PAGE_SIZE);
        Sort safeSort = pageable.getSort().isSorted() ? pageable.getSort() : defaultSort;
        return PageRequest.of(safePage, safeSize, safeSort);
    }

    public static Pageable clamp(Pageable pageable, Sort defaultSort, Set<String> allowedSortProperties) {
        int safePage = pageable.getPageNumber() < 0 ? 0 : pageable.getPageNumber();
        int requestedSize = pageable.getPageSize() <= 0 ? DEFAULT_PAGE_SIZE : pageable.getPageSize();
        int safeSize = Math.min(requestedSize, MAX_PAGE_SIZE);

        if (allowedSortProperties == null || allowedSortProperties.isEmpty()) {
            Sort fallbackSort = pageable.getSort().isSorted() ? pageable.getSort() : defaultSort;
            return PageRequest.of(safePage, safeSize, fallbackSort);
        }

        Set<String> allowed = allowedSortProperties.stream()
                .filter(property -> property != null && !property.isBlank())
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());

        List<Sort.Order> allowedOrders = pageable.getSort().stream()
                .filter(order -> allowed.contains(order.getProperty().toLowerCase()))
                .toList();

        Sort safeSort = allowedOrders.isEmpty() ? defaultSort : Sort.by(allowedOrders);
        return PageRequest.of(safePage, safeSize, safeSort);
    }
}
