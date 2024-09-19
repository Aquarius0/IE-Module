package aquarius.iemodule.impl.util;

import java.util.List;

public class ExcelTemplateConf extends TemplateConfig{
    private List<SheetProperties> sheetProperties;

    public ExcelTemplateConf(String templateName) {
        super(templateName);
    }

    public List<SheetProperties> getSheetProperties() {
        return sheetProperties;
    }

    public void setSheetProperties(List<SheetProperties> sheetProperties) {
        this.sheetProperties = sheetProperties;
    }

    public static class SheetProperties{
        private final String name;
        private final int headerRows;
        private final int index;

        public SheetProperties(String name, int headerRows, int index) {
            this.name = name;
            this.headerRows = headerRows;
            this.index = index;
        }

        public String getName() {
            return name;
        }

        public int getHeaderRows() {
            return headerRows;
        }

        public int getIndex() {
            return index;
        }
    }
}
