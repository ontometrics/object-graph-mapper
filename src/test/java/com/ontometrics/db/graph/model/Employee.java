package com.ontometrics.db.graph.model;

import java.util.Date;

import com.ontometrics.db.graph.Person;


/**
 * An sub class of Person so we can test that parent class properties are preserved in the graph DB.
 * @author aakture
 *
 */
public class Employee extends Person {

	private Date hireDate;
	private String departmentName;
	
	public Employee() {
		
	}
	
	public Employee(String name, Date birthdate, String departmentName, Date hireDate) {
		super(name, birthdate);
		this.hireDate = hireDate;
		this.departmentName = departmentName;
	}


	public Date getHireDate() {
		return hireDate;
	}

	public void setHireDate(Date hireDate) {
		this.hireDate = hireDate;
	}

	public String getDepartmentName() {
		return departmentName;
	}

	public void setDepartmentName(String departmentName) {
		this.departmentName = departmentName;
	}
	
}
