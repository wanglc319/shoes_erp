package com.shoerp.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.shoerp.common.BusinessException;
import com.shoerp.entity.Inquiry;
import com.shoerp.entity.InquiryItem;
import com.shoerp.entity.PurchaseOrder;
import com.shoerp.entity.PurchaseOrderItem;
import com.shoerp.entity.Quotation;
import com.shoerp.entity.QuotationItem;
import com.shoerp.entity.Shoe;

/**
 * 单据 Excel 导出。统一生成 xlsx，支持在明细中嵌入鞋款图片。
 */
@Service
public class ExcelExportService {

    private static final int IMAGE_COLUMN_WIDTH = 18;

    private final FileStorageService fileStorageService;

    public ExcelExportService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public byte[] exportQuotation(Quotation quotation) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("PI");
            Styles styles = new Styles(workbook);
            String[] headers = {"Image", "Style No", "Description", "Material",
                    "Color", "Size", "Qty", "Unit", "Unit Price", "Amount"};
            int[] widths = {IMAGE_COLUMN_WIDTH, 18, 32, 24, 12, 12, 10, 8, 12, 14};

            int row = writeHeaderBlock(sheet, styles, "PROFORMA INVOICE (PI)",
                    new String[][]{
                            {"PI No:", nvl(quotation.getQuoteNo()), "Date:", nvl(quotation.getQuoteDate())},
                            {"Customer:", nvl(quotation.getCustomerName()), "Valid Until:", nvl(quotation.getValidUntil())},
                            {"Terms:", nvl(quotation.getTradeTerms()), "Currency:", nvl(quotation.getCurrency())},
                            {"Payment:", nvl(quotation.getPaymentTerms()), "Sales:", nvl(quotation.getSalesOwner())}
                    }, 10);
            row = writeTableHeader(sheet, styles, row, headers, widths);

            BigDecimal total = BigDecimal.ZERO;
            for (QuotationItem item : quotation.getItems()) {
                Row dataRow = sheet.createRow(row);
                dataRow.setHeightInPoints(60);
                setCell(dataRow, 0, "", styles.body, HorizontalAlignment.CENTER);
                addImage(workbook, sheet, item.getImageUrl(), row, 0);
                setCell(dataRow, 1, nvl(item.getStyleNo()), styles.body, HorizontalAlignment.LEFT);
                setCell(dataRow, 2, nvl(item.getProductName()), styles.body, HorizontalAlignment.LEFT);
                setCell(dataRow, 3, nvl(item.getMaterial()), styles.body, HorizontalAlignment.LEFT);
                setCell(dataRow, 4, nvl(item.getColor()), styles.body, HorizontalAlignment.LEFT);
                setCell(dataRow, 5, nvl(item.getSizeRange()), styles.body, HorizontalAlignment.CENTER);
                setCell(dataRow, 6, item.getQuantity() == null ? "" : String.valueOf(item.getQuantity()), styles.number, HorizontalAlignment.RIGHT);
                setCell(dataRow, 7, nvl(item.getUnit()), styles.body, HorizontalAlignment.CENTER);
                setCell(dataRow, 8, money(item.getUnitPrice()), styles.money, HorizontalAlignment.RIGHT);
                setCell(dataRow, 9, money(item.getAmount()), styles.money, HorizontalAlignment.RIGHT);
                if (item.getAmount() != null) {
                    total = total.add(item.getAmount());
                }
                row++;
            }
            writeTotalRow(sheet, styles, row, 9, total, quotation.getCurrency());
            return toBytes(workbook);
        } catch (IOException ex) {
            throw new BusinessException(500, "Failed to export PI: " + ex.getMessage());
        }
    }

    public byte[] exportPurchase(PurchaseOrder purchase) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Purchase Order");
            Styles styles = new Styles(workbook);
            String[] headers = {"Image", "Style No", "Description", "Material",
                    "Color", "Size", "Qty", "Unit", "Unit Price", "Amount"};
            int[] widths = {IMAGE_COLUMN_WIDTH, 18, 32, 24, 12, 12, 10, 8, 12, 14};

            int row = writeHeaderBlock(sheet, styles, "PURCHASE ORDER",
                    new String[][]{
                            {"PO No:", nvl(purchase.getPurchaseNo()), "Date:", nvl(purchase.getPurchaseDate())},
                            {"Supplier:", nvl(purchase.getSupplierName()), "Expected:", nvl(purchase.getExpectedDate())},
                            {"Currency:", nvl(purchase.getCurrency()), "Buyer:", nvl(purchase.getBuyer())},
                            {"Payment:", nvl(purchase.getPaymentTerms()), "Status:", nvl(purchase.getStatus())}
                    }, 11);
            row = writeTableHeader(sheet, styles, row, headers, widths);

            BigDecimal total = BigDecimal.ZERO;
            for (PurchaseOrderItem item : purchase.getItems()) {
                Row dataRow = sheet.createRow(row);
                dataRow.setHeightInPoints(60);
                setCell(dataRow, 0, "", styles.body, HorizontalAlignment.CENTER);
                addImage(workbook, sheet, item.getImageUrl(), row, 0);
                setCell(dataRow, 1, nvl(item.getStyleNo()), styles.body, HorizontalAlignment.LEFT);
                setCell(dataRow, 2, nvl(item.getProductName()), styles.body, HorizontalAlignment.LEFT);
                setCell(dataRow, 3, nvl(item.getMaterial()), styles.body, HorizontalAlignment.LEFT);
                setCell(dataRow, 4, nvl(item.getColor()), styles.body, HorizontalAlignment.LEFT);
                setCell(dataRow, 5, nvl(item.getSizeRange()), styles.body, HorizontalAlignment.CENTER);
                setCell(dataRow, 6, item.getQuantity() == null ? "" : String.valueOf(item.getQuantity()), styles.number, HorizontalAlignment.RIGHT);
                setCell(dataRow, 7, nvl(item.getUnit()), styles.body, HorizontalAlignment.CENTER);
                setCell(dataRow, 8, money(item.getUnitPrice()), styles.money, HorizontalAlignment.RIGHT);
                setCell(dataRow, 9, money(item.getAmount()), styles.money, HorizontalAlignment.RIGHT);
                if (item.getAmount() != null) {
                    total = total.add(item.getAmount());
                }
                row++;
            }
            writeTotalRow(sheet, styles, row, 9, total, purchase.getCurrency());
            return toBytes(workbook);
        } catch (IOException ex) {
            throw new BusinessException(500, "Failed to export purchase order: " + ex.getMessage());
        }
    }

    public byte[] exportInquiry(Inquiry inquiry) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("RFQ");
            Styles styles = new Styles(workbook);
            String[] headers = {"Image", "Style No", "Description", "Upper Material", "Sole Material",
                    "Color", "Size", "Qty", "Unit", "Target Price", "Quoted Price", "Remark"};
            int[] widths = {IMAGE_COLUMN_WIDTH, 18, 32, 18, 18, 12, 12, 10, 8, 12, 12, 20};

            int row = writeHeaderBlock(sheet, styles, "REQUEST FOR QUOTATION (RFQ)",
                    new String[][]{
                            {"RFQ No:", nvl(inquiry.getInquiryNo()), "Date:", nvl(inquiry.getInquiryDate())},
                            {"Supplier:", nvl(inquiry.getSupplierName()), "Deadline:", nvl(inquiry.getDeadline())},
                            {"Currency:", nvl(inquiry.getCurrency()), "Status:", nvl(inquiry.getStatus())},
                            {"Remark:", nvl(inquiry.getRemark()), "", ""}
                    }, 12);
            row = writeTableHeader(sheet, styles, row, headers, widths);

            List<InquiryItem> items = inquiry.getItems();
            for (InquiryItem item : items) {
                Row dataRow = sheet.createRow(row);
                dataRow.setHeightInPoints(60);
                setCell(dataRow, 0, "", styles.body, HorizontalAlignment.CENTER);
                addImage(workbook, sheet, item.getImageUrl(), row, 0);
                setCell(dataRow, 1, nvl(item.getStyleNo()), styles.body, HorizontalAlignment.LEFT);
                setCell(dataRow, 2, nvl(item.getProductName()), styles.body, HorizontalAlignment.LEFT);
                setCell(dataRow, 3, nvl(item.getUpperMaterial()), styles.body, HorizontalAlignment.LEFT);
                setCell(dataRow, 4, nvl(item.getSoleMaterial()), styles.body, HorizontalAlignment.LEFT);
                setCell(dataRow, 5, nvl(item.getColor()), styles.body, HorizontalAlignment.LEFT);
                setCell(dataRow, 6, nvl(item.getSizeRange()), styles.body, HorizontalAlignment.CENTER);
                setCell(dataRow, 7, item.getQuantity() == null ? "" : String.valueOf(item.getQuantity()), styles.number, HorizontalAlignment.RIGHT);
                setCell(dataRow, 8, nvl(item.getUnit()), styles.body, HorizontalAlignment.CENTER);
                setCell(dataRow, 9, money(item.getTargetPrice()), styles.money, HorizontalAlignment.RIGHT);
                setCell(dataRow, 10, money(item.getQuotedPrice()), styles.money, HorizontalAlignment.RIGHT);
                setCell(dataRow, 11, nvl(item.getRemark()), styles.body, HorizontalAlignment.LEFT);
                row++;
            }
            return toBytes(workbook);
        } catch (IOException ex) {
            throw new BusinessException(500, "Failed to export inquiry: " + ex.getMessage());
        }
    }

    /**
     * 样品贴纸：每行 3 张，每张 4 列 3 行，只含款号、材质、工厂和日期，不含价格。
     */
    public byte[] exportShoeStickers(List<Shoe> shoes, Map<Long, String> factoryCodes, int perRow) {
        int columns = Math.max(1, Math.min(4, perRow));
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Stickers");
            StickerStyles styles = new StickerStyles(workbook);
            for (int i = 0; i < columns * 4; i++) {
                sheet.setColumnWidth(i, 13 * 256);
            }
            int perBlockRows = 3;
            int perBlockCols = 4;
            for (int idx = 0; idx < shoes.size(); idx++) {
                int blockRow = (idx / columns) * perBlockRows;
                int blockCol = (idx % columns) * perBlockCols;
                writeSticker(sheet, styles, shoes.get(idx), factoryCodes, blockRow, blockCol,
                        perBlockRows, perBlockCols);
            }
            return toBytes(workbook);
        } catch (IOException ex) {
            throw new BusinessException(500, "Failed to export stickers: " + ex.getMessage());
        }
    }

    private void writeSticker(XSSFSheet sheet, StickerStyles styles, Shoe shoe,
                              Map<Long, String> factoryCodes, int blockRow, int blockCol,
                              int blockRows, int blockCols) {
        for (int r = 0; r < blockRows; r++) {
            Row row = sheet.getRow(blockRow + r);
            if (row == null) {
                row = sheet.createRow(blockRow + r);
            }
            for (int c = 0; c < blockCols; c++) {
                Cell cell = row.getCell(blockCol + c);
                if (cell == null) {
                    cell = row.createCell(blockCol + c);
                }
                cell.setCellStyle(styles.border);
            }
        }
        sheet.getRow(blockRow).setHeightInPoints(30);
        sheet.getRow(blockRow + 1).setHeightInPoints(18);
        sheet.getRow(blockRow + 2).setHeightInPoints(18);

        Cell styleNoCell = sheet.getRow(blockRow).getCell(blockCol);
        styleNoCell.setCellValue(nvl(shoe.getStyleNo()));
        styleNoCell.setCellStyle(styles.styleNo);
        sheet.addMergedRegion(new CellRangeAddress(blockRow, blockRow, blockCol, blockCol + blockCols - 1));

        Cell materialCell = sheet.getRow(blockRow + 1).getCell(blockCol);
        materialCell.setCellValue("Upper: " + nvl(shoe.getUpperMaterial())
                + "    Sole: " + nvl(shoe.getSoleMaterial())
                + "    Size: " + nvl(shoe.getSizeRange()));
        materialCell.setCellStyle(styles.body);
        sheet.addMergedRegion(new CellRangeAddress(blockRow + 1, blockRow + 1, blockCol, blockCol + blockCols - 1));

        String factory = factoryCodes.getOrDefault(shoe.getSupplierId(), "-");
        LocalDate created = shoe.getCreatedAt() == null ? LocalDate.now() : shoe.getCreatedAt().toLocalDate();
        Cell infoCell = sheet.getRow(blockRow + 2).getCell(blockCol);
        infoCell.setCellValue("Factory: " + factory + "    Date: " + created);
        infoCell.setCellStyle(styles.body);
        sheet.addMergedRegion(new CellRangeAddress(blockRow + 2, blockRow + 2, blockCol, blockCol + blockCols - 1));
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

    private int writeHeaderBlock(XSSFSheet sheet, Styles styles, String title,
                                 String[][] infoLines, int columnCount) {
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(30);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columnCount - 1));

        int rowIdx = 1;
        for (String[] line : infoLines) {
            Row row = sheet.createRow(rowIdx);
            int col = 0;
            for (String value : line) {
                Cell cell = row.createCell(col++);
                cell.setCellValue(value == null ? "" : value);
                cell.setCellStyle(styles.info);
            }
            rowIdx++;
        }
        return rowIdx + 1;
    }

    private int writeTableHeader(XSSFSheet sheet, Styles styles, int rowIdx, String[] headers, int[] widths) {
        Row row = sheet.createRow(rowIdx);
        row.setHeightInPoints(24);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.header);
            sheet.setColumnWidth(i, widths[i] * 256);
        }
        return rowIdx + 1;
    }

    private void writeTotalRow(XSSFSheet sheet, Styles styles, int rowIdx, int amountColumn,
                               BigDecimal total, String currency) {
        Row row = sheet.createRow(rowIdx);
        Cell label = row.createCell(amountColumn - 1);
        label.setCellValue("TOTAL " + (currency == null ? "" : currency));
        label.setCellStyle(styles.totalLabel);
        Cell value = row.createCell(amountColumn);
        value.setCellValue(total.doubleValue());
        value.setCellStyle(styles.money);
    }

    private void setCell(Row row, int col, String value, CellStyle style, HorizontalAlignment alignment) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        CellStyle copy = row.getSheet().getWorkbook().createCellStyle();
        copy.cloneStyleFrom(style);
        copy.setAlignment(alignment);
        cell.setCellStyle(copy);
    }

    private void addImage(XSSFWorkbook workbook, XSSFSheet sheet, String imageUrl, int rowIdx, int colIdx) {
        Path file = resolveImage(imageUrl);
        if (file == null) {
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(file);
            int type = pictureType(file);
            int pictureIdx = workbook.addPicture(bytes, type);
            XSSFDrawing drawing = sheet.getDrawingPatriarch();
            if (drawing == null) {
                drawing = sheet.createDrawingPatriarch();
            }
            XSSFClientAnchor anchor = new XSSFClientAnchor(40000, 40000, 40000, 40000,
                    colIdx, rowIdx, colIdx + 1, rowIdx + 1);
            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
            drawing.createPicture(anchor, pictureIdx);
        } catch (IOException ignored) {
            // 图片缺失不影响单据导出
        }
    }

    private Path resolveImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank() || !imageUrl.startsWith("/uploads/")) {
            return null;
        }
        String relative = imageUrl.substring("/uploads/".length());
        Path target = fileStorageService.getRoot().resolve(relative).normalize();
        if (!target.startsWith(fileStorageService.getRoot()) || !Files.exists(target)) {
            return null;
        }
        return target;
    }

    private int pictureType(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".png")) {
            return XSSFWorkbook.PICTURE_TYPE_PNG;
        }
        if (name.endsWith(".gif")) {
            return XSSFWorkbook.PICTURE_TYPE_GIF;
        }
        if (name.endsWith(".bmp")) {
            return XSSFWorkbook.PICTURE_TYPE_BMP;
        }
        return XSSFWorkbook.PICTURE_TYPE_JPEG;
    }

    private byte[] toBytes(XSSFWorkbook workbook) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private String nvl(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDate date) {
            return date.toString();
        }
        return String.valueOf(value);
    }

    private String money(BigDecimal value) {
        return value == null ? "" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static final class Styles {

        private final CellStyle title;

        private final CellStyle info;

        private final CellStyle header;

        private final CellStyle body;

        private final CellStyle number;

        private final CellStyle money;

        private final CellStyle totalLabel;

        private Styles(XSSFWorkbook workbook) {
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            title = workbook.createCellStyle();
            title.setFont(titleFont);
            title.setAlignment(HorizontalAlignment.CENTER);
            title.setVerticalAlignment(VerticalAlignment.CENTER);

            Font infoFont = workbook.createFont();
            infoFont.setFontHeightInPoints((short) 10);
            info = workbook.createCellStyle();
            info.setFont(infoFont);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            header = workbook.createCellStyle();
            header.setFont(headerFont);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            header.setBorderTop(BorderStyle.THIN);
            header.setBorderBottom(BorderStyle.THIN);
            header.setBorderLeft(BorderStyle.THIN);
            header.setBorderRight(BorderStyle.THIN);

            body = workbook.createCellStyle();
            body.setVerticalAlignment(VerticalAlignment.CENTER);
            body.setBorderTop(BorderStyle.THIN);
            body.setBorderBottom(BorderStyle.THIN);
            body.setBorderLeft(BorderStyle.THIN);
            body.setBorderRight(BorderStyle.THIN);

            number = workbook.createCellStyle();
            number.cloneStyleFrom(body);
            number.setAlignment(HorizontalAlignment.RIGHT);

            money = workbook.createCellStyle();
            money.cloneStyleFrom(body);
            money.setAlignment(HorizontalAlignment.RIGHT);
            money.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            totalLabel = workbook.createCellStyle();
            totalLabel.setAlignment(HorizontalAlignment.RIGHT);
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalLabel.setFont(totalFont);
        }
    }
}
