package aquarius.iemodule.impl;

import aquarius.iemodule.DefaultIEExcelStyle;
import aquarius.iemodule.IEContext;
import aquarius.iemodule.IEExcelStyle;
import aquarius.iemodule.exception.NotAcceptableReportTypeException;
import aquarius.iemodule.impl.util.ExcelSheetConfigurer;
import aquarius.iemodule.processors.ExportProcessor;
import aquarius.iemodule.structure.*;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class DefaultExcelExportProcessor<T extends Exportable<T> & ExcelSheetConfigurer<T>> extends DefaultExcelProcessor implements ExportProcessor<T> {

    private XSSFWorkbook workbook;
    private ReportableEntity reportableEntity;

    private List<SheetAtrribute<T>> sheetAtrributes = new ArrayList<>();

    private IEExcelStyle ieExcelStyle;

    private String fileName;
    private CellStyle headerStyle;
    private CellStyle oddRowStyle;
    private CellStyle evenRowStyle;

    private CellStyle oddHyperLinkStyle;
    private CellStyle evenHyperLinkStyle;

    private final Class<T> CLAZZ;

    public DefaultExcelExportProcessor(IEExcelStyle ieExcelStyle, String fileName, Class<T> clazz) {
        this.ieExcelStyle = ieExcelStyle;
        this.fileName = fileName;
        this.CLAZZ = clazz;
    }

    public DefaultExcelExportProcessor(String fileName, Class<T> clazz) {
        this(new DefaultIEExcelStyle(), fileName, clazz);
    }


    @Override
    public void init() throws Exception {
        if (!this.isAcceptable(CLAZZ)) {
            throw new NotAcceptableReportTypeException(String.format("[%s] is not acceptable, ExcelExportProcessors only accept reportable implemented [%s]", CLAZZ.getName(), ExcelSheetConfigurer.class.getName()));
        }
        workbook = new XSSFWorkbook();
        reportableEntity = IEContext.getReportableEntity(CLAZZ);

        headerStyle = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        ieExcelStyle.headerStyle(headerStyle);
        ieExcelStyle.headerFont(font);
        headerStyle.setFont(font);


        evenRowStyle = workbook.createCellStyle();
        font = workbook.createFont();
        ieExcelStyle.evenStyle(evenRowStyle);
        ieExcelStyle.evenFont(font);
        evenRowStyle.setFont(font);


        oddRowStyle = workbook.createCellStyle();
        font = workbook.createFont();
        ieExcelStyle.oddStyle(oddRowStyle);
        ieExcelStyle.oddFont(font);
        oddRowStyle.setFont(font);

        oddHyperLinkStyle = workbook.createCellStyle();
        font = workbook.createFont();
        ieExcelStyle.oddStyle(oddHyperLinkStyle);
        ieExcelStyle.oddHypeLinkFont(font);
        oddHyperLinkStyle.setFont(font);

        evenHyperLinkStyle = workbook.createCellStyle();
        font = workbook.createFont();
        ieExcelStyle.evenStyle(evenHyperLinkStyle);
        ieExcelStyle.evenHypeLinkFont(font);
        evenHyperLinkStyle.setFont(font);

        ExcelSheetConfigurer<T> reportableInstance = (ExcelSheetConfigurer<T>) reportableEntity.getEntity().getConstructor().newInstance();

        Iterable<ExcelSheetConfigurer.SheetConfig<T>> iterable = reportableInstance.sheetConfigs();
        for (ExcelSheetConfigurer.SheetConfig<T> conf : iterable) {
            XSSFSheet sheet = workbook.createSheet(conf.getSheetName());
            Set<String> ignored = conf.getIgnoredFields();
            int i = initializeHeaders(sheet, reportableEntity.getMainFields(), ignored, 0, 0);
            sheetAtrributes.add(new SheetAtrribute<T>(i,
                    ignored,
                    sheet,
                    conf.getAvailableCheckFunction()));
        }

    }

    @Override
    public <X extends Reportable<?>> boolean isAcceptable(Class<X> reportable) {
        return ExcelSheetConfigurer.class.isAssignableFrom(reportable);
    }


    private int initializeHeaders(XSSFSheet sheet, Set<ReportableField> reportableFields, Set<String> ignored, int rowNum, int startCell) {

        XSSFRow headerRow = sheet.getRow(rowNum);
        if (headerRow != null) {
            rowNum++;
        } else {
            headerRow = sheet.createRow(rowNum++);
        }

        int offset = 0;
        List<Cell> singleField = new ArrayList<>();
        for (ReportableField rf : reportableFields) {
            if (ignored.contains(rf.getFieldName())) {
                offset += rf.getStartCell() - rf.getEndCell() - 1;
                continue;
            }
            XSSFCell cell = headerRow.createCell(offset + startCell + rf.getStartCell());
            cell.setCellStyle(headerStyle);

            if (rf.getEndCell() - rf.getStartCell() > 0) {
                CellRangeAddress cellRangeAddress = new CellRangeAddress(rowNum - 1, rowNum - 1, offset + startCell + rf.getStartCell(), offset + startCell + rf.getEndCell());
                for (int r = cellRangeAddress.getFirstRow(); r <= cellRangeAddress.getLastRow(); r++) {
                    XSSFRow row = sheet.getRow(r);
                    for (int colNum = cellRangeAddress.getFirstColumn(); colNum <= cellRangeAddress.getLastColumn(); colNum++) {
                        XSSFCell emptyMergedCell = row.getCell(colNum);
                        if (emptyMergedCell == null) {
                            emptyMergedCell = row.createCell(colNum);
                        }
                        emptyMergedCell.setCellStyle(headerStyle);
                    }
                }
                sheet.addMergedRegion(cellRangeAddress);

                initializeHeaders(sheet, IEContext.getReportableEntity(rf.getGenericType() == null ? rf.getType() : (Class<?>) rf.getGenericType()[0]).getMainFields(), Collections.emptySet(), rowNum, offset + startCell + rf.getStartCell());
            } else {
                singleField.add(cell);
            }
            cell.setCellValue(rf.getGroupName());
        }
        int finalHeaderRowCount = sheet.getLastRowNum();
        for (Cell cell : singleField) {
            int i = cell.getColumnIndex();
            if (rowNum <= finalHeaderRowCount) {
                CellRangeAddress cellRangeAddress = new CellRangeAddress(rowNum - 1, finalHeaderRowCount, i, i);
                for (int r = cellRangeAddress.getFirstRow(); r <= cellRangeAddress.getLastRow(); r++) {
                    XSSFRow row = sheet.getRow(r);
                    for (int colNum = cellRangeAddress.getFirstColumn(); colNum <= cellRangeAddress.getLastColumn(); colNum++) {
                        XSSFCell emptyMergefCell = row.getCell(colNum);
                        if (emptyMergefCell == null) {
                            emptyMergefCell = row.createCell(colNum);
                        }
                        emptyMergefCell.setCellStyle(headerStyle);
                    }
                }
                sheet.addMergedRegion(cellRangeAddress);

            }
        }
        return finalHeaderRowCount + 1;
    }

    @Override
    public void processRecord(Iterable<T> accessRules) throws Exception {

        for (T accessRule : accessRules) {

            for (SheetAtrribute<T> sheetAtrribute : sheetAtrributes) {
                if (!sheetAtrribute.availableCheckFunction.apply(accessRule))
                    continue;
                XSSFRow row = sheetAtrribute.sheet.createRow(sheetAtrribute.offset++);
                generateRow(accessRule, sheetAtrribute.sheet, row, sheetAtrribute.offset, 0, sheetAtrribute.offset % 2 == 0, reportableEntity.getMainFields(), sheetAtrribute.ignoredColumn);
                sheetAtrribute.offset = sheetAtrribute.sheet.getLastRowNum() + 1;
            }
        }

    }

    <X extends Exportable<X>> int generateRow(X accessRule, XSSFSheet sheet, XSSFRow row, int rowNum, int cellOffset, boolean isOdd, Set<ReportableField> reportableFields, Set<String> ignoreColumns) throws InvocationTargetException, IllegalAccessException {

        List<SingleFieldWrapper> listFields = new ArrayList<>();
        int rowOffset = rowNum;
        int offset = 0;
        int totalRow = 1;
        for (ReportableField rf : reportableFields) {

            if (ignoreColumns != null && ignoreColumns.contains(rf.getFieldName())) {
                offset += rf.getStartCell() - rf.getEndCell() - 1;
                continue;
            }
            XSSFCell cell = row.createCell(offset + cellOffset + rf.getStartCell());
            cell.setCellStyle(isOdd ? oddRowStyle : evenRowStyle);

            Object fieldValue;
            try {
                fieldValue = accessRule == null ? null : rf.getValue(accessRule);
            } catch (Exception e) {
                throw new IllegalStateException(String.format("failed to get value of '%s', input='%s'  argument='%s'", rf.getFieldName(), accessRule.getClass().getName(), rf.getType().getName()), e);
            }
            if (rf.isIterable() || rf.isSudoIterable()) {
                if (fieldValue != null)
                    listFields.add(new SingleFieldWrapper(rf, offset));
            } else {
                if (fieldValue != null && (double.class.isAssignableFrom(rf.getType()) || Double.class.isAssignableFrom(rf.getType())))
                    cell.setCellValue(((double) fieldValue));
                else if (fieldValue != null && (float.class.isAssignableFrom(rf.getType()) || Float.class.isAssignableFrom(rf.getType())))
                    cell.setCellValue(((float) fieldValue));
                else if (fieldValue != null && (long.class.isAssignableFrom(rf.getType()) || Long.class.isAssignableFrom(rf.getType())))
                    cell.setCellValue(((long) fieldValue));

                else if (fieldValue != null && (boolean.class.isAssignableFrom(rf.getType()) || Boolean.class.isAssignableFrom(rf.getType())))
                    cell.setCellValue(((boolean) fieldValue));

                else if (fieldValue != null && Date.class.isAssignableFrom(rf.getType()))
                    cell.setCellValue(((Date) fieldValue));

                else if (fieldValue != null && (int.class.isAssignableFrom(rf.getType()) || Integer.class.isAssignableFrom(rf.getType())))
                    cell.setCellValue(((int) fieldValue));

                else if (fieldValue != null && (short.class.isAssignableFrom(rf.getType()) || Short.class.isAssignableFrom(rf.getType())))
                    cell.setCellValue(((short) fieldValue));

                else if (Exportable.class.isAssignableFrom(rf.getType()))
                    this.generateRow((Exportable) fieldValue, sheet, row, rowNum, offset + cellOffset + rf.getStartCell(), isOdd, IEContext.getReportableEntity(rf.getType()).getMainFields(), null);
                else if (fieldValue != null && (String.class.isAssignableFrom(rf.getType()) || Enum.class.isAssignableFrom(rf.getType()))) {
                    cell.setCellValue((fieldValue.toString()));

                    if (rf.getHyperLinkHelper() != null) {
                        HyperLinkHelper hyperLinkHelper = rf.getHyperLinkHelper();
                        cell.setCellStyle(isOdd ? oddHyperLinkStyle : evenHyperLinkStyle);
                        CreationHelper creationHelper = workbook.getCreationHelper();
                        Hyperlink hyperlink = null;
                        if (hyperLinkHelper.hyperLinkType() == null) {
                            hyperlink = creationHelper.createHyperlink(HyperlinkType.NONE);
                        } else
                            switch (hyperLinkHelper.hyperLinkType()) {
                                case NONE:
                                    hyperlink=creationHelper.createHyperlink(HyperlinkType.NONE);
                                    break;
                                case FILE:
                                    hyperlink=creationHelper.createHyperlink(HyperlinkType.FILE);
                                    break;
                                case EMAIL:
                                    hyperlink=creationHelper.createHyperlink(HyperlinkType.EMAIL);
                                    break;
                                case URL:
                                    hyperlink=creationHelper.createHyperlink(HyperlinkType.URL);
                                    break;
                                case DOCUMENT:
                                    hyperlink=creationHelper.createHyperlink(HyperlinkType.DOCUMENT);
                                    break;
                            }
                        hyperlink.setAddress(hyperLinkHelper.returnHyperLink(fieldValue.toString()));
                        cell.setHyperlink(hyperlink);
                    }

                } else if (fieldValue != null) {
                    throw new IllegalStateException(String.format("UnsupportedType '%s'", rf.getType().getName()));
                }

            }
        }


        for (SingleFieldWrapper rf : listFields) {
            XSSFRow row1 = row;
            if (rf.reportableField.isIterable()) {
                Iterable<?> value = (Iterable<T>) rf.reportableField.getValue(accessRule);
                Iterator<?> iterator = value.iterator();

                while (iterator.hasNext()) {
                    Object next = iterator.next();
                    if (next != null && Exportable.class.isAssignableFrom(next.getClass())) {
                        totalRow = this.generateRow((Exportable) next, sheet, row1, rowNum, rf.offset + cellOffset + rf.reportableField.getStartCell(), isOdd, IEContext.getReportableEntity(next.getClass()).getMainFields(), null);
                    } else {
                        totalRow = 0;
                        XSSFCell cell = row1.createCell(rf.offset + cellOffset + rf.reportableField.getStartCell());
                        cell.setCellStyle(isOdd ? oddRowStyle : evenRowStyle);
                        if (next != null && (double.class.isAssignableFrom(next.getClass()) || Double.class.isAssignableFrom(next.getClass())))
                            cell.setCellValue(((double) next));

                        else if (next != null && (float.class.isAssignableFrom(next.getClass()) || Float.class.isAssignableFrom(next.getClass())))
                            cell.setCellValue(((float) next));

                        else if (next != null && (long.class.isAssignableFrom(next.getClass()) || Long.class.isAssignableFrom(next.getClass())))
                            cell.setCellValue(((long) next));

                        else if (next != null && (boolean.class.isAssignableFrom(next.getClass()) || Boolean.class.isAssignableFrom(next.getClass())))
                            cell.setCellValue(((boolean) next));

                        else if (next != null && Date.class.isAssignableFrom(next.getClass()))
                            cell.setCellValue(((Date) next));

                        else if (next != null && (int.class.isAssignableFrom(next.getClass()) || Integer.class.isAssignableFrom(next.getClass())))
                            cell.setCellValue(((int) next));

                        else if (next != null && (short.class.isAssignableFrom(next.getClass()) || Short.class.isAssignableFrom(next.getClass())))
                            cell.setCellValue(((short) next));

                        else if (next != null && (String.class.isAssignableFrom(next.getClass()) || Enum.class.isAssignableFrom(next.getClass()))){
                            cell.setCellValue((next.toString()));

                            if (rf.reportableField.getHyperLinkHelper() != null) {
                                HyperLinkHelper hyperLinkHelper = rf.reportableField.getHyperLinkHelper();
                                cell.setCellStyle(isOdd ? oddHyperLinkStyle : evenHyperLinkStyle);
                                CreationHelper creationHelper = workbook.getCreationHelper();
                                Hyperlink hyperlink = null;
                                if (hyperLinkHelper.hyperLinkType() == null) {
                                    hyperlink = creationHelper.createHyperlink(HyperlinkType.NONE);
                                } else
                                    switch (hyperLinkHelper.hyperLinkType()) {
                                        case NONE:
                                            hyperlink=creationHelper.createHyperlink(HyperlinkType.NONE);
                                            break;
                                        case FILE:
                                            hyperlink=creationHelper.createHyperlink(HyperlinkType.FILE);
                                            break;
                                        case EMAIL:
                                            hyperlink=creationHelper.createHyperlink(HyperlinkType.EMAIL);
                                            break;
                                        case URL:
                                            hyperlink=creationHelper.createHyperlink(HyperlinkType.URL);
                                            break;
                                        case DOCUMENT:
                                            hyperlink=creationHelper.createHyperlink(HyperlinkType.DOCUMENT);
                                            break;
                                    }
                                hyperlink.setAddress(hyperLinkHelper.returnHyperLink(next.toString()));
                                cell.setHyperlink(hyperlink);
                            }
                        }

                        else if (next != null) {
                            throw new IllegalStateException(String.format("UnsupportedType '%s'", rf.reportableField.getType().getName()));
                        }
                    }

//                    for (int i = rowNum; i < rowNum + totalRow; i++)
                    if (iterator.hasNext()) {
                        rowNum += totalRow;
                        row1 = sheet.createRow(rowNum++);
                        row1.copyRowFrom(row, new CellCopyPolicy());
                    }
                    totalRow = 1;


                }
            } else {
                //TODO Not sure need to be test
                int i = this.generateRow((Exportable) rf.reportableField.getValue(accessRule), sheet, row, rowNum, rf.offset + cellOffset + rf.reportableField.getStartCell(), isOdd, IEContext.getReportableEntity(rf.reportableField.getValue(accessRule).getClass()).getMainFields(), null);
                rowNum += i;

            }

        }
        return rowNum - rowOffset;


    }

    private void iterableFields() {

    }

    @Override
    public String finalizeProcess() throws IOException {
        for(int i= 0; i<sheetAtrributes.size();i++){
            XSSFSheet sheetAt = workbook.getSheetAt(0);
            for(int j=0; j<sheetAt.getRow(sheetAtrributes.get(i).lastHeaderRow).getLastCellNum(); j++){
                sheetAt.autoSizeColumn(j);
            }
        }

        File storedFile = new File(new File(IEContext.getExportPath()), fileName + ".xlsx");
        OutputStream os = new BufferedOutputStream(new FileOutputStream(storedFile));
        workbook.write(os);
        workbook.close();
        os.flush();
        os.close();

        return storedFile.getPath();
    }

    class SingleFieldWrapper {
        ReportableField reportableField;
        int offset;

        public SingleFieldWrapper(ReportableField reportableField, int offset) {
            this.reportableField = reportableField;
            this.offset = offset;
        }
    }


}
