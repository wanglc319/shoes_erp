package com.shoerp.controller;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shoerp.common.ApiResponse;
import com.shoerp.common.ExcelResponse;
import com.shoerp.common.PageResult;
import com.shoerp.entity.Shoe;
import com.shoerp.service.ExcelExportService;
import com.shoerp.service.ShoeService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/shoes")
public class ShoeController {

    private final ShoeService shoeService;

    private final ExcelExportService excelExportService;

    public ShoeController(ShoeService shoeService, ExcelExportService excelExportService) {
        this.shoeService = shoeService;
        this.excelExportService = excelExportService;
    }

    @GetMapping
    public ApiResponse<PageResult<Shoe>> page(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String category,
                                              @RequestParam(required = false) String gender,
                                              @RequestParam(required = false) Long supplierId,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(shoeService.page(keyword, category, gender, supplierId, page, size));
    }

    @GetMapping("/options")
    public ApiResponse<List<Shoe>> options() {
        return ApiResponse.success(shoeService.listEnabled());
    }

    @GetMapping("/next-style-no")
    public ApiResponse<String> nextStyleNo(@RequestParam(required = false) Long supplierId) {
        return ApiResponse.success(shoeService.nextStyleNo(supplierId));
    }

    /**
     * 手动录款号时查重：返回占用的样品信息，便于前端提示修改。
     */
    @GetMapping("/check-style-no")
    public ApiResponse<Map<String, Object>> checkStyleNo(@RequestParam String styleNo,
                                                         @RequestParam(required = false) Long excludeId) {
        Shoe existing = shoeService.findByStyleNo(styleNo);
        boolean conflict = existing != null && !existing.getId().equals(excludeId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("exists", conflict);
        if (conflict) {
            body.put("id", existing.getId());
            body.put("styleNo", existing.getStyleNo());
            body.put("name", existing.getName());
            body.put("createdAt", existing.getCreatedAt());
        }
        return ApiResponse.success(body);
    }

    /**
     * 样品贴纸导出（款号/材质/工厂/日期，不含价格）。
     */
    @GetMapping("/stickers/export")
    public void exportStickers(@RequestParam String ids,
                               @RequestParam(defaultValue = "3") int perRow,
                               HttpServletResponse response) throws java.io.IOException {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::valueOf)
                .toList();
        List<Shoe> shoes = shoeService.listByIds(idList);
        ExcelResponse.write(response,
                excelExportService.exportShoeStickers(shoes, shoeService.factoryNames(shoes), perRow),
                "sample-stickers.xlsx");
    }

    @GetMapping("/{id}")
    public ApiResponse<Shoe> get(@PathVariable Long id) {
        return ApiResponse.success(shoeService.getById(id));
    }

    @PostMapping
    public ApiResponse<Shoe> create(@RequestBody Shoe payload) {
        payload.setId(null);
        return ApiResponse.success(shoeService.save(payload));
    }

    @PutMapping("/{id}")
    public ApiResponse<Shoe> update(@PathVariable Long id, @RequestBody Shoe payload) {
        payload.setId(id);
        return ApiResponse.success(shoeService.save(payload));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        shoeService.delete(id);
        return ApiResponse.success();
    }
}
