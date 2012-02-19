package com.ontometrics.db.graph;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Id;


public class Person {

	@Id
	private String name;
	
	transient private long visitDuration = 1000; 

	private Date birthDate;

	private Address address;
	
	private Person parent;  
	
	private Set<Person> friends = new HashSet<Person>();
	
	public Person() {
	}

	public Person(String name, Date birthdate) {
		this.name = name;
		this.birthDate = birthdate;
	}

	public String getName() {
		return name;
	}

	public Date getBirthDate() {
		return birthDate;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
		if(address != null){
			this.address.setOwner(this);
		}
	}

	public Person getParent() {
		return parent;
	}

	public void setParent(Person parent) {
		this.parent = parent;
	}

	public Set<Person> getFriends() {
		return friends;
	}

	public void setFriends(Set<Person> friends) {
		this.friends = friends;
	}			
	
	public long getVisitDuration() {
		return visitDuration;
	}
	
}