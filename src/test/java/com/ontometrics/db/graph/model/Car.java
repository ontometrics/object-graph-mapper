package com.ontometrics.db.graph.model;

public class Car {
	private String type;
	
	public Car() {
		
	}
	public Car(String type) {
		this.type = type;
	}
	public String getType() {
		return type;
	}
	@Override
	public String toString() {
		return "Car [type=" + type + "]";
	}
	public void setType(String type) {
		this.type = type;
	}
	
	
}
