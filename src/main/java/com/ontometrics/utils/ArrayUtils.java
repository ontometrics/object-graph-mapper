package com.ontometrics.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ArrayUtils {
	/**
	 * convert the given collection to array of primitives and set it to the node's property.
	 *  
	 * @param node
	 * @param name
	 * @param collection
	 */
	public static Object toPrimitives(Collection<Object> collection) {
		if(collection == null) return null;
		List<Object> value = new ArrayList<Object>(collection);
		Object firstObject = value.get(0);
		
		if(firstObject instanceof String){
			String[] array = new String[collection.size()];
			int index = 0;
			for(Object object : collection){
				array[index++] = (String) object;
			}
			
			return array;
		}

		if(firstObject instanceof Integer){
			int[] array = new int[collection.size()];
			int index = 0;
			for(Object object : collection){
				array[index++] = ((Integer) object).intValue();
			}
			
			return array;
		}

		if(firstObject instanceof Boolean){
			boolean[] array = new boolean[collection.size()];
			int index = 0;
			for(Object object : collection){
				array[index++] = ((Boolean) object).booleanValue();
			}
			
			return array;
		}

		if(firstObject instanceof Float){
			float[] array = new float[collection.size()];
			int index = 0;
			for(Object object : collection){
				array[index++] = ((Float) object).floatValue();
			}
			
			return array;
		}

		if(firstObject instanceof Long){
			long[] array = new long[collection.size()];
			int index = 0;
			for(Object object : collection){
				array[index++] = ((Long) object).longValue();
			}
			
			return array;
		}

		if(firstObject instanceof Double){
			double[] array = new double[collection.size()];
			int index = 0;
			for(Object object : collection){
				array[index++] = ((Double) object).doubleValue();
			}
			
			return array;
		}

		if(firstObject instanceof Byte){
			byte[] array = new byte[collection.size()];
			int index = 0;
			for(Object object : collection){
				array[index++] = ((Byte) object).byteValue();
			}
			
			return array;
		}

		if(firstObject instanceof Character){
			char[] array = new char[collection.size()];
			int index = 0;
			for(Object object : collection){
				array[index++] = ((Character) object).charValue();
			}
			
			return array;
		}

		if(firstObject instanceof Short){
			short[] array = new short[collection.size()];
			int index = 0;
			for(Object object : collection){
				array[index++] = ((Short) object).shortValue();
			}
			
			return array;
		}

		return null;
	}
}
