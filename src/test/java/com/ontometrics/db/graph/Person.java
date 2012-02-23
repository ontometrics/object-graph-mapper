package com.ontometrics.db.graph;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Id;

public class Person {

	@Id
	private String name;

	transient private long visitDuration = 1000; 

	@Index
	private Date birthDate;

	private Address address;

	@Index(key = "child", value = "name")
	private Person parent;

	private Set<Person> friends = new HashSet<Person>();

	public Person() {
	}

	public Person(String name) {
		this.name = name;
	}

	public Person(String name, Date birthdate) {
		this.name = name;
		this.birthDate = birthdate;
	}

	public String getName() {
		return name;
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	public Date getBirthDate() {
		return birthDate;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
		if (address != null) {
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

	@Override
	public boolean equals(Object arg0) {
		if(!arg0.getClass().equals(this.getClass())) return false;
		Person person = (Person) arg0;
		if(this.getName().equals(person.getName())) return true;
		return false;
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	
}
