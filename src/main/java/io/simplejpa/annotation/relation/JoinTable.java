package io.simplejpa.annotation.relation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JoinTable {
    String name() default "";

    JoinColumn[] joinColumns() default {};

    JoinColumn[] inverseJoinColumns() default {};
}
