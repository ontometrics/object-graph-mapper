package com.ontometrics.db.graph;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import com.ontometrics.db.graph.model.Address;
import com.ontometrics.db.graph.model.Person.Color;
import com.ontometrics.db.graph.model.Person;
import com.ontometrics.db.graph.model.AddressBook;
import com.ontometrics.db.graph.model.Employee;
import com.ontometrics.testing.TestGraphDatabase;

public class GraphDBEntityBuilderTest {

	protected File testDirectory = new File("target/var");

	/**
	 * Full path to the temporary database.
	 */
	protected File testDatabasePath = new File(testDirectory, "testdb");

	private String username = "codeslubber";

	private Date birthdate = new DateTime().minusYears(20).toDate();
	private Date hireDate = new DateTime().minusYears(3).toDate();
	@Rule
	public TemporaryFolder dbFolder = new TemporaryFolder();

	@Rule
	public TestGraphDatabase database = new TestGraphDatabase(dbFolder);

	private String addressName = "home";
	private String city = "LA";
	private String country = "US";
	private Node personNode, friend1Node, friend2Node;
	private Node addressNode;

	private String employeeDepartmentName = "Engineering";

	@Before
	public void setup() {
		personNode = database.getDatabase().createNode();
		personNode.setProperty("name", username);
		personNode.setProperty("birthDate", birthdate.getTime());

		friend1Node = database.getDatabase().createNode();
		friend1Node.setProperty("name", "Dru");
		friend1Node.setProperty("birthDate", birthdate.getTime());

		friend2Node = database.getDatabase().createNode();
		friend2Node.setProperty("name", "Jan");
		friend2Node.setProperty("birthDate", birthdate.getTime());

		addressNode = database.getDatabase().createNode();
		addressNode.setProperty("city", city);
		addressNode.setProperty("country", country);
		addressNode.setProperty("name", addressName);

	}

	@Test
	public void buildEntity() {

		Person person = new Person();

		GraphDBEntityBuilder.buildEntity(personNode, person);

		assertThat(person, is(not(nullValue())));
		assertThat(person.getName(), is(username));
		assertThat(person.getBirthDate(), is(birthdate));

	}

	@Test
	public void buildEntityWithRelationships() {

		personNode.createRelationshipTo(addressNode, DynamicRelationshipType.withName("address"));
		// check circular references
		addressNode.createRelationshipTo(personNode, DynamicRelationshipType.withName("owner"));

		// reverse from node to entity
		Person person = new Person();
		GraphDBEntityBuilder.buildEntity(personNode, person);

		assertThat(person.getName(), is(username));
		assertThat(person.getAddress(), notNullValue());
		assertThat(person.getAddress().getName(), is(addressName));
		assertThat(person.getAddress().getCity(), is(city));
		assertThat(person.getAddress().getCountry(), is(country));

	}

	@Test
	public void buildEntityWithCollections() {
		personNode.createRelationshipTo(friend1Node, DynamicRelationshipType.withName("friends"));
		personNode.createRelationshipTo(friend2Node, DynamicRelationshipType.withName("friends"));
		friend1Node.createRelationshipTo(friend2Node, DynamicRelationshipType.withName("friends"));

		Person person = new Person();
		GraphDBEntityBuilder.buildEntity(personNode, person);
		assertThat(person.getFriends(), notNullValue());
		assertThat(person.getFriends().size(), is(2));
		Set<String> names = new HashSet<String>();
		for (Person friend : person.getFriends()) {
			names.add(friend.getName());
		}
		assertThat(names, hasItem("Jan"));
		assertThat(names, hasItem("Dru"));

		Person friend1 = new Person();
		GraphDBEntityBuilder.buildEntity(friend1Node, friend1);
		assertThat(friend1.getFriends(), notNullValue());
		assertThat(friend1.getFriends().size(), is(1));
		assertThat(friend1.getFriends().iterator().next().getName(), is("Jan"));
	}

	@Test
	public void buildEntityWithMap() {

		Node mapEntry1 = database.getDatabase().createNode();
		mapEntry1.setProperty("value", "12345678");
		
		Node mapEntry1key = database.getDatabase().createNode();
		Relationship relationship = mapEntry1.createRelationshipTo(mapEntry1key, DynamicRelationshipType.withName("key"));
		relationship.setProperty(EntityManager.TYPE_PROPERTY, Person.class.getName());
		mapEntry1key.setProperty("name", "Rob");

		Node mapEntry2 = database.getDatabase().createNode();
		mapEntry2.setProperty("value", "012345678");
		
		Node mapEntry2key = database.getDatabase().createNode();
		relationship = mapEntry2.createRelationshipTo(mapEntry2key, DynamicRelationshipType.withName("key"));
		relationship.setProperty(EntityManager.TYPE_PROPERTY, Employee.class.getName());
		mapEntry2key.setProperty("name", "Joe");

		Node mapEntry3 = database.getDatabase().createNode();
		mapEntry3.setProperty("value", "43256666");
		mapEntry3.setProperty("_class", Person.class.getName());
		
		Node mapEntry3key = database.getDatabase().createNode();
		relationship = mapEntry3.createRelationshipTo(mapEntry3key, DynamicRelationshipType.withName("key"));
		relationship.setProperty(EntityManager.TYPE_PROPERTY, Person.class.getName());
		mapEntry3key.setProperty("name", "Ann");
		
		
		Node node = database.getDatabase().createNode();
		node.createRelationshipTo(mapEntry1, DynamicRelationshipType.withName("phones"));
		node.createRelationshipTo(mapEntry2, DynamicRelationshipType.withName("phones"));
		node.createRelationshipTo(mapEntry3, DynamicRelationshipType.withName("phones"));

		AddressBook addressBook = new AddressBook();
		GraphDBEntityBuilder.buildEntity(node, addressBook);
		assertThat(addressBook.getPhones(), notNullValue());
		assertThat(addressBook.getPhones().size(), is(3));
		assertThat(addressBook.getPhones().containsValue("12345678"), is(true));
		assertThat(addressBook.getPhones().containsValue("012345678"), is(true));
		assertThat(addressBook.getPhones().containsValue("43256666"), is(true));
		assertThat(addressBook.getPhones().keySet().contains(new Person("Rob")), is(true));
		assertThat(addressBook.getPhones().keySet().contains(new Employee("Joe")), is(true));
		assertThat(addressBook.getPhones().keySet().contains(new Person("Ann")), is(true));

	}
	
	@Test
	public void buildEntityWithSubclasses() {
		Node node = database.getDatabase().createNode();
		node.setProperty("name", "home");
		
		Node personNode = database.getDatabase().createNode();
		personNode.setProperty("name", "Ann");
		
		Relationship relationship = node.createRelationshipTo(personNode, DynamicRelationshipType.withName("owner"));
		relationship.setProperty(EntityManager.TYPE_PROPERTY, Person.class.getName());
		
		Address address = new Address();
		GraphDBEntityBuilder.buildEntity(node, address);
		assertThat(address.getName(), is("home"));
		assertThat(address.getOwner(), notNullValue());
		assertThat(address.getOwner().getName(), is("Ann"));
		
		node = database.getDatabase().createNode();
		node.setProperty("name", "Office");
		
		personNode = database.getDatabase().createNode();
		personNode.setProperty("name", "Joe");
		
		relationship = node.createRelationshipTo(personNode, DynamicRelationshipType.withName("owner"));
		relationship.setProperty(EntityManager.TYPE_PROPERTY, Employee.class.getName());
		
		address = new Address();
		GraphDBEntityBuilder.buildEntity(node, address);
		assertThat(address.getName(), is("Office"));
		assertThat(address.getOwner(), notNullValue());
		assertThat(address.getOwner().getName(), is("Joe"));
		
	}

	@Test
	public void buildEntityWithEnums(){
		Node enumNode = database.getDatabase().createNode();
		enumNode.setProperty("name", "Green");
		
		Relationship relationship = personNode.createRelationshipTo(enumNode, DynamicRelationshipType.withName("favoriteColor"));
		relationship.setProperty(EntityManager.TYPE_PROPERTY, Color.class.getName());

		Person person = new Person();
		GraphDBEntityBuilder.buildEntity(personNode, person);
		assertThat(person.getFavoriteColor(), is(Color.Green));
		
	}
	
	@Test
	public void superClassPropertiesAreDiscovered() {
		Node employeeNode = database.getDatabase().createNode();
		employeeNode.setProperty("name", username);
		employeeNode.setProperty("birthDate", birthdate.getTime());
		employeeNode.setProperty("hireDate", hireDate.getTime());
		employeeNode.setProperty("departmentName", employeeDepartmentName);

		Employee employee = new Employee();

		GraphDBEntityBuilder.buildEntity(employeeNode, employee);

		assertThat(employee, is(not(nullValue())));
		assertThat(employee.getName(), is(username));
		assertThat(employee.getBirthDate(), is(birthdate));
		assertThat(employee.getHireDate(), is(hireDate));
		assertThat(employee.getDepartmentName(), is(employeeDepartmentName));
	}
}
