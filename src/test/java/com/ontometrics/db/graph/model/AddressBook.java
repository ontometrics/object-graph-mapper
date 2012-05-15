package com.ontometrics.db.graph.model;

import java.util.Map;

public class AddressBook {

	private Map<Person, String> phones;

	public AddressBook() {
		super();
	}

	public AddressBook(Map<Person, String> phones) {
		super();
		this.phones = phones;
	}

	public Map<Person, String> getPhones() {
		return phones;
	}

}
