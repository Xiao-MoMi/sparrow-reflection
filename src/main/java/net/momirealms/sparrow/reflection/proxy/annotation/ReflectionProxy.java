package net.momirealms.sparrow.reflection.proxy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReflectionProxy {

    Class<?> clazz() default Object.class;

    String[] name() default {};

    boolean ignoreRelocation() default false;

    boolean nullable() default false;

    String activeIf() default "";
}
