package com.ontometrics.db.graph;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Test;

import com.ontometrics.db.graph.conversion.TypeConverter;
import com.ontometrics.db.graph.conversion.TypeRegistry;
import com.ontometrics.db.graph.model.Person;

public class TypeRegistryTest {

	@Test
	public void registerAndUnRegisterConverters() {
		TypeConverter testConverter = new TypeConverter() {
			
			public Class<?> getType() {
				return Person.class;
			}

			public Object convertToPrimitive(Object value) {
				// TODO Auto-generated method stub
				return null;
			}

			public Object convertFromPrimitive(Object value) {
				// TODO Auto-generated method stub
				return null;
			}
		};
		
		TypeRegistry.register(testConverter);
		assertThat(TypeRegistry.getConverter(Person.class), is(testConverter));
		
		TypeRegistry.unRegister(testConverter);
		assertThat(TypeRegistry.getConverter(Person.class), nullValue());
		
	}
}
