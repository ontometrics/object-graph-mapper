package com.ontometrics.db.graph;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ontometrics.db.graph.model.Employee;
import com.ontometrics.db.graph.model.Manager;
import com.ontometrics.testing.TestGraphDatabase;

/**
 * To verify that we can create a class that implements the Composite Design
 * Pattern and it will be persisted properly.
 * 
 * @author Rob
 */
public class CompositeTest {
	
	private EntityRepository<Manager> managerRepository = new EntityRepository<Manager>();
	
	@Rule
	public TemporaryFolder dbFolder = new TemporaryFolder();
	
	@Rule
	public TestGraphDatabase database = new TestGraphDatabase(dbFolder);

	private EntityManager entityManager;
	
	@Before
	public void setup(){
		entityManager = new EntityManager(database.getDatabase());
		managerRepository.setEntityManager(entityManager);
	}

	@Test
	public void canPersistComposite(){
		
		Employee joe = new Employee("Joe");
		Employee jim = new Employee("Jim");
		Employee bob = new Employee("Bob");
		Manager pete = new Manager("Pete");
		
		pete.addSubordinate(joe);
		pete.addSubordinate(jim);
		pete.addSubordinate(bob);
		pete.addSubordinate(pete);
		
		managerRepository.create(pete);
		
		
		
	}
	
	
}
