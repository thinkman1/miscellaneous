package com.jpmc.dart.util.staxutils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used for StreamingXmlToObject, annotate a class with this to tell the parser
 * to create a new instance of the object stack for the properties to be set.
 * 
 * @author e001668
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CreateOnXPath {
	String xpath();
}
