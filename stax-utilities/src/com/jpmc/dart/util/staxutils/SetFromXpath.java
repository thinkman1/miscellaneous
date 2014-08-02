package com.jpmc.dart.util.staxutils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * used with StreamingXmlToObject, populate a property of an object with the
 * value from the xpath.
 * 
 * @author e001668
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface SetFromXpath {
	String xpath();
}
