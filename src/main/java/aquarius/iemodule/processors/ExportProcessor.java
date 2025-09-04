package aquarius.iemodule.processors;


import aquarius.iemodule.structure.Reportable;

public interface ExportProcessor<T extends Reportable>  extends Processor{

    void processRecord(Iterable<T> records) throws Exception;

}