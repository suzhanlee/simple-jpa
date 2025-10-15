package io.simplejpa.annotation.relation;

import io.simplejpa.annotation.CascadeType;
import io.simplejpa.annotation.FetchType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ManyToMany {
    String mappedBy() default "";

    FetchType fetch() default FetchType.LAZY;

    CascadeType[] cascade() default {};
}
