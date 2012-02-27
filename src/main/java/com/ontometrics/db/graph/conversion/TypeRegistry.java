package com.ontometrics.db.graph.conversion;

import java.util.HashMap;
import java.util.Map;

public class TypeRegistry {

	private static Map<String, TypeConverter> converters = new HashMap<String, TypeConverter>();
	
	public static void register(TypeConverter converter){
		converters.put(converter.getType().getName(), converter);
	}
	
	public static void unRegister(TypeConverter converter){
		converters.remove(converter.getType().getName());
	}
	
	public static TypeConverter getConverter(Class<?> objectClass){
		if(converters.isEmpty()){
			addMainConverters();
		}
		if(objectClass.isEnum()) {
			return converters.get(Enum.class.getName());
		}
		if(converters.containsKey(objectClass.getName())){
			return converters.get(objectClass.getName());
		}
		return null;
	}

	private static void addMainConverters() {
		TypeRegistry.register(new DateConverter());
		TypeRegistry.register(new EnumConverter());
		
	}

}
