package aquarius.iemodule.impl;

import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.Set;
import java.util.function.Function;

public class SheetAtrribute<T> {
    final int lastHeaderRow;
        int offset;
        final Set<String> ignoredColumn;
        final XSSFSheet sheet;
        final Function<T, Boolean> availableCheckFunction;

        public SheetAtrribute(int offset, Set<String> ignoredColumn, XSSFSheet sheet, Function<T, Boolean> availableCheckFunction) {
            this.offset = offset;
            this.lastHeaderRow=offset-1;
            this.ignoredColumn = ignoredColumn;
            this.sheet = sheet;
            this.availableCheckFunction = availableCheckFunction;
        }
    }