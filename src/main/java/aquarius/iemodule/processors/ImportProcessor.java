package aquarius.iemodule.processors;



import aquarius.iemodule.impl.util.TemplateConfig;
import aquarius.iemodule.structure.Importable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

public interface ImportProcessor<T extends Importable>  extends Processor {
    TemplateConfig generateTemplate(Class<? extends Importable> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException;

    Collection<T> processRecord(ImportTaskRunner<T> importTaskRunner) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException;



}