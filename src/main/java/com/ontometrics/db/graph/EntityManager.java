package com.ontometrics.db.graph;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

import javax.persistence.Id;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ontometrics.db.graph.conversion.TypeConverter;
import com.ontometrics.db.graph.conversion.TypeRegistry;

/**
 * Means of getting access to a specific database.
 * 
 * @author Rob
 * 
 */
public class EntityManager {

	private static final Logger log = LoggerFactory.getLogger(EntityManager.class);

	public static final String PRIMARY_KEY = "PrimarykeyIndex";

	private static Node referenceNode;

	/**
	 * The database we are using through this manager.
	 */
	private EmbeddedGraphDatabase database;

	/**
	 * Passed in on creation. This just wraps itself around the database and offers basic help with CRUD support for
	 * corresponding repositories.
	 * 
	 * @param database
	 *            the database we will be using in this session
	 */
	public EntityManager(EmbeddedGraphDatabase database) {
		this.database = database;
	}

	/**
	 * Provides access to the database we are using.
	 * 
	 * @return the valid, open database
	 */
	public EmbeddedGraphDatabase getDatabase() {
		return database;
	}

	@SuppressWarnings("rawtypes")
	public Node create(Object entity) {
		Transaction transaction = database.beginTx();
		try {
			Node node = database.createNode();
			Class clazz = entity.getClass();
			while (clazz != null && !clazz.getName().equals(Object.class.getName())) {
				log.debug("processing class: {}", clazz);
				for (Field field : clazz.getDeclaredFields()) {
					try {
						field.setAccessible(true);
						if (!Modifier.isTransient(field.getModifiers())) {
							setProperty(node, field.getName(), field.get(entity));
							if (isThePrimaryKey(field)) {
								getNodeIndex(entity).add(node, PRIMARY_KEY, field.get(entity));
							}
						}
					} catch (Exception e) {
						log.error("error creating node for entity: " + entity, e);
					}
				}
				clazz = clazz.getSuperclass();
			}
			transaction.success();
			return node;
		} finally {
			transaction.finish();
		}
	}

	/**
	 * @param entity
	 * @return the database index for the given entity's class
	 */
	public Index<Node> getNodeIndex(Object entity) {
		return database.index().forNodes(entity.getClass().getName());
	}

	/**
	 * Set properties or relationships for the given node based on the value type. if the value is a primitive, it will
	 * creates a property for it. if the value is not primitive but has a converter, it will use the converter to create
	 * the property. if the value is not primitive and doesn't have a converter it will create a relationship. if the
	 * value is a collection, it will create a relationship for all its elements.
	 * 
	 * @param node
	 * @param name
	 * @param value
	 */
	private void setProperty(Node node, final String name, Object value) {
		if (value == null) {
			removeValueIfExists(node, name);
			return; // we are not setting null values
		}
		if (isPrimitiveType(value)) {
			log.debug("set property with name '{}' and primitive type '{}'", name, value.getClass());
			node.setProperty(name, value);
			return;
		}
		TypeConverter converter = TypeRegistry.getConverter(value.getClass());
		if (converter != null) {
			log.debug("set property with name '{}' and type '{}' using converter '{}'",
					new Object[] { name, value.getClass(), converter.getClass() });
			Object convertedValue = converter.convertToPrimitive(value);
			node.setProperty(name, convertedValue);
			return;
		}
		if (Collection.class.isAssignableFrom(value.getClass())) {
			@SuppressWarnings("unchecked")
			Collection<Object> collection = (Collection<Object>) value;
			for (Object object : collection) {
				createRelationship(node, name, object);
			}
		} else {
			createRelationship(node, name, value);
		}

	}

	public Node update(Object entity, Node existingNode) {
		Transaction transaction = database.beginTx();
		for (Field field : entity.getClass().getDeclaredFields()) {
			try {
				field.setAccessible(true);
				if (!Modifier.isTransient(field.getModifiers())) {
					setProperty(existingNode, field.getName(), field.get(entity));
				}
				transaction.success();
			} catch (Exception e) {
				log.error("error updating node for entity: " + entity, e);
			} finally {
				transaction.finish();
			}
		}
		return existingNode;
	}

	private static void removeValueIfExists(Node node, final String name) {
		if (node.hasProperty(name)) {
			node.removeProperty(name);
		} else {
			Relationship relationship = node.getSingleRelationship(new RelationshipType() {

				public String name() {
					return name;
				}
			}, Direction.OUTGOING);
			if (relationship != null) {
				relationship.delete();
			}
		}

	}

	/**
	 * Create relationship from the given node, with a type has the given name. The end node will be the node for the
	 * given value, if a node exists it will use it, if not it will create new node.
	 * 
	 * @param node
	 * @param name
	 * @param value
	 */
	private void createRelationship(Node node, final String name, Object value) {
		log.debug("Create a relationship and node for {}", name);
		Node toNode = existingNodeFor(value);
		if (toNode == null) {
			toNode = create(value);
		} else {
			log.debug("found existing node for the relationship '{}'", name);
		}
		node.createRelationshipTo(toNode, new RelationshipType() {
			public String name() {
				return name;
			}
		});

	}

	/**
	 * Returns existing node for given entity, it will get the primary key of the entity and then load the entity from
	 * the index using it.
	 * 
	 * If there is no primary key, or the index doesn't have the found primary key, method will return null
	 * 
	 * @param entity
	 * @return
	 */
	private Node existingNodeFor(Object entity) {
		Object primaryKey = null;
		for (Field field : entity.getClass().getDeclaredFields()) {
			try {
				if (isThePrimaryKey(field)) {
					field.setAccessible(true);
					primaryKey = field.get(entity);
					break;
				}
			} catch (Exception e) {
				log.error("error creating node for entity: " + entity, e);
			}
		}
		if (primaryKey != null) {
			return getNodeIndex(entity).get(PRIMARY_KEY, primaryKey).getSingle();
		}
		return null;
	}

	/**
	 * 
	 * @param value
	 * @return true for types supported by the new4j Property
	 */
	private static boolean isPrimitiveType(Object value) {
		return (value instanceof String || value instanceof Integer || value instanceof Boolean
				|| value instanceof Float || value instanceof Long || value instanceof Double || value instanceof Byte
				|| value instanceof Character || value instanceof Short);
	}

	private static boolean isThePrimaryKey(Field field) {
		return field.isAnnotationPresent(Id.class);
	}

	public void setDatabase(EmbeddedGraphDatabase database) {
		this.database = database;
	}

	public static Node getReferenceNode() {
		return referenceNode;
	}

	/**
	 * Create reference node with the given type
	 * 
	 * @param type
	 */
	public void createReferenceNodeOfType(RelationshipType type) {
		referenceNode = database.createNode();
		database.getReferenceNode().createRelationshipTo(referenceNode, type);
	}

	/**
	 * create relationship between given nodes with the given type
	 * 
	 * @param fromNode
	 * @param toNode
	 * @param type
	 */
	public void createRelationship(Node fromNode, Node toNode, RelationshipType type) {
		fromNode.createRelationshipTo(toNode, type);
	}

}