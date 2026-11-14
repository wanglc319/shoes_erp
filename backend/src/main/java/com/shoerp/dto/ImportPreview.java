package com.shoerp.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * PI 文件导入预览结果。前端确认后再创建报价单。
 */
public record ImportPreview(List<Row> rows, List<String> warnings, String source) {

    public record Row(String styleNo,
                      String productName,
                      String material,
                      String color,
                      String sizeRange,
                      Integer quantity,
                      BigDecimal unitPrice,
                      String leadDate,
                      Long shoeId,
                      boolean matched) {
    }
}
