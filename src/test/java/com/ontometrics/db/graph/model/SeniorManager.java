package com.ontometrics.db.graph.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeniorManager extends Manager {
	private static final Logger log = LoggerFactory.getLogger(SeniorManager.class);
	public SeniorManager() {
		super();
	}
	public SeniorManager(String name, Car car) {
		super(name);
		this.car = car;
	}
	private Car car;
	@Override
	public String toString() {
		return "SeniorManager [car=" + car + ", " + super.toString() + "]";
	}
	public void setCar(Car car) {
		this.car = car;
	}

	
	
}
