package aquarius.iemodule.structure;

public interface Importable<T extends Importable<T>> extends Reportable<T>{
    default boolean reportEquals(T reportable){
        return this.equals(reportable);
    }

    default int reportHash(){
        return this.hashCode();
    }

    default void merge(T t){

    }
}
