package io.simplejpa.annotation.relation;

import io.simplejpa.annotation.CascadeType;
import io.simplejpa.annotation.FetchType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface ManyToOne {
    FetchType fetch() default FetchType.EAGER;

    CascadeType[] cascade() default {};

    boolean unique() default false;
}
