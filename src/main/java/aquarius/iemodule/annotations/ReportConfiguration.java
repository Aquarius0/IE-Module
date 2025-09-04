package aquarius.iemodule.autoconfiguration.annotations;

import aquarius.iemodule.structure.ReportableConfigurer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ReportConfiguration {

    Class<? extends ReportableConfigurer> configurer();
}
