package org.jmockit;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 *
 */
@Target({FIELD, PARAMETER, METHOD})
@Retention(RUNTIME)
public @interface Tested {
    /**
     *
     */
    String value() default "";
}
