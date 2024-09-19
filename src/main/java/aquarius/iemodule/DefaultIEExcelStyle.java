package aquarius.iemodule;

import org.apache.poi.ss.usermodel.*;

public class DefaultIEExcelStyle implements IEExcelStyle {
    @Override
    public void headerStyle(CellStyle style) {
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
    }

    @Override
    public void headerFont(Font font) {
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);

    }

    @Override
    public void oddStyle(CellStyle style) {
//        style.setFillBackgroundColor(IndexedColors.GREY_25_PERCENT.index);
//        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
//        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    @Override
    public void oddFont(Font font) {
        font.setBold(false);
        font.setFontHeightInPoints((short) 10);
    }

    @Override
    public void evenStyle(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    @Override
    public void evenFont(Font font) {
        oddFont(font);
    }

    @Override
    public void evenHypeLinkFont(Font font) {
        evenFont(font);
        font.setColor(IndexedColors.BLUE.getIndex());
        font.setUnderline(Font.U_SINGLE);
    }

    @Override
    public void oddHypeLinkFont(Font font) {
        oddFont(font);
        font.setColor(IndexedColors.BLUE.getIndex());
        font.setUnderline(Font.U_SINGLE);
    }


}
