package com.shoerp.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.shoerp.common.BusinessException;
import com.shoerp.common.ChineseMoney;
import com.shoerp.entity.Inquiry;
import com.shoerp.entity.InquiryItem;
import com.shoerp.entity.PurchaseOrder;
import com.shoerp.entity.PurchaseOrderItem;
import com.shoerp.entity.Quotation;
import com.shoerp.entity.QuotationItem;
import com.shoerp.entity.Shoe;
import com.shoerp.repository.ShoeRepository;

/**
 * 单据导出：按客户提供的三个模板版式生成 Excel。
 * PI（英文报价单）、采购合同（购销合同）、询价单（中文工厂报价单）。
 */
@Service
public class ExcelExportService {

    private static final int EMU_PER_PIXEL = 9525;

    private static final DateTimeFormatter DOT_DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private static final DateTimeFormatter SLASH_DATE = DateTimeFormatter.ofPattern("yyyy/M/d");

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String COMPANY = "温州市建社贸易有限公司";

    private static final String DEFAULT_REMARKS = "All Prices quoted are valid for 30 days from the date of issue.\n"
            + "The quotation will change according to the exchange rates,please check with us before order.";

    private final FileStorageService fileStorageService;

    private final ShoeRepository shoeRepository;

    public ExcelExportService(FileStorageService fileStorageService, ShoeRepository shoeRepository) {
        this.fileStorageService = fileStorageService;
        this.shoeRepository = shoeRepository;
    }

    // ------------------------------------------------------------------ PI

    public byte[] exportQuotation(Quotation quotation) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Quotation");
            DocStyles styles = new DocStyles(workbook, "Calibri", (short) 24, (short) 11, false);
            setWidths(sheet, new int[]{46, 24, 24, 18, 18, 18, 24});
            pageSetup(sheet);

            mergedRow(sheet, 0, 0, 6, 58,
                    "To " + nvl(quotation.getCustomerName()) + "  —  SINOPARK Quotation", styles.title);
            mergedRow(sheet, 1, 0, 6, 26,
                    "Dated on: " + dot(quotation.getQuoteDate()), styles.subtitle);

            String[] headers = {"Picture", "Style No.", "Upper", "Lining", "Insole", "Outsole",
                    "FOB price (" + currencyCode(quotation.getCurrency()) + "/pair)"};
            Row headerRow = row(sheet, 2, 38);
            for (int i = 0; i < headers.length; i++) {
                set(headerRow, i, headers[i], styles.header);
            }

            Map<Long, Shoe> shoes = loadShoes(quotation.getItems().stream()
                    .map(QuotationItem::getShoeId).toList());

            int rowIndex = 3;
            for (QuotationItem item : quotation.getItems()) {
                Row dataRow = row(sheet, rowIndex, 150);
                Shoe shoe = item.getShoeId() == null ? null : shoes.get(item.getShoeId());
                String upper = firstNonBlank(shoe == null ? null : shoe.getUpperMaterial(), item.getMaterial());
                set(dataRow, 1, nvl(item.getStyleNo()), styles.center);
                set(dataRow, 2, nvl(upper), styles.center);
                set(dataRow, 3, "", styles.center);
                set(dataRow, 4, "", styles.center);
                set(dataRow, 5, shoe == null ? "" : nvl(shoe.getSoleMaterial()), styles.center);
                set(dataRow, 6, priceText(quotation.getCurrency(), item.getUnitPrice()), styles.center);
                addPicture(workbook, sheet, item.getImageUrl(), rowIndex, 0, 1, 10, 6);
                rowIndex++;
            }

            int footer = rowIndex + 1;
            footer = footerLine(sheet, footer, "1.)MOQ", moqText(shoes), styles);
            footer = footerLine(sheet, footer, "2.)Packaging", "Box Packing", styles);
            footer = footerLine(sheet, footer, "3.)Loading port", "Ningbo", styles);

            row(sheet, footer, 22);
            row(sheet, footer + 1, 22);
            mergedCell(sheet, footer, 0, footer + 1, 1, "4.)Remarks", styles.footerLabel);
            mergedCell(sheet, footer, 2, footer + 1, 6,
                    firstNonBlank(quotation.getRemark(), DEFAULT_REMARKS), styles.footerValue);

            int sign = footer + 3;
            mergedRow(sheet, sign, 3, 6, 20, "（SINOPARK）", styles.signature);
            mergedRow(sheet, sign + 1, 3, 6, 20,
                    "（" + firstNonBlank(quotation.getSalesOwner(), "Nina") + "）", styles.signature);
            mergedRow(sheet, sign + 2, 3, 6, 20,
                    (quotation.getQuoteDate() == null ? LocalDate.now() : quotation.getQuoteDate()).toString(),
                    styles.signature);

            return toBytes(workbook);
        } catch (IOException ex) {
            throw new BusinessException(500, "Failed to export quotation: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------- purchase order

    public byte[] exportPurchase(PurchaseOrder order) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Purchase Order");
            DocStyles styles = new DocStyles(workbook, "宋体", (short) 22, (short) 10, true);
            setWidths(sheet, new int[]{18, 8, 8, 8, 14, 6, 7, 9, 12, 12, 12, 2, 10, 14, 14, 14, 10, 14, 14});
            pageSetup(sheet);

            mergedRow(sheet, 0, 0, 18, 40, "购  销  合  同", styles.title);
            row(sheet, 1, 20);
            set(row(sheet, 1, 20), 0, "甲方：", styles.label);
            mergedCell(sheet, 1, 1, 1, 11, COMPANY, styles.body);
            row(sheet, 2, 20);
            set(row(sheet, 2, 20), 0, "乙方：", styles.label);
            mergedCell(sheet, 2, 1, 2, 11, nvl(order.getSupplierName()), styles.body);
            row(sheet, 3, 12);
            mergedRow(sheet, 4, 0, 10, 20, "经双方协商签订本合同，并共同信守下列条款：", styles.body);

            Row header = row(sheet, 5, 30);
            String[] purchaseHeaders = {"品    名", "", "", "", "规  格", "计量单位", "数量", "单价(元)", "/",
                    "金额(元)", "交货日期"};
            for (int i = 0; i < purchaseHeaders.length; i++) {
                set(header, i, purchaseHeaders[i], styles.header);
            }
            merge(sheet, 5, 5, 0, 3);

            int rowIndex = 6;
            BigDecimal totalQuantity = BigDecimal.ZERO;
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (PurchaseOrderItem item : order.getItems()) {
                Row dataRow = row(sheet, rowIndex, 140);
                for (int c = 0; c <= 10; c++) {
                    set(dataRow, c, "", styles.body);
                }
                mergedCell(sheet, rowIndex, 0, rowIndex, 3, nvl(item.getStyleNo()), styles.body);
                set(dataRow, 4, nvl(item.getSizeRange()), styles.center);
                set(dataRow, 5, cnUnit(item.getUnit()), styles.center);
                setNumber(dataRow, 6, item.getQuantity() == null ? null : BigDecimal.valueOf(item.getQuantity()),
                        styles.quantity);
                setNumber(dataRow, 7, item.getUnitPrice(), styles.money);
                set(dataRow, 8, firstNonBlank(item.getRemark(), "见包装资料"), styles.center);
                setNumber(dataRow, 9, item.getAmount(), styles.money);
                set(dataRow, 10, slash(order.getExpectedDate()), styles.center);
                addPicture(workbook, sheet, item.getImageUrl(), rowIndex, 0, 4, 8, 26);
                totalQuantity = totalQuantity.add(item.getQuantity() == null
                        ? BigDecimal.ZERO : BigDecimal.valueOf(item.getQuantity()));
                totalAmount = totalAmount.add(item.getAmount() == null ? BigDecimal.ZERO : item.getAmount());
                rowIndex++;
            }

            int notesRow = rowIndex;
            mergedRow(sheet, notesRow, 0, 10, 18, "备注：1）商标：" + nvl(order.getBuyer())
                    + "    2）配码：见右表规格一栏", styles.small);
            mergedRow(sheet, notesRow + 1, 0, 10, 18,
                    "请送到我司指定地点：" + firstNonBlank(order.getRemark(), "宁波仓库"), styles.small);

            Row totalRow = row(sheet, notesRow + 2, 22);
            for (int c = 0; c <= 10; c++) {
                set(totalRow, c, "", styles.body);
            }
            mergedCell(sheet, notesRow + 2, 0, notesRow + 2, 4, "合        计", styles.center);
            set(totalRow, 5, "双", styles.center);
            setNumber(totalRow, 6, totalQuantity, styles.quantity);
            set(totalRow, 7, "", styles.center);
            set(totalRow, 8, "", styles.center);
            setNumber(totalRow, 9, totalAmount, styles.money);
            set(totalRow, 10, "", styles.center);

            BigDecimal amount = order.getTotalAmount() == null ? totalAmount : order.getTotalAmount();
            Row upperRow = row(sheet, notesRow + 3, 24);
            for (int c = 0; c <= 10; c++) {
                set(upperRow, c, "", styles.body);
            }
            mergedCell(sheet, notesRow + 3, 0, notesRow + 3, 2, "合计金额(大写)", styles.center);
            set(upperRow, 3, "人民币", styles.center);
            mergedCell(sheet, notesRow + 3, 4, notesRow + 3, 10, ChineseMoney.toUpper(amount), styles.body);

            int clauseRow = notesRow + 5;
            mergedRow(sheet, clauseRow, 0, 10, 20, "质量标准", styles.labelLeft);
            mergedRow(sheet, clauseRow + 1, 0, 10, 36,
                    "乙方严格按照客人确认样生产并商检合格。因为产品质量产生的任何问题，由乙方全权负责。", styles.small);

            int packRow = clauseRow + 3;
            mergedRow(sheet, packRow, 0, 10, 20, "包装要求，费用承担：", styles.labelLeft);
            mergedRow(sheet, packRow + 1, 0, 10, 24,
                    "乙方要严格按照甲方正式订单上的包装要求执行，包括：外箱、内盒、吊牌、包装纸等，其余按照常规出口标准。",
                    styles.small);
            mergedRow(sheet, packRow + 2, 0, 10, 20, "拖柜费、运输费和报关费由甲方承担。", styles.small);

            int acceptRow = packRow + 4;
            mergedRow(sheet, acceptRow, 0, 10, 20, "验收标准及提出异议的期限：", styles.labelLeft);
            mergedRow(sheet, acceptRow + 1, 0, 10, 24,
                    "验收标准：本合同货物外观、内在与包装质量标准必须符合甲方确认封样的水平和合同及订单的规定，如果不符，甲方有权拒收货物。",
                    styles.small);
            mergedRow(sheet, acceptRow + 2, 0, 10, 20, "提出异议的期限：自出货后12个月内。", styles.small);

            // 右侧：其他约定事项与签署栏
            int rightStart = clauseRow;
            mergedCell(sheet, rightStart, 12, rightStart + 3, 12, "其他\n约定\n事项", styles.header);
            mergedCell(sheet, rightStart, 13, rightStart, 18,
                    "1)所有材料都需要确认后,才能进行量产.材料都需要达到客人的标准.", styles.small);
            mergedCell(sheet, rightStart + 1, 13, rightStart + 1, 18,
                    "2)如出货或装货后才寄出船头样,而又发现样版和大货有问题的话,我司有权取消订单或要求赔偿.", styles.small);
            mergedCell(sheet, rightStart + 2, 13, rightStart + 2, 18,
                    "3)船头样每款每色37码1双，请于出货前七天提供给我司。", styles.small);

            int signRow = packRow + 4;
            mergedCell(sheet, signRow, 12, signRow, 14, "甲方：", styles.small);
            mergedCell(sheet, signRow, 16, signRow, 18, "乙方：", styles.small);
            String[] signLabels = {"法人代表：", "地址：", "电话：", "传真：", "开户银行：", "帐号："};
            for (int i = 0; i < signLabels.length; i++) {
                mergedCell(sheet, signRow + 1 + i, 12, signRow + 1 + i, 14, signLabels[i], styles.small);
                mergedCell(sheet, signRow + 1 + i, 16, signRow + 1 + i, 18, signLabels[i], styles.small);
            }

            return toBytes(workbook);
        } catch (IOException ex) {
            throw new BusinessException(500, "Failed to export purchase order: " + ex.getMessage());
        }
    }

    // --------------------------------------------------------------- inquiry

    public byte[] exportInquiry(Inquiry inquiry) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Quotation");
            DocStyles styles = new DocStyles(workbook, "宋体", (short) 20, (short) 10, true);
            setWidths(sheet, new int[]{8, 30, 15, 13, 13, 13, 18, 18});
            pageSetup(sheet);

            row(sheet, 0, 18);
            row(sheet, 1, 26);
            mergedCell(sheet, 2, 1, 3, 7,
                    "地址：娄桥镇中汇路81号A1幢14楼\n电话：0577-85690057  手机：13336983777  传真：0577-88428177",
                    styles.small);

            Row toRow = row(sheet, 4, 22);
            set(toRow, 1, "致：" + nvl(inquiry.getSupplierName()), styles.body);
            mergedCell(sheet, 4, 6, 4, 7,
                    dot(inquiry.getInquiryDate() == null ? LocalDate.now() : inquiry.getInquiryDate()), styles.center);

            mergedRow(sheet, 5, 1, 7, 30, "建社贸易工厂报价单", styles.title);

            String[] headers = {"工厂", "图片 / 款号", "帮面", "内里", "中底", "大底", "价格（含税含运费）", "包装"};
            Row headerRow = row(sheet, 6, 28);
            for (int i = 0; i < headers.length; i++) {
                set(headerRow, i, headers[i], styles.header);
            }

            int rowIndex = 7;
            for (InquiryItem item : inquiry.getItems()) {
                Row dataRow = row(sheet, rowIndex, 150);
                set(dataRow, 0, nvl(inquiry.getSupplierName()), styles.center);
                set(dataRow, 2, nvl(item.getUpperMaterial()), styles.center);
                set(dataRow, 3, nvl(item.getColor()), styles.center);
                set(dataRow, 4, "", styles.center);
                set(dataRow, 5, nvl(item.getSoleMaterial()), styles.center);
                set(dataRow, 6, priceText(inquiry.getCurrency(),
                        item.getTargetPrice() == null ? item.getQuotedPrice() : item.getTargetPrice()),
                        styles.center);
                set(dataRow, 7, nvl(item.getRemark()), styles.center);
                set(dataRow, 1, nvl(item.getStyleNo()), styles.topCenter);
                addPicture(workbook, sheet, item.getImageUrl(), rowIndex, 1, 1, 8, 28);
                rowIndex++;
            }

            int bottom = rowIndex + 1;
            mergedRow(sheet, bottom, 0, 7, 22,
                    "备注：以上报价请含税含运费，并注明包装方式与交货期。"
                            + (inquiry.getDeadline() == null ? "" : "  报价截止：" + slash(inquiry.getDeadline())),
                    styles.small);
            if (inquiry.getRemark() != null && !inquiry.getRemark().isBlank()) {
                mergedRow(sheet, bottom + 1, 0, 7, 22, inquiry.getRemark(), styles.small);
            }

            return toBytes(workbook);
        } catch (IOException ex) {
            throw new BusinessException(500, "Failed to export inquiry: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------- stickers

    /**
     * 样品贴纸：每行默认 3 张，只含款号、材质、工厂和日期，不含价格。
     */
    public byte[] exportShoeStickers(List<Shoe> shoes, Map<Long, String> factoryNames, int perRow) {
        int columns = Math.max(1, Math.min(4, perRow));
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Stickers");
            StickerStyles styles = new StickerStyles(workbook);
            for (int i = 0; i < columns * 4; i++) {
                sheet.setColumnWidth(i, 13 * 256);
            }
            int blockRows = 3;
            int blockCols = 4;
            for (int index = 0; index < shoes.size(); index++) {
                int blockRow = (index / columns) * blockRows;
                int blockCol = (index % columns) * blockCols;
                writeSticker(sheet, styles, shoes.get(index), factoryNames, blockRow, blockCol,
                        blockRows, blockCols);
            }
            return toBytes(workbook);
        } catch (IOException ex) {
            throw new BusinessException(500, "Failed to export stickers: " + ex.getMessage());
        }
    }

    private void writeSticker(XSSFSheet sheet, StickerStyles styles, Shoe shoe,
                              Map<Long, String> factoryNames, int blockRow, int blockCol,
                              int blockRows, int blockCols) {
        for (int r = 0; r < blockRows; r++) {
            Row row = row(sheet, blockRow + r, 0);
            for (int c = 0; c < blockCols; c++) {
                set(row, blockCol + c, "", styles.border);
            }
        }
        sheet.getRow(blockRow).setHeightInPoints(30);
        sheet.getRow(blockRow + 1).setHeightInPoints(18);
        sheet.getRow(blockRow + 2).setHeightInPoints(18);

        Cell styleNoCell = sheet.getRow(blockRow).getCell(blockCol);
        styleNoCell.setCellValue(nvl(shoe.getStyleNo()));
        styleNoCell.setCellStyle(styles.styleNo);
        merge(sheet, blockRow, blockRow, blockCol, blockCol + blockCols - 1);

        Cell materialCell = sheet.getRow(blockRow + 1).getCell(blockCol);
        materialCell.setCellValue("Upper: " + nvl(shoe.getUpperMaterial())
                + "    Sole: " + nvl(shoe.getSoleMaterial())
                + "    Size: " + nvl(shoe.getSizeRange()));
        materialCell.setCellStyle(styles.body);
        merge(sheet, blockRow + 1, blockRow + 1, blockCol, blockCol + blockCols - 1);

        String factory = factoryNames.getOrDefault(shoe.getSupplierId(), "-");
        LocalDate created = shoe.getCreatedAt() == null ? LocalDate.now() : shoe.getCreatedAt().toLocalDate();
        Cell infoCell = sheet.getRow(blockRow + 2).getCell(blockCol);
        infoCell.setCellValue("Factory: " + factory + "    Date: " + created);
        infoCell.setCellStyle(styles.body);
        merge(sheet, blockRow + 2, blockRow + 2, blockCol, blockCol + blockCols - 1);
    }

    // ----------------------------------------------------------------- utils

    private int footerLine(XSSFSheet sheet, int rowIndex, String label, String value, DocStyles styles) {
        row(sheet, rowIndex, 20);
        mergedCell(sheet, rowIndex, 0, rowIndex, 1, label, styles.footerLabel);
        mergedCell(sheet, rowIndex, 2, rowIndex, 6, value, styles.footerValue);
        return rowIndex + 1;
    }

    private Map<Long, Shoe> loadShoes(List<Long> shoeIds) {
        List<Long> ids = shoeIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return shoeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Shoe::getId, shoe -> shoe));
    }

    private String moqText(Map<Long, Shoe> shoes) {
        return shoes.values().stream()
                .map(Shoe::getMoq)
                .filter(Objects::nonNull)
                .findFirst()
                .map(moq -> moq + "/color")
                .orElse("400/color");
    }

    private String currencyCode(String currency) {
        return currency == null || currency.isBlank() ? "USD" : currency;
    }

    private String priceText(String currency, BigDecimal price) {
        if (price == null) {
            return "";
        }
        String code = currencyCode(currency);
        String symbol = switch (code) {
            case "USD" -> "$";
            case "CNY", "RMB" -> "￥";
            case "EUR" -> "€";
            case "GBP" -> "£";
            default -> code + " ";
        };
        return symbol + money(price);
    }

    private String money(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String cnUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return "双";
        }
        return switch (unit.trim().toUpperCase()) {
            case "PRS", "PAIR", "PAIRS" -> "双";
            default -> unit;
        };
    }

    private String dot(LocalDate date) {
        return date == null ? "" : date.format(DOT_DATE);
    }

    private String slash(LocalDate date) {
        return date == null ? "" : date.format(SLASH_DATE);
    }

    private String iso(LocalDate date) {
        return date == null ? "" : date.format(ISO_DATE);
    }

    private String nvl(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
    }

    private void setWidths(XSSFSheet sheet, int[] widths) {
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }

    private void pageSetup(XSSFSheet sheet) {
        sheet.getPrintSetup().setLandscape(true);
        sheet.getPrintSetup().setPaperSize(PrintSetup.A4_PAPERSIZE);
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
        sheet.setMargin(org.apache.poi.ss.usermodel.PageMargin.LEFT, 0.4);
        sheet.setMargin(org.apache.poi.ss.usermodel.PageMargin.RIGHT, 0.4);
        sheet.setMargin(org.apache.poi.ss.usermodel.PageMargin.TOP, 0.5);
        sheet.setMargin(org.apache.poi.ss.usermodel.PageMargin.BOTTOM, 0.5);
    }

    private Row row(XSSFSheet sheet, int index, float height) {
        Row row = sheet.getRow(index);
        if (row == null) {
            row = sheet.createRow(index);
        }
        if (height > 0) {
            row.setHeightInPoints(height);
        }
        return row;
    }

    private void set(Row row, int column, String value, CellStyle style) {
        Cell cell = row.getCell(column);
        if (cell == null) {
            cell = row.createCell(column);
        }
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void setNumber(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.getCell(column);
        if (cell == null) {
            cell = row.createCell(column);
        }
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private void merge(XSSFSheet sheet, int rowIndex, int lastRow, int firstCol, int lastCol) {
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, lastRow, firstCol, lastCol));
    }

    private void mergedRow(XSSFSheet sheet, int rowIndex, int firstCol, int lastCol, float height,
                           String value, CellStyle style) {
        row(sheet, rowIndex, height);
        for (int c = firstCol; c <= lastCol; c++) {
            set(sheet.getRow(rowIndex), c, "", style);
        }
        set(sheet.getRow(rowIndex), firstCol, value, style);
        merge(sheet, rowIndex, rowIndex, firstCol, lastCol);
    }

    private void mergedCell(XSSFSheet sheet, int firstRow, int firstCol, int lastRow, int lastCol,
                            String value, CellStyle style) {
        for (int r = firstRow; r <= lastRow; r++) {
            row(sheet, r, 0);
            for (int c = firstCol; c <= lastCol; c++) {
                set(sheet.getRow(r), c, "", style);
            }
        }
        set(sheet.getRow(firstRow), firstCol, value, style);
        merge(sheet, firstRow, lastRow, firstCol, lastCol);
    }

    /**
     * 按比例把图片放进单元格，避免拉伸变形。
     */
    private void addPicture(XSSFWorkbook workbook, XSSFSheet sheet, String url, int rowIndex,
                            int colIndex, int spanCols, int padding, int topReserve) {
        if (url == null || url.isBlank()) {
            return;
        }
        File file = resolveImage(url);
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return;
            }
            int columns = Math.max(1, spanCols);
            int[] columnWidths = new int[columns];
            int totalWidth = 0;
            for (int i = 0; i < columns; i++) {
                columnWidths[i] = (int) (sheet.getColumnWidth(colIndex + i) / 256.0 * 7);
                totalWidth += columnWidths[i];
            }
            Row row = sheet.getRow(rowIndex);
            float rowHeightPoints = row == null ? 60 : row.getHeightInPoints();
            int rowHeight = (int) (rowHeightPoints * 96 / 72);
            int maxWidth = Math.max(40, totalWidth - padding * 2);
            int maxHeight = Math.max(40, rowHeight - topReserve - padding);
            double scale = Math.min((double) maxWidth / image.getWidth(), (double) maxHeight / image.getHeight());
            int width = Math.max(20, (int) (image.getWidth() * scale));
            int height = Math.max(20, (int) (image.getHeight() * scale));
            int offsetX = Math.max(0, (totalWidth - width) / 2);
            int offsetY = Math.max(0, topReserve + (maxHeight - height) / 2);

            int lastCol = colIndex + columns - 1;
            int dx2 = offsetX + width;
            int accumulated = 0;
            for (int i = 0; i < columns; i++) {
                if (offsetX + width <= accumulated + columnWidths[i]) {
                    lastCol = colIndex + i;
                    dx2 = offsetX + width - accumulated;
                    break;
                }
                accumulated += columnWidths[i];
            }

            XSSFClientAnchor anchor = new XSSFClientAnchor(
                    offsetX * EMU_PER_PIXEL, offsetY * EMU_PER_PIXEL,
                    dx2 * EMU_PER_PIXEL, (offsetY + height) * EMU_PER_PIXEL,
                    colIndex, rowIndex, lastCol, rowIndex);
            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
            XSSFDrawing drawing = sheet.getDrawingPatriarch() != null
                    ? sheet.getDrawingPatriarch() : sheet.createDrawingPatriarch();
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(image, "png", png);
            drawing.createPicture(anchor, workbook.addPicture(png.toByteArray(), Workbook.PICTURE_TYPE_PNG));
        } catch (Exception ignored) {
            // 图片缺失或损坏不阻断导出
        }
    }

    private File resolveImage(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return null;
        }
        String relative = url.startsWith("/uploads/")
                ? url.substring("/uploads/".length())
                : url.replaceFirst("^/+", "");
        return fileStorageService.getRoot().resolve(relative).normalize().toFile();
    }

    private byte[] toBytes(XSSFWorkbook workbook) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static final class StickerStyles {

        private final CellStyle border;

        private final CellStyle styleNo;

        private final CellStyle body;

        private StickerStyles(XSSFWorkbook workbook) {
            border = workbook.createCellStyle();
            border.setBorderTop(BorderStyle.MEDIUM);
            border.setBorderBottom(BorderStyle.MEDIUM);
            border.setBorderLeft(BorderStyle.MEDIUM);
            border.setBorderRight(BorderStyle.MEDIUM);

            Font styleNoFont = workbook.createFont();
            styleNoFont.setBold(true);
            styleNoFont.setFontHeightInPoints((short) 16);
            styleNo = workbook.createCellStyle();
            styleNo.cloneStyleFrom(border);
            styleNo.setFont(styleNoFont);
            styleNo.setAlignment(HorizontalAlignment.CENTER);
            styleNo.setVerticalAlignment(VerticalAlignment.CENTER);

            Font bodyFont = workbook.createFont();
            bodyFont.setFontHeightInPoints((short) 9);
            body = workbook.createCellStyle();
            body.cloneStyleFrom(border);
            body.setFont(bodyFont);
            body.setAlignment(HorizontalAlignment.CENTER);
            body.setVerticalAlignment(VerticalAlignment.CENTER);
        }
    }

    private static final class DocStyles {

        private final CellStyle title;

        private final CellStyle subtitle;

        private final CellStyle header;

        private final CellStyle body;

        private final CellStyle center;

        private final CellStyle topCenter;

        private final CellStyle quantity;

        private final CellStyle money;

        private final CellStyle label;

        private final CellStyle labelLeft;

        private final CellStyle small;

        private final CellStyle footerLabel;

        private final CellStyle footerValue;

        private final CellStyle signature;

        private DocStyles(XSSFWorkbook workbook, String fontName, short titleSize, short bodySize,
                          boolean greyHeader) {
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints(titleSize);
            titleFont.setFontName(fontName);
            title = workbook.createCellStyle();
            title.setFont(titleFont);
            title.setAlignment(HorizontalAlignment.CENTER);
            title.setVerticalAlignment(VerticalAlignment.CENTER);

            Font subtitleFont = workbook.createFont();
            subtitleFont.setFontHeightInPoints((short) (bodySize + 1));
            subtitleFont.setFontName(fontName);
            subtitle = workbook.createCellStyle();
            subtitle.setFont(subtitleFont);
            subtitle.setAlignment(HorizontalAlignment.CENTER);
            subtitle.setVerticalAlignment(VerticalAlignment.CENTER);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints(bodySize);
            headerFont.setFontName(fontName);
            header = workbook.createCellStyle();
            header.setFont(headerFont);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            header.setWrapText(true);
            border(header);
            if (greyHeader) {
                header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }

            Font bodyFont = workbook.createFont();
            bodyFont.setFontHeightInPoints(bodySize);
            bodyFont.setFontName(fontName);
            body = workbook.createCellStyle();
            body.setFont(bodyFont);
            body.setAlignment(HorizontalAlignment.LEFT);
            body.setVerticalAlignment(VerticalAlignment.CENTER);
            body.setWrapText(true);
            border(body);

            center = clone(workbook, body);
            center.setAlignment(HorizontalAlignment.CENTER);

            topCenter = clone(workbook, center);
            topCenter.setVerticalAlignment(VerticalAlignment.TOP);

            quantity = clone(workbook, center);
            quantity.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));

            money = clone(workbook, center);
            money.setDataFormat(workbook.createDataFormat().getFormat("\"￥\"#,##0.00"));

            Font labelFont = workbook.createFont();
            labelFont.setBold(true);
            labelFont.setFontHeightInPoints(bodySize);
            labelFont.setFontName(fontName);
            label = workbook.createCellStyle();
            label.setFont(labelFont);
            label.setAlignment(HorizontalAlignment.RIGHT);
            label.setVerticalAlignment(VerticalAlignment.CENTER);

            labelLeft = clone(workbook, label);
            labelLeft.setAlignment(HorizontalAlignment.LEFT);

            Font smallFont = workbook.createFont();
            smallFont.setFontHeightInPoints((short) Math.max(8, bodySize - 1));
            smallFont.setFontName(fontName);
            small = workbook.createCellStyle();
            small.setFont(smallFont);
            small.setAlignment(HorizontalAlignment.LEFT);
            small.setVerticalAlignment(VerticalAlignment.CENTER);
            small.setWrapText(true);

            footerLabel = workbook.createCellStyle();
            footerLabel.setFont(labelFont);
            footerLabel.setAlignment(HorizontalAlignment.LEFT);
            footerLabel.setVerticalAlignment(VerticalAlignment.CENTER);

            Font footerFont = workbook.createFont();
            footerFont.setFontHeightInPoints(bodySize);
            footerFont.setFontName(fontName);
            footerValue = workbook.createCellStyle();
            footerValue.setFont(footerFont);
            footerValue.setAlignment(HorizontalAlignment.LEFT);
            footerValue.setVerticalAlignment(VerticalAlignment.CENTER);
            footerValue.setWrapText(true);

            Font signatureFont = workbook.createFont();
            signatureFont.setFontHeightInPoints((short) (bodySize + 1));
            signatureFont.setFontName(fontName);
            signature = workbook.createCellStyle();
            signature.setFont(signatureFont);
            signature.setAlignment(HorizontalAlignment.LEFT);
            signature.setVerticalAlignment(VerticalAlignment.CENTER);
        }

        private static CellStyle clone(XSSFWorkbook workbook, CellStyle source) {
            CellStyle target = workbook.createCellStyle();
            target.cloneStyleFrom(source);
            return target;
        }

        private static void border(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }
    }
}
