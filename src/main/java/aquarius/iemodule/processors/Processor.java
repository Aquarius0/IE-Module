package aquarius.iemodule.processors;


import aquarius.iemodule.filemanager.IEFile;
import aquarius.iemodule.structure.Reportable;

import java.io.IOException;

public interface Processor {
    void init() throws Exception;

    IEFile finalizeProcess() throws IOException;

    String ProcessorName();

    default <X extends Reportable<?>> boolean isAcceptable(Class<X> reportable){
        return true;
    }

}
