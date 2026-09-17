package com.shoerp.common;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Data;

@Data
public class PageResult<T> {

    private List<T> records;

    private long total;

    private int page;

    private int size;

    private int totalPages;

    public static <T> PageResult<T> of(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getContent());
        result.setTotal(page.getTotalElements());
        result.setPage(page.getNumber() + 1);
        result.setSize(page.getSize());
        result.setTotalPages(page.getTotalPages());
        return result;
    }

    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setTotalPages(size == 0 ? 0 : (int) Math.ceil((double) total / size));
        return result;
    }
}
