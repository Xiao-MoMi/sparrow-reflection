package net.momirealms.sparrow.reflection.proxy.annotation;

import net.momirealms.sparrow.reflection.proxy.Strategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldGetter {

    String name() default "";

    String[] names() default {};

    Strategy strategy() default Strategy.ASM;

    boolean isStatic() default false;
}
