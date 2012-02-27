package com.ontometrics.db.graph.conversion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

public class EnumConverterTest {

	enum MyEnum{
		Testing;
	}
	
	@Test
	public void convertingEnums(){
		EnumConverter converter = new EnumConverter(MyEnum.class);
		TypeRegistry.register(converter);
		
		
		assertThat((EnumConverter)TypeRegistry.getConverter(MyEnum.class), is(converter));
		assertThat((String) converter.convertToPrimitive(MyEnum.Testing), is("Testing"));
		assertThat((MyEnum) converter.convertFromPrimitive("Testing"), is(MyEnum.Testing));
	}
}
