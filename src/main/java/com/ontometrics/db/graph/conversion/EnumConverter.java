package com.ontometrics.db.graph.conversion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnumConverter implements TypeConverter {
	private static final Logger log = LoggerFactory.getLogger(EnumConverter.class);
	
	@SuppressWarnings("rawtypes")
	private Class<? extends Enum> enumClass;
	
	
	public EnumConverter(@SuppressWarnings("rawtypes") Class<? extends Enum> enumClass) {
		super();
		this.enumClass = enumClass;
	}

	public Class<?> getType() {
		return enumClass;
	}

	public Object convertToPrimitive(Object value) {
		return value.toString();
	}

	
	@SuppressWarnings("unchecked")
	public Object convertFromPrimitive(Object value) {
		log.debug("converting to Enum from {}", value);
		return Enum.valueOf(enumClass, (String) value);
	}
}
