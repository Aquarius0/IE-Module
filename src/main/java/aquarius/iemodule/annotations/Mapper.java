package aquarius.iemodule.annotations;

import aquarius.iemodule.structure.IEMapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Mapper {
   Class<? extends IEMapper<?,?>> mapper();
}
