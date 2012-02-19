package com.ontometrics.db.graph.conversion;

public interface TypeConverter {

	public Class<?> getType();

	public Object convertToPrimitive(Object value);

	public Object convertFromPrimitive(Object value);

}
