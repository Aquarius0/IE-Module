package aquarius.iemodule.impl.util;

import aquarius.iemodule.structure.Reportable;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

public interface ExcelSheetConfigurer<T extends Reportable<T>> {

    Iterable<SheetConfig<T>> sheetConfigs();

    class SheetConfig<X> {
        private final Set<String> ignoredFields;
        private final String sheetName;
        private final Function<X, Boolean> availableCheckFunction;

        public SheetConfig(String sheetName) {
            this(Collections.emptySet(),sheetName);
        }

        public SheetConfig(Set<String> ignoredFields, String sheetName) {
            this(ignoredFields,sheetName,e->true);
        }

        public SheetConfig(Set<String> ignoredFields, String sheetName, Function<X, Boolean> availableCheckFunction) {
            this.ignoredFields = ignoredFields;
            this.sheetName = sheetName;
            this.availableCheckFunction = availableCheckFunction;
        }

        public Set<String> getIgnoredFields() {
            return ignoredFields;
        }

        public String getSheetName() {
            return sheetName;
        }

        public Function<X, Boolean> getAvailableCheckFunction() {
            return availableCheckFunction;
        }
    }
}
