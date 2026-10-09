package com.shoerp.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class PageQuery {

    public static final int MAX_SIZE = 200;

    public static final int DEFAULT_SIZE = 10;

    private PageQuery() {
    }

    public static int normalizePage(int page) {
        return Math.max(page, 1);
    }

    public static int normalizeSize(int size) {
        if (size < 1) {
            throw new BusinessException("Page size must be greater than zero");
        }
        if (size > MAX_SIZE) {
            throw new BusinessException("Page size must not exceed " + MAX_SIZE);
        }
        return size;
    }

    public static PageRequest of(int page, int size) {
        return PageRequest.of(normalizePage(page) - 1, normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "id"));
    }
}
