package aquarius.iemodule.impl;

import aquarius.iemodule.processors.Processor;

public abstract class DefaultExcelProcessor  implements Processor {
    @Override
    public String ProcessorName() {
        return "Default-Excel-Processor";
    }
}
