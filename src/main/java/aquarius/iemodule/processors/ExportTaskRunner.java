package aquarius.iemodule.processors;

import aquarius.iemodule.structure.Exportable;

public abstract class ExportTaskRunner<T extends Exportable> {
    private ExportProcessor<T > processor;

    public ExportTaskRunner() {
    }

    public void setProcessor(ExportProcessor<T> processor) {
        this.processor = processor;
    }

    public void doExport(Iterable<T> records) throws Exception {
        this.processor.processRecord(records);
    }

    public abstract void run() throws Exception;
}
