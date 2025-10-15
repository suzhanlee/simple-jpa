package io.simplejpa.annotation.relation;

import io.simplejpa.annotation.CascadeType;
import io.simplejpa.annotation.FetchType;

public @interface ManyToMany {
    String mappedBy() default "";

    FetchType fetch() default FetchType.LAZY;

    CascadeType[] cascade() default {};
}
