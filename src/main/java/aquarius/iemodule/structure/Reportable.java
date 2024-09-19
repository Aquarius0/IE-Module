package aquarius.iemodule.structure;

public interface Reportable<T extends Reportable<T>> {
    default String reportName(){
        return this.getClass().getSimpleName();
    }


}
