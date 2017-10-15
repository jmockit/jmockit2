package org.jmockit;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 *
 */
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface Mocked
{
}
