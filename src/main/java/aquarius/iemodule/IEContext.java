package aquarius.iemodule;

import aquarius.iemodule.config.IEProperties;
import aquarius.iemodule.impl.util.TemplateConfig;
import aquarius.iemodule.processors.ImportProcessor;
import aquarius.iemodule.processors.Processor;
import aquarius.iemodule.structure.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IEContext {

    private static  Map<Class<?>, ReportableEntity> reportableEntityMap;

    private static String reportPath;

    private static String templatePath;
    private static String exportPath;


    public static void prop(IEProperties properties){
        reportPath =properties.getIeReportPath();
        templatePath =properties.getIeTemplatePath();
        exportPath= properties.getIeExportPath();;
    }

    public static  String getTemplate(Class<?extends Importable> entityClass, Class<?extends ImportProcessor> importProcessor){
        ReportableEntity reportableEntity = getReportableEntity(entityClass);

        return reportableEntity.getTemplate(importProcessor,entityClass);
    }
    public static String getReportPath() {
        return reportPath;
    }

    public static String getTemplatePath() {
        return templatePath;
    }

    public static String getExportPath() {
        return exportPath;
    }

    public static ReportableEntity getReportableEntity(Class<?> entityClass){
        ReportableEntity reportableEntity = reportableEntityMap.get(entityClass);
        if(reportableEntityMap==null)
            throw new IllegalStateException("IEContext not initialized");

        if(reportableEntity==null)
            throw new IllegalStateException(String.format("entity '%s' is not a reportable entity",entityClass.getName()));

        return reportableEntity;
    }

    public static Object getConfig(ReportableEntity reportableEntity, Processor processor){
        return reportableEntity.getTemplateConfig(processor.ProcessorName());
    }

    public static  IEContextBuilder getBuilder(){
        return new IEContextBuilder();
    }
    public static class IEContextBuilder{
        private IEContextBuilder() {
            reportableEntityMap=new HashMap<>();
        }

        public  IEContextBuilder add(Class<? extends Reportable> reportable, ReportEntityBuilder<? extends Reportable> reportEntityBuilder){
            ReportableEntity build = reportEntityBuilder.build();
            if(!build.getEntity().equals(reportable))
                throw new IllegalStateException("Entoty class and reportable class must be same");
            reportableEntityMap.put(reportable,build);
            return this;
        }

    }

    public static class ReportEntityBuilder<T extends  Reportable<?>>{

        private ReportableEntity reportableEntity;
        private Class<T> reportable;
        public ReportEntityBuilder(Class<T> reportable) {
            this.reportable=reportable;
            this.reportableEntity = new ReportableEntity();
            reportableEntity.setEntity(reportable);
        }
        public ReportEntityBuilder<T> setName(String name){
            reportableEntity.setName(name);
            return this;
        }
        public ReportEntityBuilder<T> setExportFileNamePrefix(String prefix){
            reportableEntity.setExportFileNamePrefix(prefix);
            return this;
        }

        public ReportEntityBuilder<T> procesorConfig(String processorName, TemplateConfig config){
            reportableEntity.addTemplateConfig(processorName,config);
            return this;

        }
        public ReportEntityBuilder<T> addReportableField(ReportableFieldBuilder builder) throws NoSuchFieldException, NoSuchMethodException {
            ReportableField build = builder.build(reportable);
            reportableEntity.add(build);
            return this;
        }


        public ReportableEntity build(){
            return reportableEntity;
        }

        private  void addReportableField(ReportableField field){
            reportableEntity.add(field);
        }

    }

    public static class ReportableFieldBuilder{
        private ReportableField reportableField;


        private String fieldName;


        public ReportableFieldBuilder() {
            this.reportableField = new ReportableField();
        }

        public ReportableFieldBuilder genericType(Type[] type){
            reportableField.setGenericType(type);
            return this;
        }
        public ReportableFieldBuilder isIterable(boolean iterable){
            reportableField.setIterable(iterable);
            return this;
        }

        public ReportableFieldBuilder sudoIterable(boolean sudoIterable){
            reportableField.setSudoIterable(sudoIterable);
            return this;
        }
        public ReportableFieldBuilder type(Class<?> clazz){
            reportableField.setType(clazz);
            return this;
        }
        public ReportableFieldBuilder groupName(String groupName){
            reportableField.setGroupName(groupName);
            return this;
        }
        public ReportableFieldBuilder order(int order){
            reportableField.setOrder(order);
            return this;
        }
        public ReportableFieldBuilder fieldName(String fieldName){
            this.fieldName=fieldName;
            return this;
        }

        public ReportableFieldBuilder setStartCell(int startCell){
            reportableField.setStartCell(startCell);
            return this;
        }
        public ReportableFieldBuilder setEndCell(int endCell){
            reportableField.setEndCell(endCell);
            return this;
        }
        public ReportableFieldBuilder mapper(IEMapper<Object, Object> mapper){
            reportableField.setMapper(mapper);
            return this;
        }
        public ReportableFieldBuilder hyperLinkHelper(HyperLinkHelper<Object> helper){
            reportableField.setHyperLinkHelper(helper);
            return this;
        }

        public ReportableField build(Class<?extends Reportable> reportable) throws NoSuchFieldException, NoSuchMethodException {
            Map <String, Field> fields=new HashMap<>();
            Class<?> c = reportable;
            do {
                fields.putAll(Stream.of(c.getDeclaredFields()).collect(Collectors.toMap(Field::getName, v->v)));
            } while (!(c = c.getSuperclass()).equals(Object.class));

            reportableField.setFieldName(fieldName);
            Field declaredField = fields.get(fieldName);
            Class<?> declaringClass = declaredField.getType();
            Method getter;
            Method setter;
            if(declaringClass.equals(boolean.class) || declaringClass.equals(Boolean.class)){
                 getter = reportable.getMethod("is" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1));
            }else{
                getter = reportable.getMethod("get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1));
            }
            setter = reportable.getMethod("set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1),declaringClass);

            reportableField.setSetter(setter);
            reportableField.setGetter(getter);
            return reportableField;
        }


    }
}
