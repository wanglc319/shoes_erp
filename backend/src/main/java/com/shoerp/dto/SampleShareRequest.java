package com.shoerp.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SampleShareRequest {

    private String title;

    private List<Long> shoeIds;

    /**
     * 有效天数，0 或 null 表示长期有效。
     */
    private Integer expireDays;

    private String remark;
}
