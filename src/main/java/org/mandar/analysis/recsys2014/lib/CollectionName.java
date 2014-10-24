package org.mandar.analysis.recsys2014.lib;

import org.grouplens.lenskit.core.Parameter;

import javax.inject.Qualifier;
import java.lang.annotation.*;

/**
 * Created by mandar on 1/6/14.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Qualifier
@Parameter(String.class)

public @interface CollectionName {
}
