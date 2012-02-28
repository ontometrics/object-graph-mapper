package com.ontometrics.db.graph;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ FIELD })
@Retention(RUNTIME)
public @interface Index {
	/**
	 * The key in the key/value pair to associate with the property in the index. The default will use the property name.
	 * 
	 * @return
	 */
	String key() default "n/a";
	
	
	/**
	 * The field's name to get the value from where the value is value in the key/value pair to associate with the property in the index. 
	 * The default is the property value for single properties, for relationships it will be null.
	 * 
	 * The field referenced in the "value", should not be a relationship.
	 * 
	 * Only set the key/value when you are indexing a relationship
	 * @return
	 */
	String value() default "n/a";

}
