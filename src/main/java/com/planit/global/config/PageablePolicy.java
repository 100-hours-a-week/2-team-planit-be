package com.planit.global.config;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
}

