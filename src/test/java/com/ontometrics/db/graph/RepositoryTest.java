package com.ontometrics.db.graph;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ontometrics.db.graph.model.Car;
import com.ontometrics.db.graph.model.Employee;
import com.ontometrics.db.graph.model.Manager;
import com.ontometrics.db.graph.model.Person;
import com.ontometrics.db.graph.model.RepositoryTestCase;
import com.ontometrics.db.graph.model.SeniorManager;

public class RepositoryTest extends RepositoryTestCase {

	private Logger log = LoggerFactory.getLogger(RepositoryTest.class);
	public EntityRepository<Person> repository;

	@Before
	public void setUp() {
		super.setUp();
		repository = new EntityRepository<Person>();
		repository.setEntityManager(entityManager);
	}

	@Test
	public void repositoryShouldHaveFindByPrimaryKey() {
		Person person = new Person("neo4j", new Date());
		Node node = repository.create(person);
		log.info("node name is {}", node.getProperty("name"));
		assertThat((String) node.getProperty("name"), is("neo4j"));
	}

	@Test
	public void repositoryCanReadEntity() throws Exception {
		Date birthDate = new Date();
		Person person = new Person("neo4j", birthDate);
		repository.create(person);
		Person readPerson = repository.read(Person.class, "neo4j");
		assertThat(readPerson.getName(), is("neo4j"));
		assertThat(readPerson.getBirthDate(), is(birthDate));
	}

	@Test
	public void repositoryCanUpdateEntity() throws Exception {
		// create a person
		Date originalBirthDate = new Date();
		Person person = new Person("neo4j", originalBirthDate);
		repository.create(person);

		// update this person's birth date
		Date newBirthdate = new SimpleDateFormat("MM/dd/yyyy").parse("1/1/2000");
		log.info("new birthdate: {}", newBirthdate);
		Person udpatedPerson = new Person("neo4j", newBirthdate);
		repository.update(udpatedPerson);

		// lookup the person and verify the birth date has been updated
		Person readPerson = repository.read(Person.class, "neo4j");
		assertThat(readPerson.getName(), is("neo4j"));
		assertThat(readPerson.getBirthDate(), is(newBirthdate));
	}

	@Test
	public void repositoryCanDeleteEntities() throws Exception {
		// create a person
		Date originalBirthDate = new Date();
		Person person = new Person("neo4j", originalBirthDate);
		repository.create(person);

		// make sure it exists before we delete
		Person readPerson = repository.read(Person.class, "neo4j");
		assertThat(readPerson.getName(), notNullValue());

		repository.destroy(person, "neo4j");

		// make sure it has been deleted
		boolean isDeleted = false;
		try {
			readPerson = repository.read(Person.class, "neo4j");
		} catch (IllegalArgumentException ex) {
			isDeleted = true;
		}
		assertThat(isDeleted, is(true));
	}
	
	@Test 
	public void superClassCollectionsAreRetrieved() {
		EntityRepository<SeniorManager> repo = new EntityRepository<SeniorManager>();
		repo.setEntityManager(entityManager);
		SeniorManager seniorManager = new SeniorManager("Bob", new Car("Cadillac"));
		seniorManager.addSubordinate(new Employee("Tim"));
		repo.create(seniorManager);
		log.debug("senior manager: {}", repo.read(SeniorManager.class, "Bob"));
		
		
	}
	
	@Test
	public void canUpdateEntityWithCollection() {
		EntityRepository<Manager> repo = new EntityRepository<Manager>();
		repo.setEntityManager(entityManager);
		Manager manager = new Manager("Bob");
		manager.addSubordinate(new Employee("Tom", new DateTime().minusYears(30).toDate(), "Engineering", new Date()));
		
		repo.create(manager);
		Manager retrievedManager = repo.read(Manager.class, "Bob");
		assertThat(retrievedManager.getSubordinates().size(), is(1));
		manager.addSubordinate(new Employee("Sharon", new DateTime().minusYears(20).toDate(), "Engineering", new Date()));
		repo.update(manager);

		retrievedManager = repo.read(Manager.class, "Bob");
		assertThat(retrievedManager.getSubordinates().size(), is(2));

		manager.getSubordinates().remove(0);
		repo.update(manager);

		retrievedManager = repo.read(Manager.class, "Bob");
		assertThat(retrievedManager.getSubordinates().size(), is(1));
		
	}

}
