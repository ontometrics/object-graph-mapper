package com.ontometrics.db.graph.conversion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnumConverter implements TypeConverter {
	private static final Logger log = LoggerFactory.getLogger(EnumConverter.class);
	public Class<?> getType() {
		return Enum.class;
	}

	public Object convertToPrimitive(Object value) {
		return value.toString();
	}

	public Object convertFromPrimitive(Object value) {
		log.debug("converting to Enum from {}", value);
		return Enum.valueOf((Class<Enum>) value, (String)value);
	}
	public enum Day {
		Monday, Tuesday, Wednesday;
	}
	public static void main(String[] args) {
		System.out.println(Enum.valueOf(Day.class, "Monday"));
	}

}
