package aquarius.iemodule;



import aquarius.iemodule.filemanager.IEFile;
import aquarius.iemodule.processors.ExportProcessor;
import aquarius.iemodule.processors.ExportTaskRunner;
import aquarius.iemodule.processors.ImportProcessor;
import aquarius.iemodule.processors.ImportTaskRunner;
import aquarius.iemodule.structure.Exportable;
import aquarius.iemodule.structure.Importable;

import java.util.Collection;

public class IEUtils {

    public  static <T extends Importable<?>> IEFile importData(ImportTaskRunner<T> taskRunner, ImportProcessor<T> importProcessor) throws Exception {
        importProcessor.init();
        Collection<T> ts = importProcessor.processRecord(taskRunner);
        IEFile finalise = importProcessor.finalizeProcess();
        taskRunner.storeRecord(ts);
        return finalise;
    }


    public  static <T extends Exportable<?>> IEFile exportData(ExportTaskRunner<T> taskRunner, ExportProcessor<T> exportProcessor) throws Exception {
        exportProcessor.init();
        taskRunner.setProcessor(exportProcessor);
        taskRunner.run();
        return exportProcessor.finalizeProcess();
    }


}
