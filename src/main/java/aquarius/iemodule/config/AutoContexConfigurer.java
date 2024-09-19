package aquarius.iemodule.config;

import aquarius.iemodule.IEContext;
import aquarius.iemodule.annotations.HyperLink;
import aquarius.iemodule.annotations.Mapper;
import aquarius.iemodule.annotations.ReportConfiguration;
import aquarius.iemodule.impl.util.TemplateConfig;
import aquarius.iemodule.processors.ImportProcessor;
import aquarius.iemodule.structure.*;
import aquarius.iemodule.utils.StringUtils;
import com.google.common.primitives.Primitives;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AutoContexConfigurer {
    private static final Logger LOGGER = LogManager.getLogger();

    public void init(IEProperties properties) throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        IEContext.prop(properties);
        String ieTemplatePath = properties.getIeTemplatePath();
        String ieReportPath = properties.getIeReportPath();
        String ieExportPath = properties.getIeExportPath();
        File file = new File(ieTemplatePath);
        if (!file.exists()) {
            boolean mkdirs = file.mkdirs();
            if (!mkdirs) {
                throw new IllegalStateException("cannot create path " + ieTemplatePath + ".");
            }
        }

        file = new File(ieReportPath);
        if (!file.exists()) {
            boolean mkdirs = file.mkdirs();
            if (!mkdirs) {
                throw new IllegalStateException("cannot create path " + ieReportPath + ".");
            }
        }
        file = new File(ieExportPath);
        if (!file.exists()) {
            boolean mkdirs = file.mkdirs();
            if (!mkdirs) {
                throw new IllegalStateException("cannot create path " + ieExportPath + ".");
            }
        }
        Map<Class<?>, Long> cachedFieldCount = new HashMap();
        Map<Class<? extends Reportable>, Boolean> cachedIterableField = new HashMap();
        Set<Class<? extends Reportable>> subTypesOf = new HashSet<>();
        Set<Class<? extends ImportProcessor>> impProcessors = new HashSet<>();
        for (String pkg : properties.getPackageToScan()) {
            Reflections rf = new Reflections(pkg, new TypeAnnotationsScanner(), new SubTypesScanner());
            subTypesOf.addAll(rf.getSubTypesOf(Reportable.class));
            impProcessors.addAll(rf.getSubTypesOf(ImportProcessor.class));
        }

        IEContext.IEContextBuilder ieContextBuilder = IEContext.getBuilder();

        for (Class<? extends Reportable> reportableEntity : subTypesOf) {
            if (reportableEntity.isInterface()) continue;
            LOGGER.debug("processing '{}'", reportableEntity.getName());
            int cellNum = 0;
            List<Field> declaredFields = new ArrayList<>();
            Class<?> c = reportableEntity;
            do {
                declaredFields.addAll(Stream.of(c.getDeclaredFields()).collect(Collectors.toList()));
            } while (!(c = c.getSuperclass()).equals(Object.class));
            IEContext.ReportEntityBuilder<? extends Reportable> reBuilder = new IEContext.ReportEntityBuilder<>(reportableEntity)
                    .setExportFileNamePrefix("EX_" + reportableEntity.getSimpleName())
                    .setName(reportableEntity.getConstructor().newInstance().reportName());
            Set<FieldWrapper> fieldWrappers = new TreeSet<>();

            Map<String, ReportableConfigurer.ReportableConfig> reportableConfig;
            ReportConfiguration configuration = reportableEntity.getAnnotation(ReportConfiguration.class);
            if (configuration != null) {
                ReportableConfigurer reportableConfigurer = configuration.configurer().getConstructor().newInstance();
                reportableConfig = reportableConfigurer.reportableConfig();
                for (Field f : declaredFields) {
                    ReportableConfigurer.ReportableConfig config = reportableConfig.get(f.getName());
                    if (config == null) continue;
                    fieldWrappers.add(new FieldWrapper(config.getOrder(), f));
                }
            } else {
                reportableConfig = new HashMap<>();
                for (Field f : declaredFields) {
                    if (!f.isAnnotationPresent(aquarius.iemodule.annotations.Reportable.class))
                        continue;

                    aquarius.iemodule.annotations.Reportable reportable = f.getAnnotation(aquarius.iemodule.annotations.Reportable.class);
                    ReportableConfigurer.ReportableConfig config = new ReportableConfigurer.ReportableConfig(reportable.altName(), reportable.order());

                    HyperLink hyperLink = f.getAnnotation(HyperLink.class);
                    if (hyperLink != null) {
                        config.setHyperLinkHelper(hyperLink.helper());
                    }

                    Mapper mapper = f.getAnnotation(Mapper.class);
                    if (mapper != null) {
                        config.setMapper(mapper.mapper());
                    }
                    reportableConfig.put(f.getName(), config);
                    fieldWrappers.add(new FieldWrapper(config.getOrder(), f));
                }
            }


            for (FieldWrapper fw : fieldWrappers) {
                Field f = fw.field;
                ReportableConfigurer.ReportableConfig config = reportableConfig.get(f.getName());
                HyperLinkHelper hyperLinkHelper=null;
                if (config.getHyperLinkHelper() != null) {
                     hyperLinkHelper = config.getHyperLinkHelper().getConstructor().newInstance();
                }
                if (config.getMapper() != null) {
                    IEMapper ieMapper = config.getMapper().getConstructor().newInstance();

                    Class<?> type = config.getMapper().getMethod("toReportable", Primitives.wrap(f.getType())).getReturnType();

                    //////////////////////
                    //////////////////////
                    if (isPrimitive(type)) {
                        reBuilder.addReportableField(
                                new IEContext.ReportableFieldBuilder()
                                        .setStartCell(cellNum++)
                                        .setEndCell(cellNum - 1)
                                        .fieldName(f.getName())
                                        .mapper(ieMapper)
                                        .hyperLinkHelper(hyperLinkHelper)
                                        .order(config.getOrder())
                                        .type(type)
                                        .groupName(StringUtils.hasContent(config.getAltName()) ? config.getAltName() : f.getName())

                        );
                    } else if (Iterable.class.isAssignableFrom(type)) {
                        ParameterizedType genericType = (ParameterizedType) config.getMapper().getMethod("toReportable", Primitives.wrap(f.getType())).getGenericReturnType();
                        ;
                        if (isPrimitive((Class<?>) genericType.getActualTypeArguments()[0])) {
                            reBuilder.addReportableField(
                                    new IEContext.ReportableFieldBuilder()
                                            .setStartCell(cellNum++)
                                            .setEndCell(cellNum - 1)
                                            .fieldName(f.getName())
                                            .order(config.getOrder())
                                            .mapper(ieMapper)
                                            .hyperLinkHelper(hyperLinkHelper)
                                            .genericType(genericType.getActualTypeArguments())

                                            .type(type)
                                            .isIterable(true)
                                            .groupName(StringUtils.hasContent(config.getAltName()) ? config.getAltName() : f.getName())


                            );
                        } else if (Reportable.class.isAssignableFrom((Class<?>) genericType.getActualTypeArguments()[0])) {
                            Class<? extends Reportable> type0 = (Class<? extends Reportable>) genericType.getActualTypeArguments()[0];
                            IEContext.ReportableFieldBuilder reportableFieldBuilder = new IEContext.ReportableFieldBuilder()
                                    .setStartCell(cellNum)
                                    .fieldName(f.getName())
                                    .type(type)
                                    .mapper(ieMapper)
                                    .genericType(genericType.getActualTypeArguments())
                                    .hyperLinkHelper(hyperLinkHelper)
                                    .order(config.getOrder())
                                    .isIterable(true)
                                    .groupName(StringUtils.hasContent(config.getAltName()) ? config.getAltName() : f.getName());
                            Long count = cachedFieldCount.computeIfAbsent(type, k -> fieldCount(k, cachedFieldCount));
                            cellNum += count;
                            reportableFieldBuilder.setEndCell(cellNum - 1);
                            reBuilder.addReportableField(
                                    reportableFieldBuilder
                            );
                        } else {
                            throw new IllegalStateException(String.format("type '%s' is not supported", type.getName()));
                        }

                    } else if (Reportable.class.isAssignableFrom(type)) {
                        Class<? extends Reportable> type0 = (Class<? extends Reportable>) f.getType();
                        IEContext.ReportableFieldBuilder reportableFieldBuilder = new IEContext.ReportableFieldBuilder()
                                .setStartCell(cellNum)
                                .fieldName(f.getName())
                                .order(config.getOrder())
                                .type(type)
                                .mapper(ieMapper)
                                .hyperLinkHelper(hyperLinkHelper)
                                .sudoIterable(cachedIterableField.get(type) != null ? (cachedIterableField.get(type)) : checkIterableContaining(type, cachedIterableField))
                                .groupName(StringUtils.hasContent(config.getAltName()) ? config.getAltName() : f.getName());

                        Long count = cachedFieldCount.computeIfAbsent(type, k -> fieldCount(k, cachedFieldCount));
                        cellNum += count;
                        reportableFieldBuilder.setEndCell(cellNum - 1);
                        reBuilder.addReportableField(
                                reportableFieldBuilder
                        );

                    } else {
                        throw new IllegalStateException(String.format("type '%s' is not supported", type.getName()));
                    }
                    //////////////////////
                    //////////////////////

                } else if (isPrimitive(f.getType())) {
                    reBuilder.addReportableField(
                            new IEContext.ReportableFieldBuilder()
                                    .hyperLinkHelper(hyperLinkHelper)
                                    .setStartCell(cellNum++)
                                    .setEndCell(cellNum - 1)
                                    .fieldName(f.getName())
                                    .order(config.getOrder())
                                    .type(f.getType())
                                    .groupName(StringUtils.hasContent(config.getAltName()) ? config.getAltName() : f.getName())

                    );
                } else if (Iterable.class.isAssignableFrom(f.getType())) {
                    ParameterizedType genericType = (ParameterizedType) f.getGenericType();
                    if (isPrimitive((Class<?>) genericType.getActualTypeArguments()[0])) {
                        reBuilder.addReportableField(
                                new IEContext.ReportableFieldBuilder()
                                        .hyperLinkHelper(hyperLinkHelper)
                                        .setStartCell(cellNum++)
                                        .setEndCell(cellNum - 1)
                                        .fieldName(f.getName())
                                        .order(config.getOrder())
                                        .type(f.getType())
                                        .genericType(genericType.getActualTypeArguments())
                                        .isIterable(true)
                                        .groupName(StringUtils.hasContent(config.getAltName()) ? config.getAltName() : f.getName())


                        );
                    } else if (Reportable.class.isAssignableFrom((Class<?>) genericType.getActualTypeArguments()[0])) {
                        Class<? extends Reportable> type = (Class<? extends Reportable>) genericType.getActualTypeArguments()[0];
                        IEContext.ReportableFieldBuilder reportableFieldBuilder = new IEContext.ReportableFieldBuilder()
                                .setStartCell(cellNum)
                                .fieldName(f.getName())
                                .type(f.getType())
                                .genericType(genericType.getActualTypeArguments())
                                .hyperLinkHelper(hyperLinkHelper)
                                .order(config.getOrder())
                                .isIterable(true)
                                .groupName(StringUtils.hasContent(config.getAltName()) ? config.getAltName() : f.getName());
                        Long count = cachedFieldCount.computeIfAbsent(type, k -> fieldCount(k, cachedFieldCount));
                        cellNum += count;
                        reportableFieldBuilder.setEndCell(cellNum - 1);
                        reBuilder.addReportableField(
                                reportableFieldBuilder
                        );
                    } else {
                        if (config.getMapper() == null)
                            throw new IllegalStateException(String.format("type '%s' is not supported for %s [%s]", f.getType().getName(), reportableEntity.getName(), f.getName()));

                        Class<?> type = config.getMapper().getMethod("toReportable", Primitives.wrap(f.getType())).getReturnType();
                        IEMapper ieMapper = config.getMapper().getConstructor().newInstance();
                        IEContext.ReportableFieldBuilder reportableFieldBuilder = new IEContext.ReportableFieldBuilder()
                                .setStartCell(cellNum)
                                .fieldName(f.getName())
                                .mapper(ieMapper)
                                .genericType(genericType.getActualTypeArguments())
                                .order(config.getOrder())
                                .hyperLinkHelper(hyperLinkHelper)
                                .type(type)
                                .isIterable(true)
                                .groupName(StringUtils.hasContent(config.getAltName()) ? config.getAltName() : f.getName());
                        Long count = cachedFieldCount.computeIfAbsent(type, k -> fieldCount(k, cachedFieldCount));
                        cellNum += count;
                        reportableFieldBuilder.setEndCell(cellNum - 1);
                    }

                } else if (Reportable.class.isAssignableFrom(f.getType())) {
                    Class<? extends Reportable> type = (Class<? extends Reportable>) f.getType();
                    IEContext.ReportableFieldBuilder reportableFieldBuilder = new IEContext.ReportableFieldBuilder()
                            .setStartCell(cellNum)
                            .fieldName(f.getName())
                            .order(config.getOrder())
                            .type(type)
                            .hyperLinkHelper(hyperLinkHelper)
                            .sudoIterable(cachedIterableField.get(type) != null ? (cachedIterableField.get(type)) : checkIterableContaining(type, cachedIterableField))
                            .groupName(StringUtils.hasContent(config.getAltName()) ? config.getAltName() : f.getName());

                    Long count = cachedFieldCount.computeIfAbsent(type, k -> fieldCount(k, cachedFieldCount));
                    cellNum += count;
                    reportableFieldBuilder.setEndCell(cellNum - 1);
                    reBuilder.addReportableField(
                            reportableFieldBuilder
                    );

                } else {
                    if (config.getMapper() == null)
                        throw new IllegalStateException(String.format("type '%s' is not supported", f.getType().getName()));


                }
            }
            ieContextBuilder.add(reportableEntity, reBuilder);

        }
        for (Class<? extends Reportable> reportableEntity : subTypesOf) {

            if (Importable.class.isAssignableFrom(reportableEntity))
                LOGGER.debug("Generating template for {}", reportableEntity.getName());
            for (Class<? extends ImportProcessor> p : impProcessors) {
                if (!p.isInterface()) {
                    Constructor<? extends ImportProcessor> constructor = p.getDeclaredConstructor();
                    boolean accessible = constructor.isAccessible();
                    if (!accessible) {
                        constructor.setAccessible(true);
                    }
                    ImportProcessor importProcessor = constructor.newInstance();
                    if (!accessible) {
                        constructor.setAccessible(false);
                    }
                    if (!importProcessor.isAcceptable(reportableEntity)) continue;
                    if (!reportableEntity.isInterface()) {
                        try {
                            TemplateConfig config = importProcessor.generateTemplate(reportableEntity);
                            ReportableEntity reportableEntity1 = IEContext.getReportableEntity(reportableEntity);
                            reportableEntity1.addTemplateConfig(importProcessor.ProcessorName(), config);
                            reportableEntity1.addTemplate(importProcessor.getClass(), reportableEntity, config.getTemplateName());
                        } catch ( IOException e) {
                            throw new IllegalStateException(String.format("'%s' cannot generate Template", p.getName()), e);
                        }
                    }

                }
            }
        }
    }


    private long fieldCount(Class<?> reportable, Map<Class<?>, Long> cached) {
        try {

//            LOGGER.debug("Counting '{}' fields", reportable.getName());
            long fieldCount = 0;
            List<Field> declaredFields = new ArrayList<>();
            Class<?> c = reportable;
            do {
                declaredFields.addAll(Stream.of(c.getDeclaredFields()).collect(Collectors.toList()));
            } while (c.getSuperclass() != null && !(c = c.getSuperclass()).equals(Object.class));
            Map<String, ReportableConfigurer.ReportableConfig> reportableConfig;

            ReportConfiguration configuration = reportable.getAnnotation(ReportConfiguration.class);
            if (configuration != null) {
                ReportableConfigurer reportableConfigurer = configuration.configurer().getConstructor().newInstance();
                reportableConfig = reportableConfigurer.reportableConfig();
                for (Field f : declaredFields) {
                    ReportableConfigurer.ReportableConfig config = reportableConfig.get(f.getName());
                }
            } else {
                reportableConfig = new HashMap<>();
                for (Field f : declaredFields) {
                    if (!f.isAnnotationPresent(aquarius.iemodule.annotations.Reportable.class))
                        continue;

                    aquarius.iemodule.annotations.Reportable reportable0 = f.getAnnotation(aquarius.iemodule.annotations.Reportable.class);
                    ReportableConfigurer.ReportableConfig config = new ReportableConfigurer.ReportableConfig(reportable0.altName(), reportable0.order());
                    Mapper mapper = f.getAnnotation(Mapper.class);
                    if (mapper != null) {
                        config.setMapper(mapper.mapper());
                    }
                    reportableConfig.put(f.getName(), config);

                }
            }
//         long mainCount = 0;
//         long childCount = 0;
//            Field[] declaredFields = reportable.getDeclaredFields();

            for (Field f : declaredFields) {
                ReportableConfigurer.ReportableConfig config = reportableConfig.get(f.getName());
                if (config != null) {
                    if (Iterable.class.isAssignableFrom(f.getType())) {
                        ParameterizedType genericType = (ParameterizedType) f.getGenericType();

                        if (config.getMapper() != null) {

                            Long count = cached.get((Class<? extends Reportable>) config.getMapper().getMethod("toReportable", Primitives.wrap(f.getType())).getReturnType());
                            if (count == null) {
                                if (Iterable.class.isAssignableFrom(config.getMapper().getMethod("toReportable", Primitives.wrap(f.getType())).getReturnType())) {
                                    ParameterizedType gt = (ParameterizedType) config.getMapper().getMethod("toReportable", Primitives.wrap(f.getType())).getGenericReturnType();
                                    if (isPrimitive((Class<?>) gt.getActualTypeArguments()[0])) {
                                        fieldCount++;
                                    } else if (Reportable.class.isAssignableFrom((Class<?>) gt.getActualTypeArguments()[0])) {
                                        count = fieldCount((Class<?>) gt.getActualTypeArguments()[0], cached);
                                        fieldCount += count;
                                    }
                                } else if (isPrimitive(config.getMapper().getMethod("toReportable", Primitives.wrap(f.getType())).getReturnType())) {
                                    fieldCount++;
                                } else if (Reportable.class.isAssignableFrom(config.getMapper().getMethod("toReportable", Primitives.wrap(f.getType())).getReturnType())) {
                                    count = fieldCount(config.getMapper().getMethod("toReportable", Primitives.wrap(f.getType())).getReturnType(), cached);
                                    fieldCount += count;
                                }


                            }

                            ;

                        } else if (isPrimitive((Class<?>) genericType.getActualTypeArguments()[0])) {
                            fieldCount++;
                        } else if (Reportable.class.isAssignableFrom((Class<?>) genericType.getActualTypeArguments()[0])) {
                            Long count = cached.get((Class<? extends Reportable>) genericType.getActualTypeArguments()[0]);

                            if (count == null) {
                                count = fieldCount((Class<? extends Reportable>) genericType.getActualTypeArguments()[0], cached);
                            }
                            fieldCount += count;
                            ;

                        }
                    } else if (config.getMapper() != null) {
                        Long count = cached.get(config.getMapper().getMethod("toReportable", Primitives.wrap(f.getType())).getReturnType());

                        if (count == null) {
                            if (Iterable.class.isAssignableFrom(config.getMapper().getMethod("toReportable", Primitives.wrap(f.getType())).getReturnType())) {
                                ParameterizedType gt = (ParameterizedType) config.getMapper().getMethod("toReportable", Primitives.wrap(f.getType())).getGenericReturnType();
                                if (isPrimitive((Class<?>) gt.getActualTypeArguments()[0])) {
                                    fieldCount++;
                                } else if (Reportable.class.isAssignableFrom((Class<?>) gt.getActualTypeArguments()[0])) {
                                    count = fieldCount((Class<?>) gt.getActualTypeArguments()[0], cached);
                                    fieldCount += count;
                                }
                            } else if (isPrimitive(config.getMapper().getMethod("toReportable", Primitives.wrap(f.getType())).getReturnType())) {
                                fieldCount++;
                            } else if (Reportable.class.isAssignableFrom(config.getMapper().getMethod("toReportable", Primitives.wrap(f.getType())).getReturnType())) {
                                count = fieldCount(config.getMapper().getMethod("toReportable", Primitives.wrap(f.getType())).getReturnType(), cached);
                                fieldCount += count;
                            }

                        }

                    } else if (isPrimitive(f.getType())) {
                        fieldCount++;
                    } else if (Reportable.class.isAssignableFrom(f.getType())) {
                        Long count = cached.get((Class<? extends Reportable>) f.getType());
                        if (count == null) {
                            count = fieldCount((Class<? extends Reportable>) f.getType(), cached);
                        }
                        fieldCount += count;
                        ;

                    }
                }
            }
            return fieldCount;
        } catch (Exception e) {
            throw new IllegalStateException("fail to config IE", e);
        }
    }

    private boolean checkIterableContaining(Class<?> reportable, Map<Class<? extends Reportable>, Boolean> cached) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<Field> declaredFields = new ArrayList<>();
        Class<?> c = reportable;
        do {
            declaredFields.addAll(Stream.of(c.getDeclaredFields()).collect(Collectors.toList()));
        } while (!(c = c.getSuperclass()).equals(Object.class));
        Map<String, ReportableConfigurer.ReportableConfig> reportableConfig;

        ReportConfiguration configuration = reportable.getAnnotation(ReportConfiguration.class);
        if (configuration != null) {
            ReportableConfigurer reportableConfigurer = configuration.configurer().getConstructor().newInstance();
            reportableConfig = reportableConfigurer.reportableConfig();
            for (Field f : declaredFields) {
                ReportableConfigurer.ReportableConfig config = reportableConfig.get(f.getName());
            }
        } else {
            reportableConfig = new HashMap<>();
            for (Field f : declaredFields) {
                if (!f.isAnnotationPresent(aquarius.iemodule.annotations.Reportable.class))
                    continue;

                aquarius.iemodule.annotations.Reportable reportable0 = f.getAnnotation(aquarius.iemodule.annotations.Reportable.class);
                ReportableConfigurer.ReportableConfig config = new ReportableConfigurer.ReportableConfig(reportable0.altName(), reportable0.order());
                Mapper mapper = f.getAnnotation(Mapper.class);
                if (mapper != null) {
                    config.setMapper(mapper.mapper());
                }
                reportableConfig.put(f.getName(), config);

            }
        }

        for (Field f : declaredFields) {
            ReportableConfigurer.ReportableConfig config = reportableConfig.get(f.getName());
            if (config != null) {
                if (isPrimitive(f.getType())) {
                    return false;
                } else if (Iterable.class.isAssignableFrom(f.getType())) {
                    return true;
                } else if (Reportable.class.isAssignableFrom(f.getType())) {
                    Boolean aBoolean = cached.get((Class<? extends Reportable>) f.getType());
                    return aBoolean != null ? aBoolean : checkIterableContaining((Class<? extends Reportable>) f.getType(), cached);
                } else if (config.getMapper() != null) {
                    Boolean aBoolean = cached.get((Class<? extends Reportable>) config.getMapper().getMethod("toReportable", Primitives.wrap(f.getType())).getReturnType());
                    return aBoolean != null ? aBoolean : checkIterableContaining((Class<? extends Reportable>) config.getMapper().getMethod("toReportable", Primitives.wrap(f.getType())).getReturnType(), cached);

                }
            }

        }
        return false;
    }

    private boolean isPrimitive(Class<?> type) {
        return type.isPrimitive() || type.isEnum() ||
                type.equals(String.class) ||
                type.equals(Boolean.class) ||
                type.equals(Integer.class) ||
                type.equals(Double.class) ||
                type.equals(Float.class) ||
                type.equals(Short.class) ||
                type.equals(Long.class) ||
                type.equals(Date.class) ||
                type.equals(Byte.class);
    }


    class FieldWrapper implements Comparable<FieldWrapper> {
        private int order;

        private Field field;

        public FieldWrapper(int order, Field field) {
            this.order = order;
            this.field = field;
        }


        @Override
        public int compareTo( FieldWrapper o) {
            int i = this.order - o.order;
            if (i == 0 && this.field.getName().equals(o.field.getName())) return i;
            else if (i == 0) {
                return 1;
            }
            return i;
        }
    }


}
