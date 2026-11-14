package com.shoerp.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoerp.common.BusinessException;
import com.shoerp.dto.ImportPreview;
import com.shoerp.entity.Shoe;
import com.shoerp.repository.ShoeRepository;

/**
 * PI 导入：把客户/工厂给的 Excel、CSV 或报价图片解析成明细行。
 * 只做预览，最终由用户确认后走正常下单流程。
 */
@Service
public class PiImportService {

    private static final int MAX_ROWS = 500;

    private static final int MAX_COLUMNS = 30;

    private static final Map<String, String> HEADER_ALIASES = headerAliases();

    private final ShoeRepository shoeRepository;

    private final OcrService ocrService;

    private final ObjectMapper objectMapper;

    public PiImportService(ShoeRepository shoeRepository, OcrService ocrService, ObjectMapper objectMapper) {
        this.shoeRepository = shoeRepository;
        this.ocrService = ocrService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ImportPreview preview(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Please choose a file");
        }
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        List<List<String>> table;
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            table = readExcel(file);
        } else if (name.endsWith(".csv") || name.endsWith(".txt")) {
            table = readCsv(file);
        } else {
            throw new BusinessException("Only Excel (.xlsx/.xls) or CSV files are supported");
        }
        return buildPreview(table, "file");
    }

    @Transactional(readOnly = true)
    public ImportPreview ocrPreview(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Please choose an image");
        }
        try {
            String text = ocrService.recognize(file.getBytes(), file.getContentType());
            return buildPreviewFromJson(text);
        } catch (IOException ex) {
            throw new BusinessException(400, "Failed to read image: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------- parsing

    private List<List<String>> readExcel(MultipartFile file) {
        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new BusinessException("The workbook has no sheet");
            }
            DataFormatter formatter = new DataFormatter();
            List<List<String>> table = new ArrayList<>();
            int lastRow = Math.min(sheet.getLastRowNum(), MAX_ROWS);
            for (int r = 0; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                List<String> values = new ArrayList<>();
                if (row != null) {
                    int lastCell = Math.min(row.getLastCellNum(), MAX_COLUMNS);
                    for (int c = 0; c < lastCell; c++) {
                        Cell cell = row.getCell(c);
                        values.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
                    }
                }
                table.add(values);
            }
            return table;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(400, "Failed to read Excel: " + ex.getMessage());
        }
    }

    private List<List<String>> readCsv(MultipartFile file) {
        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (text.startsWith("\uFEFF")) {
                text = text.substring(1);
            }
            List<List<String>> table = new ArrayList<>();
            for (String line : text.split("\\r?\\n")) {
                if (table.size() >= MAX_ROWS) {
                    break;
                }
                table.add(splitCsvLine(line));
            }
            return table;
        } catch (IOException ex) {
            throw new BusinessException(400, "Failed to read CSV: " + ex.getMessage());
        }
    }

    private List<String> splitCsvLine(String line) {
        List<String> values = new ArrayList<>();
        if (line == null) {
            return values;
        }
        char delimiter = line.contains("\t") ? '\t' : ',';
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == delimiter && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private ImportPreview buildPreview(List<List<String>> table, String source) {
        List<String> warnings = new ArrayList<>();
        if (table.isEmpty()) {
            return new ImportPreview(List.of(), List.of("The file is empty"), source);
        }
        int headerIndex = findHeaderRow(table);
        Map<String, Integer> columns = headerIndex >= 0
                ? mapColumns(table.get(headerIndex))
                : defaultColumns();
        if (headerIndex < 0) {
            warnings.add("No header row detected; assuming style, description, material, qty, price order");
        }
        List<ImportPreview.Row> rows = new ArrayList<>();
        for (int i = headerIndex + 1; i < table.size(); i++) {
            List<String> values = table.get(i);
            String styleNo = valueAt(values, columns.get("style"));
            if (styleNo.isBlank()) {
                continue;
            }
            ImportPreview.Row row = toRow(styleNo,
                    valueAt(values, columns.get("description")),
                    valueAt(values, columns.get("material")),
                    valueAt(values, columns.get("color")),
                    valueAt(values, columns.get("size")),
                    valueAt(values, columns.get("quantity")),
                    valueAt(values, columns.get("price")),
                    valueAt(values, columns.get("date")));
            if (row.shoeId() == null) {
                warnings.add("Style " + styleNo + " not found in samples");
            }
            rows.add(row);
        }
        if (rows.isEmpty()) {
            warnings.add("No data rows were recognised");
        }
        return new ImportPreview(rows, warnings, source);
    }

    private ImportPreview buildPreviewFromJson(String text) {
        List<String> warnings = new ArrayList<>();
        List<ImportPreview.Row> rows = new ArrayList<>();
        try {
            String json = text.trim();
            int start = json.indexOf('[');
            int end = json.lastIndexOf(']');
            if (start < 0 || end <= start) {
                return new ImportPreview(List.of(), List.of("OCR result is not a JSON array"), "ocr");
            }
            JsonNode array = objectMapper.readTree(json.substring(start, end + 1));
            for (JsonNode node : array) {
                String styleNo = textOf(node, "styleNo");
                if (styleNo.isBlank()) {
                    continue;
                }
                ImportPreview.Row row = toRow(styleNo,
                        textOf(node, "description"),
                        textOf(node, "material"),
                        textOf(node, "color"),
                        textOf(node, "sizeRange"),
                        textOf(node, "quantity"),
                        textOf(node, "unitPrice"),
                        textOf(node, "leadDate"));
                if (row.shoeId() == null) {
                    warnings.add("Style " + styleNo + " not found in samples");
                }
                rows.add(row);
            }
        } catch (Exception ex) {
            return new ImportPreview(List.of(), List.of("Failed to parse OCR result: " + ex.getMessage()), "ocr");
        }
        if (rows.isEmpty()) {
            warnings.add("No quote lines were recognised from the image");
        }
        return new ImportPreview(rows, warnings, "ocr");
    }

    private ImportPreview.Row toRow(String styleNo, String productName, String material, String color,
                                    String sizeRange, String quantity, String unitPrice, String leadDate) {
        Shoe shoe = shoeRepository.findByStyleNo(styleNo).orElse(null);
        String name = isBlank(productName) && shoe != null ? shoe.getNameEn() : productName;
        String mat = isBlank(material) && shoe != null ? shoe.getUpperMaterial() : material;
        String col = isBlank(color) && shoe != null ? shoe.getColor() : color;
        String size = isBlank(sizeRange) && shoe != null ? shoe.getSizeRange() : sizeRange;
        Integer qty = parseInt(quantity);
        if (qty == null && shoe != null && shoe.getMoq() != null) {
            qty = shoe.getMoq();
        }
        BigDecimal price = parseDecimal(unitPrice);
        if (price == null && shoe != null) {
            price = shoe.getSellingPrice();
        }
        return new ImportPreview.Row(styleNo, name, mat, col, size, qty, price, leadDate,
                shoe == null ? null : shoe.getId(), shoe != null);
    }

    private int findHeaderRow(List<List<String>> table) {
        int limit = Math.min(table.size(), 10);
        for (int i = 0; i < limit; i++) {
            long hits = table.get(i).stream()
                    .map(value -> normalise(value))
                    .filter(HEADER_ALIASES::containsKey)
                    .count();
            if (hits >= 2) {
                return i;
            }
        }
        return -1;
    }

    private Map<String, Integer> mapColumns(List<String> header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String key = HEADER_ALIASES.get(normalise(header.get(i)));
            if (key != null) {
                columns.putIfAbsent(key, i);
            }
        }
        return columns;
    }

    private Map<String, Integer> defaultColumns() {
        Map<String, Integer> columns = new HashMap<>();
        columns.put("style", 0);
        columns.put("description", 1);
        columns.put("material", 2);
        columns.put("quantity", 3);
        columns.put("price", 4);
        return columns;
    }

    private String valueAt(List<String> values, Integer index) {
        if (index == null || index < 0 || index >= values.size()) {
            return "";
        }
        return values.get(index) == null ? "" : values.get(index).trim();
    }

    private String textOf(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText("").trim();
    }

    private Integer parseInt(String value) {
        BigDecimal number = parseDecimal(value);
        return number == null ? null : number.intValue();
    }

    private BigDecimal parseDecimal(String value) {
        if (isBlank(value)) {
            return null;
        }
        String cleaned = value.replaceAll("[^0-9.\\-]", "");
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) {
            return null;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalise(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[\\s._:#-]", "").trim();
    }

    private static Map<String, String> headerAliases() {
        Map<String, String> aliases = new HashMap<>();
        add(aliases, "style", "style", "styleno", "stylenumber", "artno", "article", "item", "itemno",
                "model", "modelno", "款号", "型号", "货号", "编号");
        add(aliases, "description", "description", "desc", "name", "product", "productname", "itemname",
                "品名", "名称", "产品", "描述");
        add(aliases, "material", "material", "materials", "upper", "upper material", "材料", "材质", "面料");
        add(aliases, "color", "color", "colour", "颜色", "色号");
        add(aliases, "size", "size", "sizerange", "sizes", "尺码", "尺寸", "码数");
        add(aliases, "quantity", "qty", "quantity", "pcs", "prs", "pairs", "数量", "件数", "双数");
        add(aliases, "price", "price", "unitprice", "fob", "fobprice", "单价", "价格", "报价");
        add(aliases, "date", "date", "delivery", "deliverydate", "leadtime", "etd", "交期", "日期", "交货期");
        return aliases;
    }

    private static void add(Map<String, String> aliases, String key, String... values) {
        for (String value : values) {
            aliases.put(value.replaceAll("[\\s._:#-]", ""), key);
        }
    }
}
