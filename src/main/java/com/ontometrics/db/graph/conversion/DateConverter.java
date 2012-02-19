package com.ontometrics.db.graph.conversion;

import java.util.Date;

public class DateConverter implements TypeConverter{

	public Class<?> getType() {
		return Date.class;
	}

	public Object convertToPrimitive(Object value) {
		Date date = (Date) value;
		return date.getTime();
	}

	public Object convertFromPrimitive(Object value) {
		Long time = (Long) value;
		return new Date(time);
	}

}
