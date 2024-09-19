package aquarius.iemodule.structure;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class ReportableField implements Comparable<ReportableField> {

    private boolean iterable;

    private boolean sudoIterable;
    private Class<?> type;
    private String groupName;
    private String fieldName;
    private Method getter;
    private Method setter;

    private int startCell;

    private int endCell;
    private int order;

    private Type[] genericType;

    private HyperLinkHelper hyperLinkHelper;

    public HyperLinkHelper getHyperLinkHelper() {
        return hyperLinkHelper;
    }

    public void setHyperLinkHelper(HyperLinkHelper hyperLinkHelper) {
        this.hyperLinkHelper = hyperLinkHelper;
    }

    public void setGenericType(Type[] type) {
        genericType = type;
    }

    public Type[] getGenericType() {
        return genericType;
    }

    public boolean isIterable() {
        return iterable;
    }

    public void setIterable(boolean iterable) {
        this.iterable = iterable;
    }

    public boolean isSudoIterable() {
        return sudoIterable;
    }

    public void setSudoIterable(boolean sudoIterable) {
        this.sudoIterable = sudoIterable;
    }

    public int getOrder() {
        return order;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    private IEMapper mapper;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Method getGetter() {
        return getter;
    }

    public void setGetter(Method getter) {
        this.getter = getter;
    }

    public Method getSetter() {
        return setter;
    }

    public void setSetter(Method setter) {
        this.setter = setter;
    }

    public IEMapper getMapper() {
        return mapper;
    }

    public void setMapper(IEMapper mapper) {
        this.mapper = mapper;
    }

    public int getStartCell() {
        return startCell;
    }

    public void setStartCell(int startCell) {
        this.startCell = startCell;
    }

    public int getEndCell() {
        return endCell;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setEndCell(int endCell) {
        this.endCell = endCell;
    }

    public Object getValue(Reportable reportable) throws InvocationTargetException, IllegalAccessException {
        if (mapper == null)
            return this.getGetter().invoke(reportable);
        return mapper.toReportable(this.getGetter().invoke(reportable));
    }

    public void setValue(Object reportable, Object value) throws InvocationTargetException, IllegalAccessException {
        if (mapper == null)
            this.getSetter().invoke(reportable, value);
        else
            this.getSetter().invoke(reportable, mapper.toActualObject(value));
    }

    @Override
    public int compareTo(@NotNull ReportableField o) {
        int i = this.order - o.order;
        if (i == 0 && this.fieldName.equals(o.fieldName)) return i;
        else if (i == 0) {
            return 1;
        }
        return i;
    }
//    public Class acceptType() throws NoSuchMethodException {
//        return this.mapper != null ? ((Method) Stream.of(this.mapper.getClass().getMethods()).filter((e) -> {
//            return e.getName().equals("convert");
//        }).findFirst().orElseThrow(NoSuchMethodException::new)).getReturnType() : this.acceptType;
//    }
}
