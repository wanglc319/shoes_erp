package com.shoerp.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 免登录相册视图。只暴露展示所需字段，刻意不包含任何价格。
 */
public record PublicAlbum(String title, LocalDate expireAt, List<PublicShoe> shoes) {

    public record PublicShoe(String styleNo,
                             String name,
                             String nameEn,
                             String category,
                             String gender,
                             String upperMaterial,
                             String soleMaterial,
                             String color,
                             String sizeRange,
                             String description,
                             List<String> images) {
    }
}
