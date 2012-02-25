package com.ontometrics.db.graph.model;

import java.util.ArrayList;
import java.util.List;

public class Manager extends Employee {
	
	private Manager boss;
	
	private List<Employee> subordinates = new ArrayList<Employee>();
	public Manager() {
		
	}
	public Manager(String name) {
		super(name);
	}

	public Manager getBoss() {
		return boss;
	}
	
	public List<Employee> getSubordinates() {
		return subordinates;
	}

	public void addSubordinate(Employee employee) {
		this.subordinates.add(employee);
	}
	@Override
	public String toString() {
		return "Manager [boss=" + boss + ", subordinates=" + subordinates + "]";
	}
	
	public void setSubordinates(List<Employee> subordinates) {
		this.subordinates = subordinates;
	}

}
