package aquarius.iemodule.impl;

import aquarius.iemodule.exception.ServiceNotSupportedException;
import aquarius.iemodule.utils.StringUtils;
import com.google.common.primitives.Primitives;
import aquarius.iemodule.DefaultIEExcelStyle;
import aquarius.iemodule.IEContext;
import aquarius.iemodule.IEExcelStyle;
import aquarius.iemodule.config.ApplicationContextProvider;
import aquarius.iemodule.exception.NotAcceptableReportTypeException;
import aquarius.iemodule.impl.util.ExcelSheetConfigurer;
import aquarius.iemodule.impl.util.ExcelTemplateConf;
import aquarius.iemodule.impl.util.TemplateConfig;
import aquarius.iemodule.processors.ImportProcessor;
import aquarius.iemodule.processors.ImportTaskRunner;
import aquarius.iemodule.structure.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class DefaultExcelImportProcessor<T extends Importable<T> & ExcelSheetConfigurer<T>> extends DefaultExcelProcessor implements ImportProcessor<T> {
    private static final Logger LOGGER = LogManager.getLogger();

    private CellStyle headerStyle;

    private IEExcelStyle ieExcelStyle;
    private XSSFWorkbook inoutWorkbook;
    private XSSFWorkbook reportWorkbook;
    private Class<T> clazz;
    private String user;

    private Map<Integer, LoadedEntityHolder<T>> loadedEntity = new HashMap<>();
    private ReportableEntity reportableEntity;
    private List<SheetAtrribute<T>> sheetAtrributes = new ArrayList<>();
    private Validator validator;
    private boolean hasError = false;

    private DefaultExcelImportProcessor() {
        ieExcelStyle = new DefaultIEExcelStyle();
    }

    @Override
    public <X extends Reportable<?>> boolean isAcceptable(Class<X> reportable) {
        return ExcelSheetConfigurer.class.isAssignableFrom(reportable);
    }

    public DefaultExcelImportProcessor(InputStream excelfile, Class<T> entityDtoClass, String user) throws IOException {
        this(excelfile, entityDtoClass, user, new DefaultIEExcelStyle());
    }

    public DefaultExcelImportProcessor(InputStream excelfile, Class<T> entityDtoClass, String user, IEExcelStyle ieExcelStyle) throws IOException {
        this.inoutWorkbook = new XSSFWorkbook(excelfile);
        this.reportWorkbook = new XSSFWorkbook();
        this.clazz = entityDtoClass;
        this.user = user;
        this.ieExcelStyle = ieExcelStyle;
        this.validator = ApplicationContextProvider.getContext().getBean(Validator.class);
    }

    @Override
    public void init() throws Exception {
        if(!this.isAcceptable(clazz)){
            throw new NotAcceptableReportTypeException(String.format("[%s] is not acceptable, ExcelImportProcessors only accept reportable implemented [%s]",clazz.getName(),ExcelSheetConfigurer.class.getName() ));
        }
        reportableEntity = IEContext.getReportableEntity(clazz);
        Object config = IEContext.getConfig(reportableEntity, this);
        ExcelTemplateConf con;
        if (config instanceof ExcelTemplateConf)
            con = (ExcelTemplateConf) config;
        else {
            throw new IllegalStateException("invalid config");
        }


        ExcelSheetConfigurer<T> reportableInstance = (ExcelSheetConfigurer<T>) reportableEntity.getEntity().getConstructor().newInstance();
        Iterable<ExcelSheetConfigurer.SheetConfig<T>> iterable = reportableInstance.sheetConfigs();

        int c = 0;
        for (ExcelSheetConfigurer.SheetConfig<T> conf : iterable) {
            XSSFSheet sheet = inoutWorkbook.getSheetAt(c++);
            Set<String> ignored = conf.getIgnoredFields();
            sheetAtrributes.add(new SheetAtrribute<T>(con.getSheetProperties().get(c - 1).getHeaderRows(),
                    ignored,
                    sheet,
                    conf.getAvailableCheckFunction()));
        }
    }

    @Override
    public TemplateConfig generateTemplate(Class<? extends Importable> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        reportableEntity = IEContext.getReportableEntity(clazz);

        headerStyle = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        ieExcelStyle.headerStyle(headerStyle);
        ieExcelStyle.headerFont(font);
        headerStyle.setFont(font);


        ExcelSheetConfigurer<T> reportableInstance = (ExcelSheetConfigurer<T>) reportableEntity.getEntity().getConstructor().newInstance();

        Iterable<ExcelSheetConfigurer.SheetConfig<T>> iterable = reportableInstance.sheetConfigs();
        String templatePath = IEContext.getTemplatePath();

        String templateName = templatePath + "/Excel_" + clazz.getSimpleName() + "_Template.xlsx";
        int c = 0;

        ExcelTemplateConf templateConfig = new ExcelTemplateConf(templateName);
        List<ExcelTemplateConf.SheetProperties> sheetProperties = new ArrayList<>();
        templateConfig.setSheetProperties(sheetProperties);

        if (iterable != null){
            for (ExcelSheetConfigurer.SheetConfig<T> conf : iterable) {
                XSSFSheet sheet = workbook.createSheet(conf.getSheetName());
                Set<String> ignored = conf.getIgnoredFields();
                int i = initializeHeaders(sheet, reportableEntity.getMainFields(), ignored, 0, 0);
                ExcelTemplateConf.SheetProperties prop = new ExcelTemplateConf.SheetProperties(conf.getSheetName(), i, c++);
                sheetProperties.add(prop);
            }

            File storedFile = new File(templateName);
            OutputStream os = new BufferedOutputStream(Files.newOutputStream(storedFile.toPath()));
            LOGGER.debug("Writing template to {}", templateName);
            workbook.write(os);
            workbook.close();
            os.flush();
            os.close();
        }

        return templateConfig;
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

                initializeHeaders(sheet, IEContext.getReportableEntity(rf.getGenericType()==null?rf.getType():(Class<?>) rf.getGenericType()[0]).getMainFields(), Collections.emptySet(), rowNum, offset + startCell + rf.getStartCell());
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
    public Collection<T> processRecord(ImportTaskRunner<T> importTaskRunner) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        reportableEntity = IEContext.getReportableEntity(clazz);
        ExcelTemplateConf config = (ExcelTemplateConf) IEContext.getConfig(reportableEntity, this);
        ExcelSheetConfigurer<T> reportableInstance = (ExcelSheetConfigurer<T>) reportableEntity.getEntity().getConstructor().newInstance();
        Iterable<ExcelSheetConfigurer.SheetConfig<T>> iterable = reportableInstance.sheetConfigs();

        int sheetNum = 0;
        int totalSheet = config.getSheetProperties().size();
        int[] sheetLastRow = new int[totalSheet];
        int c = 0;
        for (ExcelSheetConfigurer.SheetConfig<T> conf : iterable) {
            XSSFSheet sheet = inoutWorkbook.getSheetAt(c++);
            XSSFSheet reportSheet = reportWorkbook.createSheet(sheet.getSheetName());
            sheetLastRow[sheetNum] = initializeHeaders(reportSheet, reportableEntity.getMainFields(), conf.getIgnoredFields(), 0, 0);
            for (Row row : sheet) {

                if (row.getRowNum() < config.getSheetProperties().get(c - 1).getHeaderRows()) continue;
                T object = createObject(clazz, null, reportableEntity.getMainFields(),conf.getIgnoredFields(), 0, row);

                boolean hasObject = loadedEntity.containsKey(object.reportHash());
                if (hasObject) {
                    LoadedEntityHolder<T> loadedObject = loadedEntity.get(object.reportHash());
                    loadedObject.addRow(sheetNum, row);
                    loadedObject.loadedEntity.merge(object);
                } else {
                    LoadedEntityHolder<T> tLoadedEntityHolder = new LoadedEntityHolder<>(object, totalSheet);
                    tLoadedEntityHolder.addRow(sheetNum, row);
                    loadedEntity.put(object.reportHash(), tLoadedEntityHolder);

                }
            }
            sheetNum++;
        }

        List<T> loadedEntity = new ArrayList<>();
        for (LoadedEntityHolder<T> holder : this.loadedEntity.values()) {
            importTaskRunner.fillDefaults(holder.loadedEntity);
            if (validator != null && importTaskRunner.shouldValidate()) {
                Set<ConstraintViolation<T>> validate = validator.validate(holder.getLoadedEntity());
                if (validate != null && !validate.isEmpty() || !importTaskRunner.validate(holder.getLoadedEntity())) {
                    hasError = true;
                    int sheetNum0 = 0;
                    for (List<Row> sheetRows : holder.getRows()) {
                        XSSFSheet sheetAt = reportWorkbook.getSheetAt(sheetNum0);

                        for (Row row : sheetRows) {
                            XSSFRow reportRow = sheetAt.createRow(sheetLastRow[sheetNum0]++);
                            int cellCount = 0;
                            for (Cell cellReport : row) {
                                XSSFCell cell = reportRow.createCell(cellCount++);
                                switch (cellReport.getCellTypeEnum()) {
                                    case NUMERIC:
                                        cell.setCellValue(cellReport.getNumericCellValue());
                                        break;
                                    case BOOLEAN:
                                        cell.setCellValue(cellReport.getBooleanCellValue());
                                        break;
                                    default:
                                        cell.setCellValue(cellReport.getStringCellValue());
                                        break;
                                }
                            }
                        }
                        sheetNum0++;
                    }
                    continue;
                }
            }else{
                if (!importTaskRunner.validate(holder.getLoadedEntity())) {
                    hasError = true;
                    int sheetNum0 = 0;
                    for (List<Row> sheetRows : holder.getRows()) {
                        XSSFSheet sheetAt = reportWorkbook.getSheetAt(sheetNum0);

                        for (Row row : sheetRows) {
                            XSSFRow reportRow = sheetAt.createRow(sheetLastRow[sheetNum0]++);
                            int cellCount = 0;
                            for (Cell cellReport : row) {
                                XSSFCell cell = reportRow.createCell(cellCount++);
                                switch (cellReport.getCellTypeEnum()) {
                                    case NUMERIC:
                                        cell.setCellValue(cellReport.getNumericCellValue());
                                        break;
                                    case BOOLEAN:
                                        cell.setCellValue(cellReport.getBooleanCellValue());
                                        break;
                                    default:
                                        cell.setCellValue(cellReport.getStringCellValue());
                                        break;
                                }
                            }
                        }
                        sheetNum0++;
                    }
                    continue;
                }
            }
            loadedEntity.add(holder.loadedEntity);
        }
        return loadedEntity;
    }


    private Object cell2Obj(Cell c, Class<?> type) {
        Object objectVal = null;
        String stringCellValue = null;
        if (c==null)return null;
        switch (c.getCellTypeEnum()) {
            case STRING:
                stringCellValue = c.getStringCellValue();
                break;
            case NUMERIC:
                stringCellValue = new DecimalFormat("#.###########").format(Double.valueOf(c.getNumericCellValue()));
                break;
            case BOOLEAN:
                stringCellValue = c.getBooleanCellValue() + "";
                break;
            case BLANK:
            case _NONE:
                stringCellValue = null;
                break;
            case ERROR:
            case FORMULA:
                break;
            default:
                stringCellValue = c.getDateCellValue().toString();
        }
        if(Primitives.isWrapperType(type) && !StringUtils.hasContent(stringCellValue)){
            objectVal=null;

        }else {
            if (double.class.isAssignableFrom(type) || Double.class.isAssignableFrom(type)) {
                objectVal = Double.parseDouble(StringUtils.hasContent(stringCellValue) ? stringCellValue : "0");
            } else if (float.class.isAssignableFrom(type) || Float.class.isAssignableFrom(type)) {

                objectVal = Float.parseFloat(StringUtils.hasContent(stringCellValue) ? stringCellValue : "0");
            } else if (long.class.isAssignableFrom(type) || Long.class.isAssignableFrom(type)) {
                objectVal = Long.parseLong(StringUtils.hasContent(stringCellValue) ? stringCellValue : "0");
            } else if (boolean.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type)) {
                objectVal = Boolean.parseBoolean(StringUtils.hasContent(stringCellValue) ? stringCellValue : "0");
            } else if (int.class.isAssignableFrom(type) || Integer.class.isAssignableFrom(type)) {
                objectVal = Integer.parseInt(StringUtils.hasContent(stringCellValue) ? stringCellValue : "0");
            } else if (short.class.isAssignableFrom(type) || Short.class.isAssignableFrom(type)) {
                objectVal = Short.parseShort(StringUtils.hasContent(stringCellValue) ? stringCellValue : "0");
            } else if (Date.class.isAssignableFrom(type)) {
                Date dateCellValue = c.getDateCellValue();
                objectVal = dateCellValue;
            } else if (String.class.isAssignableFrom(type)) {
                objectVal = stringCellValue;
            } else if (type.isEnum()) {
                Object[] enumConstants = type.getEnumConstants();
                for (Object enumConstant : enumConstants) {
                    if(enumConstant.toString().equals(stringCellValue)){
                        objectVal=enumConstant;
                        break;
                    }
                }

            }
        }

        return objectVal;
    }

    private <X> X createObject(Class<X> x, X object, Set<ReportableField> fieds,Set<String> ignored, int offset, Row row) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<ReportableField> iterableFields = new LinkedList<>();
        if (object == null)
            object = x.getConstructor().newInstance();

        for (ReportableField f : fieds) {
            Object objectVal = null;
            Class<?> type = f.getType();
            if(ignored!=null && ignored.contains(f.getFieldName())){
                offset += f.getStartCell() - f.getEndCell() - 1;
                continue;
            }
            if (Importable.class.isAssignableFrom(type)) {
                objectVal = createObject(type, null, IEContext.getReportableEntity(type).getMainFields(),null, offset + f.getStartCell(), row);
            } else if (Iterable.class.isAssignableFrom(type)) {
                if (Set.class.isAssignableFrom(type)) {
                    HashSet<Object> objects = new HashSet<>();
                    objectVal = objects;
                    if (Importable.class.isAssignableFrom((Class<?>) f.getGenericType()[0])) {
                        objects.add(createObject((Class<?>) f.getGenericType()[0], null, IEContext.getReportableEntity((Class<?>) (Class<?>) f.getGenericType()[0]).getMainFields(), null,offset + f.getStartCell(), row));
                    } else if (Iterable.class.isAssignableFrom((Class<?>) f.getGenericType()[0])) {
                        throw new ServiceNotSupportedException();
                    } else {
                        Cell cell = row.getCell(offset + f.getStartCell());
                        Object o = cell2Obj(cell, (Class<?>) f.getGenericType()[0]);
                        if(o==null)continue;
                        objects.add(o);
                    }
                } else if (List.class.isAssignableFrom(f.getType())) {
                    List<Object> objects = new ArrayList<>();
                    objectVal = objects;
                    if (Importable.class.isAssignableFrom((Class<?>) f.getGenericType()[0])) {
                        objects.add(createObject((Class<?>) f.getGenericType()[0], null, IEContext.getReportableEntity((Class<?>) f.getGenericType()[0]).getMainFields(), null,offset + f.getStartCell(), row));
                    } else if (Iterable.class.isAssignableFrom((Class<?>) f.getGenericType()[0])) {
                        throw new ServiceNotSupportedException();
                    } else {
                        Cell cell = row.getCell(offset + f.getStartCell());
                        Object o = cell2Obj(cell, (Class<?>) f.getGenericType()[0]);
                        if(o==null)continue;
                        objects.add(o);
                    }
                }
            } else {
                Cell cell = row.getCell(offset + f.getStartCell());
                objectVal = cell2Obj(cell, f.getType());
                if( objectVal==null )continue;

            }
            f.setValue(object, objectVal);

        }
        return object;
    }

    @Override
    public String finalizeProcess() throws IOException {

        inoutWorkbook.close();
        if (hasError) {
            Path path = new File(IEContext.getReportPath() + "/" + reportableEntity.getEntity().getSimpleName() + "-" + user + "-" + new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss").format(new Date()) + ".xlsx").toPath();
            BufferedOutputStream writer = new BufferedOutputStream(Files.newOutputStream(path));
            reportWorkbook.write(writer);
            writer.flush();
            writer.close();
            reportWorkbook.close();

            return path.toString();
        }
        return null;
    }


    private class LoadedEntityHolder<T> {
        private final T loadedEntity;
        private final List<List<Row>> rows;

        public LoadedEntityHolder(T loadedEntity, int sheetCount) {
            this.loadedEntity = loadedEntity;
            rows = new ArrayList<>();
            for (int i = 0; i < sheetCount; i++)
                rows.add(new ArrayList<>());
        }

        public T getLoadedEntity() {
            return loadedEntity;
        }

        public void addRow(int sheetNum, Row row) {
            rows.get(sheetNum).add(row);
        }

        public List<List<Row>> getRows() {
            return rows;
        }
    }

}
