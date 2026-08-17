package uz.infosec.risk.service.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Cell styles for the exported workbook.
 *
 * <p>POI styles are workbook-scoped and capped (~64k), so they are created once
 * here and reused. Creating a style per cell in a loop is the classic way to
 * blow up an export on a large sheet.
 */
public class WorkbookStyles {

    public final CellStyle header;
    public final CellStyle text;
    public final CellStyle wrapped;
    public final CellStyle number;
    public final CellStyle percent;
    public final CellStyle date;
    public final CellStyle centered;
    public final CellStyle title;
    public final CellStyle note;

    public WorkbookStyles(XSSFWorkbook wb) {
        DataFormat fmt = wb.createDataFormat();

        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 10);

        header = wb.createCellStyle();
        header.setFont(headerFont);
        header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        header.setWrapText(true);
        border(header);

        text = wb.createCellStyle();
        text.setVerticalAlignment(VerticalAlignment.TOP);
        border(text);

        wrapped = wb.createCellStyle();
        wrapped.setWrapText(true);
        wrapped.setVerticalAlignment(VerticalAlignment.TOP);
        border(wrapped);

        number = wb.createCellStyle();
        number.setAlignment(HorizontalAlignment.CENTER);
        number.setVerticalAlignment(VerticalAlignment.TOP);
        border(number);

        percent = wb.createCellStyle();
        // 0.20 shows as 0.2 rather than 20% - the source file stores shares,
        // not percentages, and the formulas multiply by that share.
        percent.setDataFormat(fmt.getFormat("0.00"));
        percent.setAlignment(HorizontalAlignment.CENTER);
        border(percent);

        date = wb.createCellStyle();
        date.setDataFormat(fmt.getFormat("dd.mm.yyyy"));
        date.setAlignment(HorizontalAlignment.CENTER);
        border(date);

        centered = wb.createCellStyle();
        centered.setAlignment(HorizontalAlignment.CENTER);
        centered.setVerticalAlignment(VerticalAlignment.CENTER);
        border(centered);

        Font titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 12);
        title = wb.createCellStyle();
        title.setFont(titleFont);

        note = wb.createCellStyle();
        note.setWrapText(true);
        note.setVerticalAlignment(VerticalAlignment.TOP);
    }

    private void border(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
    }
}
