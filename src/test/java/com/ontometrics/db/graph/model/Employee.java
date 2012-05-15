package com.ontometrics.db.graph.model;

import java.util.Date;

/**
 * An sub class of Person so we can test that parent class properties are
 * preserved in the graph DB.
 * 
 * @author aakture
 * 
 */
public class Employee extends Person {

	private Date hireDate;
	private String departmentName;
	private Manager boss;

	public Employee() {

	}

	public Employee(String name, Date birthdate, String departmentName, Date hireDate) {
		super(name, birthdate);
		this.hireDate = hireDate;
		this.departmentName = departmentName;
	}

	public Employee(String name) {
		super(name);
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

	public Manager getBoss() {
		return boss;
	}

	public void setBoss(Manager boss) {
		this.boss = boss;
	}

	@Override
	public String toString() {
		return "Employee [hireDate=" + hireDate + ", departmentName=" + departmentName + ", boss=" + boss
				+ ", getName()=" + getName() + "]";
	}


	
}
