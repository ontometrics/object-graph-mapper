package com.ontometrics.db.graph;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.ReadableRelationshipIndex;

import com.ontometrics.db.graph.model.Employee;
import com.ontometrics.testing.TestGraphDatabase;

public class EntityManagerTest {
	
	private static enum RelTypes implements RelationshipType
	{
	    PERSONS_REFERENCE,
	    PERSON
	}
	
	@Rule
	public TemporaryFolder dbFolder = new TemporaryFolder();
	
	@Rule
	public TestGraphDatabase database = new TestGraphDatabase(dbFolder);

	private String username = "codeslubber";

	private Date birthdate = new DateTime().minusYears(20).toDate();

	private Date hireDate = new DateTime().minusYears(3).toDate();
	private String addressName = "home";
	
	private String city = "LA";
	
	private String country = "US";
	

	private RelationshipType addressType, parentType, ownerType, friendsType;

	private Person person;
	private Employee employee;
	private String employeeDepartmentName = "Engineering";
	
	private EntityManager entityManager;
	
	@Before
	public void setup(){
		entityManager = new EntityManager(database.getDatabase());
		
		person = new Person(username, birthdate);
		employee = new Employee("neo4j", birthdate, employeeDepartmentName, hireDate);
		addressType = new RelationshipType() {
			public String name() {
				return "address";
			}
		};

		parentType = new RelationshipType() {
			public String name() {
				return "parent";
			}
		};

		ownerType = new RelationshipType() {
			public String name() {
				return "owner";
			}
		};

		friendsType = new RelationshipType() {
			public String name() {
				return "friends";
			}
		};

	}
	
	@Test
	public void createAndIndexNodeFromEntity(){
		Node personNode = entityManager.create(person);

		assertThat((String)personNode.getProperty("name"), is(username));
		
		Node indexedNode = entityManager.getNodeIndex(Person.class).get(EntityManager.PRIMARY_KEY, username).getSingle();
		assertThat(indexedNode, is(personNode));
		
		Node notIndexedPerson = entityManager.getNodeIndex(Person.class).get(EntityManager.PRIMARY_KEY, "xxx").getSingle();
		assertThat(notIndexedPerson, nullValue());

	}

	@Test
	public void indexingProperties(){
		Node personNode = entityManager.create(person);

		Node indexedNode = entityManager.getNodeIndex(Person.class).get("birthDate", birthdate).getSingle();
		assertThat(indexedNode, is(personNode));
		
		person.setBirthDate(null);
		
		entityManager.update(person, personNode);
		indexedNode = entityManager.getNodeIndex(Person.class).get("birthDate", birthdate).getSingle();
		assertThat(indexedNode, nullValue());

	}

	@Test
	public void indexingRelationships(){
		Person parent = new Person("williams", new DateTime().minusYears(50).toDate());
		person.setParent(parent);
		
		Node personNode = entityManager.create(person);

		Relationship parentRelationShip = personNode.getSingleRelationship(parentType, Direction.OUTGOING);

		ReadableRelationshipIndex parentsIndex = (ReadableRelationshipIndex) entityManager.getRelationshipIndex(Person.class, "parent"); 
		IndexHits<Relationship> relationships = parentsIndex.get( "child", username, personNode, null );
		assertThat(relationships.hasNext(), is(true));
		assertThat(relationships.next(), is(parentRelationShip));
		
		
		person.setParent(null);
		entityManager.update(person, personNode);		
		relationships = parentsIndex.get( "child", username, personNode, null );
		assertThat(relationships.hasNext(), is(false));
		
	}

	@Test
	public void createEntityWithRelationships(){
		Person parent = new Person("williams", new DateTime().minusYears(50).toDate());
		
		person.setAddress(new Address(addressName, city, country));
		person.setParent(parent);
		
		Node parentNode = entityManager.create(parent);
		Node personNode = entityManager.create(person);

		Relationship relationShip = personNode.getSingleRelationship(addressType, Direction.OUTGOING);
		assertThat(relationShip, notNullValue());		
		Node addressNode = relationShip.getEndNode();
		assertThat((String)addressNode.getProperty("name"), is(addressName));
		assertThat((String)addressNode.getProperty("city"), is(city));
		assertThat((String)addressNode.getProperty("country"), is(country));
		
		Relationship ownerRelationShip = addressNode.getSingleRelationship(ownerType, Direction.OUTGOING);
		assertThat(ownerRelationShip, notNullValue());
		assertThat(ownerRelationShip.getEndNode(), is(personNode));
		
		Relationship parentRelationShip = personNode.getSingleRelationship(parentType, Direction.OUTGOING);
		assertThat(parentRelationShip, notNullValue());
		assertThat(parentRelationShip.getEndNode(), is(parentNode));
		
	}

	@Test
	public void createEntityWithCollections(){
		Person friend1 = new Person("Jan", new DateTime().minusYears(30).toDate());
		Person friend2 = new Person("Dru", new DateTime().minusYears(20).toDate());
		person.getFriends().add(friend1);
		person.getFriends().add(friend2);		
		friend1.getFriends().add(friend2);
		
		Node friend2Node = entityManager.create(friend2);
		Node personNode = entityManager.create(person);
		Iterator<Relationship> iterator = personNode.getRelationships(Direction.OUTGOING, friendsType).iterator();
		assertThat(iterator.hasNext(), is(true));
		List<Node> friendsNodes = new ArrayList<Node>(); 
		while(iterator.hasNext()){
			friendsNodes.add(iterator.next().getEndNode());
		}
		assertThat(friendsNodes.size(), is(2));
		assertThat(friendsNodes, hasItem(friend2Node));
		
	}

	@Test
	public void updateNullValueWilRemoveIt(){
		person.setAddress(new Address(addressName, city, country));
		Node personNode = entityManager.create(person);
		Relationship relationShip = personNode.getSingleRelationship(addressType, Direction.OUTGOING);
		assertThat(relationShip, notNullValue());	
		
		Node addressNode = relationShip.getEndNode();
		relationShip = addressNode.getSingleRelationship(ownerType, Direction.OUTGOING);
		assertThat(relationShip, notNullValue());	
		
		person.getAddress().setOwner(null);
		entityManager.update(person.getAddress(), addressNode);
		
		relationShip = addressNode.getSingleRelationship(ownerType, Direction.OUTGOING);
		assertThat(relationShip, nullValue());
		
		person.setAddress(null);
		entityManager.update(person, personNode);
		
		relationShip = personNode.getSingleRelationship(addressType, Direction.OUTGOING);
		assertThat(relationShip, nullValue());
	}

	@Test
	public void referenceNode(){
		entityManager.createReferenceNodeOfType(RelTypes.PERSONS_REFERENCE);
		
		Node personNode = entityManager.create(person);
		
		entityManager.createRelationship(EntityManager.getReferenceNode(), personNode, RelTypes.PERSON);
		
		Relationship relationship = personNode.getSingleRelationship(RelTypes.PERSON, Direction.INCOMING);
		assertThat(relationship, notNullValue());
		assertThat(relationship.getOtherNode(personNode), is(EntityManager.getReferenceNode()));
	}
	
	
	@Test(expected=NotFoundException.class)
	public void transientNodesShouldNotBePersisted(){
		Node personNode = entityManager.create(person);

		String transientPropertyName = "visitDuration";
		personNode.getProperty(transientPropertyName);
	}
	
	@Test
	public void superClassPropertiesArePersisted() {
		Node node = entityManager.create(employee);
		assertThat((Long)node.getProperty("hireDate"), is(hireDate.getTime()));
		assertThat((String)node.getProperty("departmentName"), is(employeeDepartmentName));
		assertThat((String)node.getProperty("name"), is("neo4j"));
		
	}

}
