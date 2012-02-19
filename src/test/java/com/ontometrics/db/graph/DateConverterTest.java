package com.ontometrics.db.graph;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.util.Date;

import org.junit.Test;

import com.ontometrics.db.graph.conversion.DateConverter;

public class DateConverterTest {

	@Test
	public void conversion() {
		DateConverter dateConverter = new DateConverter();
		Date date = new Date();
		Object convertedValue = dateConverter.convertToPrimitive(date);
		
		assertThat(convertedValue, instanceOf(Long.class));
		assertThat((Long) convertedValue, is(date.getTime()));

		assertThat((Date) dateConverter.convertFromPrimitive(convertedValue), is(date));
	}
}
