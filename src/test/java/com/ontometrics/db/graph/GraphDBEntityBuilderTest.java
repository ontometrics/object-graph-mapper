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
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

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
	public void setup(){
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
		addressNode.setProperty("city", city );
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
	public void buildEntityWithRelationships(){
		
		personNode.createRelationshipTo(addressNode, new RelationshipType() {
			
			public String name() {
				return "address";
			}
		});
		//check circular references
		addressNode.createRelationshipTo(personNode, new RelationshipType() {
			
			public String name() {
				return "owner";
			}
		});
		
		//reverse from node to entity
		Person person = new Person();
		GraphDBEntityBuilder.buildEntity(personNode, person);
		
		assertThat(person.getName(), is(username));
		assertThat(person.getAddress(), notNullValue());
		assertThat(person.getAddress().getName(), is(addressName));
		assertThat(person.getAddress().getCity(), is(city));
		assertThat(person.getAddress().getCountry(), is(country));

	}
	
	@Test
	public void buildEntityWithCollections(){
		personNode.createRelationshipTo(friend1Node, new RelationshipType() {
			
			public String name() {
				return "friends";
			}
		});

		personNode.createRelationshipTo(friend2Node, new RelationshipType() {
			
			public String name() {
				return "friends";
			}
		});

		friend1Node.createRelationshipTo(friend2Node, new RelationshipType() {
			
			public String name() {
				return "friends";
			}
		});

		Person person = new Person();
		GraphDBEntityBuilder.buildEntity(personNode, person);
		assertThat(person.getFriends(), notNullValue());
		assertThat(person.getFriends().size(), is(2));
		Set<String> names = new HashSet<String>();
		for(Person friend : person.getFriends()){
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
