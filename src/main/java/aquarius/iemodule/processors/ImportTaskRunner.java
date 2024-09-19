package aquarius.iemodule.processors;

import aquarius.iemodule.structure.Importable;

import java.util.Collection;

public abstract class ImportTaskRunner<T extends Importable> {

    public abstract void storeRecord(Collection<T> records);

    public  boolean validate(T record){
        return true;
    }

    public boolean shouldValidate(){
        return true;
    }
    public  void fillDefaults(T record){

    }
}
