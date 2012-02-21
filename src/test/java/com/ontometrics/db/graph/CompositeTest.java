package com.ontometrics.db.graph;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	private static final Logger log = LoggerFactory.getLogger(CompositeTest.class);
	
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
		
		Node node = managerRepository.create(pete);
		long rootNodeID = node.getId();
		
		assertThat(pete.getSubordinates().size(), is(3));
		
		Node newNode = database.getDatabase().getNodeById(rootNodeID);
		
		assertThat(newNode, is(notNullValue()));
		
		int nodeCount = 0;
		Iterator<Node> i = database.getDatabase().getAllNodes().iterator();
		while (i.hasNext()){
			nodeCount++;
			i.next();
		}

		log.info("nodes in database: {}", nodeCount);
		
		assertThat(nodeCount, is(5));
		
	}
	
	
}
