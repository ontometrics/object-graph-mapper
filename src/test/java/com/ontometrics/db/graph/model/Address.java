package com.ontometrics.db.graph.model;

import com.ontometrics.db.graph.GeneratedId;
import com.ontometrics.db.graph.Id;

public class Address {

	@Id
	@GeneratedId
	private Long id;
	
	private String name;
	
	private String city;
	
	private String country;
	
	private Person owner; //make relation bidirectional
	
	public Address() {
		super();
	}

	public Address(String name, String city, String country) {
		super();
		this.name = name;
		this.city = city;
		this.country = country;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public Person getOwner() {
		return owner;
	}

	public void setOwner(Person owner) {
		this.owner = owner;
	}

	public Long getId() {
		return id;
	}
}
