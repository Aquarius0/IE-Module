package aquarius.iemodule.structure;

public interface IEMapper<REP, T> {

    REP toReportable(T input);

    T toActualObject(REP input);
}
