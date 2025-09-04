package aquarius.iemodule.structure;



import aquarius.iemodule.impl.util.TemplateConfig;
import aquarius.iemodule.processors.ImportProcessor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ReportableEntity {
    private Class<?> entity;

    private String name;

    Map<String,String> template=new HashMap<>();
    Map<String, TemplateConfig> templateConfig = new HashMap<>();

    private String ExportFileNamePrefix;

    private Set<ReportableField> mainFields;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    //    private Map<String, List<ReportableField>> exclusiveFieldGroup;

    public Class<?> getEntity() {
        return entity;
    }

    public void setEntity(Class<?> entity) {
        this.entity = entity;
    }

    public void addTemplateConfig(String processorName, TemplateConfig config) {
        if(templateConfig.containsKey(processorName)){
            throw new IllegalStateException(String.format("'%s' processor already exists",processorName));
        }

        templateConfig.put(processorName, config);
    }

    public Object getTemplateConfig(String processorName) {
        return templateConfig.get(processorName);
    }

    public String getExportFileNamePrefix() {
        return ExportFileNamePrefix;
    }

    public void setExportFileNamePrefix(String exportFileNamePrefix) {
        ExportFileNamePrefix = exportFileNamePrefix;
    }

    public Set<ReportableField> getMainFields() {
        return mainFields;
    }

    public void setMainFields(Set<ReportableField> mainFields) {
        this.mainFields = mainFields;
    }

    public void add(ReportableField reportableField) {
        if (mainFields == null) {
            mainFields = new TreeSet<>();
        }
        mainFields.add(reportableField);
    }
    public  void addTemplate(Class<? extends ImportProcessor> processor, Class<? extends Reportable> importable, String template){
        this.template.put(String.format("%s-%s",processor.getName(),importable.getName()),template);
    }
    public  String getTemplate(Class<? extends ImportProcessor> processor,Class<? extends Importable> importable){
       return template.get(String.format("%s-%s",processor.getName(),importable.getName()));

    }

}
