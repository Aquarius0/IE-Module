package aquarius.iemodule.impl;

import jakarta.persistence.EntityManagerFactory;
import aquarius.iemodule.DefaultIEExcelStyle;
import aquarius.iemodule.IEExcelStyle;
import aquarius.iemodule.impl.util.ExcelSheetConfigurer;
import aquarius.iemodule.structure.Exportable;
import aquarius.iemodule.structure.ReportableField;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.hibernate.Session;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

public class DefaultHibernateSessionSupportExcelExportProcessor<T extends Exportable<T> & ExcelSheetConfigurer<T>> extends DefaultExcelExportProcessor<T> {

    private final EntityManagerFactory entityManagerFactory;
    private Session session;

    public DefaultHibernateSessionSupportExcelExportProcessor(EntityManagerFactory entityManagerFactory, IEExcelStyle ieExcelStyle, String fileName, Class<T> clazz) {
        super(ieExcelStyle, fileName, clazz);
        this.entityManagerFactory = entityManagerFactory;
    }

    public DefaultHibernateSessionSupportExcelExportProcessor(EntityManagerFactory entityManagerFactory, String fileName, Class<T> clazz) {
        this(entityManagerFactory, new DefaultIEExcelStyle(), fileName, clazz);
    }

    @Override
    public void processRecord(Iterable<T> accessRules) throws Exception {
        try {
            this.session = (Session) entityManagerFactory.createEntityManager().getDelegate();
            super.processRecord(accessRules);
        } finally {
            this.session.close();
            this.entityManagerFactory.close();
        }

    }

    @Override
    <X extends Exportable<X>> int generateRow(X accessRule, XSSFSheet sheet, XSSFRow row, int rowNum, int cellOffset, boolean isOdd, Set<ReportableField> reportableFields, Set<String> ignoreColumns) throws InvocationTargetException, IllegalAccessException {
        try{
            if(!session.isConnected()){
                session.update(accessRule);
            }

        }catch (Exception ignore){}


        return super.generateRow(accessRule, sheet, row, rowNum, cellOffset, isOdd, reportableFields, ignoreColumns);
    }
}
