package com.ontometrics.db.graph;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.persistence.Transient;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
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
import com.ontometrics.utils.ArrayUtils;

/**
 * Means of getting access to a specific database.
 * 
 * @author Rob
 * 
 */
public class EntityManager {

	public static final String TYPE_PROPERTY = "_class";

	private static final Logger log = LoggerFactory.getLogger(EntityManager.class);

	public static final String PRIMARY_KEY = "PrimarykeyIndex";

	private static Node referenceNode;

	private static Set<String> coreTypes;

	/**
	 * The database we are using through this manager.
	 */
	private EmbeddedGraphDatabase database;

	/**
	 * Passed in on creation. This just wraps itself around the database and
	 * offers basic help with CRUD support for corresponding repositories.
	 * 
	 * @param database
	 *            the database we will be using in this session
	 */
	public EntityManager(EmbeddedGraphDatabase database) {
		this.database = database;
		coreTypes = new HashSet<String>();
		coreTypes.add(Object.class.getName());
		coreTypes.add(Double.class.getName());
		coreTypes.add(Long.class.getName());
		coreTypes.add(Integer.class.getName());
	}

	/**
	 * Provides access to the database we are using.
	 * 
	 * @return the valid, open database
	 */
	public EmbeddedGraphDatabase getDatabase() {
		return database;
	}

	/**
	 * Provides a means of persisting any object. Will use reflection to find
	 * the properties that should be persisted, will then write them to the
	 * corresponding node in the graph.
	 * 
	 * @param entity
	 *            the populated entity that the caller would like to put in the
	 *            database
	 * @return the node that was built
	 */
	public Node create(Object entity) {
		Transaction transaction = database.beginTx();
		try {
			Node node = database.createNode();
			Class<?> clazz = entity.getClass();
			while (clazz != null && !isCoreType(clazz)) {
				log.debug("processing class: {}", clazz);
				for (Field field : clazz.getDeclaredFields()) {
					log.debug("processing field: {}", field.getName());
					field.setAccessible(true);
					if (!isTransient(field) && !isLogger(field)) {
						Object value = getFieldValue(entity, field);
						if (value == null) {
							if (isThePrimaryKey(field)) {
								if(field.isAnnotationPresent(GeneratedId.class)){
									value = assignId(entity, field, node.getId());
								}else{
									throw new IllegalArgumentException("Primary key cannot be null, field: " + field.getName());
								}
							} else {
								continue;
							}
						}
						setProperty(node, field.getName(), value);
						//index the entity using the key/value in the index annotation
						if (field.isAnnotationPresent(com.ontometrics.db.graph.Index.class)) {
							String indexKey = field.getAnnotation(com.ontometrics.db.graph.Index.class).key();
							String indexValueName = field.getAnnotation(com.ontometrics.db.graph.Index.class).value();
							Object indexValue = null;
							if (indexKey.equals("n/a")) {
								indexKey = field.getName();
							}
							if (!indexValueName.equals("n/a")) {
								indexValue = getFieldValue(entity, getFieldWithName(entity, indexValueName));
							}
							addIndex(entity.getClass(), node, field.getName(), value, indexKey, indexValue);
						}
						if (isThePrimaryKey(field)) {
							getNodeIndex(entity.getClass()).add(node, PRIMARY_KEY, value);
						}
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

	private Object assignId(Object entity, Field field, long id) {
		if(id == 0){
			throw new IllegalArgumentException("cannot assign id w/ value zero");
		}
		try {
			field.set(entity, Long.valueOf(id));
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("cannot assign id: "+ e.getMessage());
		}
		
		return Long.valueOf(id);
	}

	private boolean isLogger(Field field) {
		if(field.getType().getName().contains("Logger")) {
			log.debug("Ignoring Logger field");
			return true;
		}
		return false;
	}

	/**
	 * Provides access to the index for an entity.
	 * 
	 * @param entity
	 * @param name
	 * @return field with the given name in the given entity
	 */
	private Field getFieldWithName(Object entity, String name) {
		Class<?> clazz = entity.getClass();
		while (clazz != null && !isCoreType(clazz)) {
			log.debug("processing class: {}", clazz);
			Field field = null;
			try {
				field = clazz.getDeclaredField(name);
			} catch (SecurityException e) {
			} catch (NoSuchFieldException e) {
			}
			if (field != null) {
				return field;
			}
			clazz = clazz.getSuperclass();
		}
		return null;
	}

	/**
	 * return the value of the given field in the given entity
	 * 
	 * @param entity
	 * @param field
	 * @return
	 */
	private Object getFieldValue(Object entity, Field field) {
		Object value = null;
		try {
			field.setAccessible(true);
			value = field.get(entity);
		} catch (IllegalArgumentException e) {
			log.error("Error getting the field value from " + entity, e);
		} catch (IllegalAccessException e) {
			log.error("Error getting the field value from " + entity, e);
		}
		return value;
	}

	/**
	 * @param entity
	 * @return the database index for the given entity's class
	 */
	public Index<Node> getNodeIndex(Class<?> aClass) {
		Class<?> superClass = aClass;
		while (superClass != null && !superClass.getName().equals(Object.class.getName())) {
			superClass = superClass.getSuperclass();
		}
		return database.index().forNodes(superClass.getName());
	}

	public Index<Relationship> getRelationshipIndex(Class<?> aClass, String name) {
		Class<?> superClass = aClass;
		while (superClass != null && !superClass.getName().equals(Object.class.getName())) {
			superClass = superClass.getSuperclass();
		}
		return database.index().forRelationships(superClass.getName() + "." + name);
	}

	/**
	 * Set properties or relationships for the given node based on the value
	 * type. if the value is a primitive, it will creates a property for it. if
	 * the value is not primitive but has a converter, it will use the converter
	 * to create the property. if the value is not primitive and doesn't have a
	 * converter it will create a relationship. if the value is a collection, it
	 * will create a relationship for all its elements.
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
		
		deleteRelationships(node, DynamicRelationshipType.withName(name), Direction.OUTGOING);
		
		if(Map.class.isAssignableFrom(value.getClass())){
			@SuppressWarnings("unchecked")
			Map<Object, Object> map = (Map<Object, Object>) value;
			handleMapProperty(node, name, map);
			return;
		}
		
		if(value.getClass().isEnum()){
			handleEnumProperty(node, name, (Enum<?>) value);
			return;
		}
		
		if (Collection.class.isAssignableFrom(value.getClass())) {
			@SuppressWarnings("unchecked")
			Collection<Object> collection = (Collection<Object>) value;
			if(isCollectionOfPrimitives(collection) && !collection.isEmpty()){
				node.setProperty(name, ArrayUtils.toPrimitives(collection));
			}else{
				for (Object object : collection) {
					createRelationship(node, name, object);
				}
			}
		} else {
			createRelationship(node, name, value);
		}

	}

	/**
	 * delete all relationships for the given type and direction
	 * 
	 * @param node
	 * @param withName
	 * @param outgoing
	 */
	private void deleteRelationships(Node node, RelationshipType type, Direction direction) {
		if(!node.hasRelationship(type, direction)) return;
		Iterator<Relationship> iterator = node.getRelationships(type, direction).iterator();
		while(iterator.hasNext()){
			Relationship relationship = iterator.next();
			relationship.delete();
		}
	}

	/**
	 * save the Enum property as a relationship with a property to hold the enum class, and the end node has a name property
	 * @param node
	 * @param name
	 * @param value
	 */
	private void handleEnumProperty(Node node, String name, Enum<?> value) {
		Node enumNode = getNodeIndex(value.getClass()).get(PRIMARY_KEY, value.name()).getSingle();
		if (enumNode == null) {
			enumNode = database.createNode();
			setProperty(enumNode, "name", value.name());
			setProperty(enumNode, "ordinal", value.ordinal());
			getNodeIndex(value.getClass()).add(enumNode, PRIMARY_KEY, value.name());
		}
		Relationship relationship = node.createRelationshipTo(enumNode, DynamicRelationshipType.withName(name));
		relationship.setProperty(TYPE_PROPERTY, value.getClass().getName());
	}

	/**
	 * Handle saving a map property, it will create a node for each key/value pair, and make a relationship to it.
	 * key and value will be properties in the new node, should be handled like other properties using setProperty
	 * 
	 * @param node
	 * @param name
	 * @param value
	 */
	private void handleMapProperty(Node node, String name, Map<Object, Object> map) {
		log.debug("handle a map property {}", name);
		for(Object key : map.keySet()){
			Node entryNode = database.createNode();
			setProperty(entryNode, "key", key);
			setProperty(entryNode, "value", map.get(key));
			Relationship relationship = node.createRelationshipTo(entryNode, DynamicRelationshipType.withName(name));
			relationship.setProperty(TYPE_PROPERTY, map.get(key).getClass().getName());
		}
	}

	/**
	 * 
	 * @param collection
	 * @return true if all objects in the collection are of primitive types
	 */
	private boolean isCollectionOfPrimitives(Collection<Object> collection) {
		for(Object object : collection){
			if(!isPrimitiveType(object)){
				return false;
			}
		}
		return true;
	}

	/**
	 * index the given property in the given class
	 * 
	 * @param aClass
	 * @param node
	 * @param name
	 * @param value
	 * @param indexValue
	 * @param indexKey
	 */
	private void addIndex(Class<?> aClass, Node node, String name, Object value, String indexKey, Object indexValue) {
		if (node.hasProperty(name)) {
			log.debug("update index for property with name '{}' and primitive type '{}'", name, value.getClass());
			getNodeIndex(aClass).add(node, indexKey, value);
		} else {

			Iterator<Relationship> iterator = node.getRelationships(DynamicRelationshipType.withName(name)).iterator();
			while (iterator.hasNext()) {
				getRelationshipIndex(aClass, name).add(iterator.next(), indexKey, indexValue);
			}
		}
		return;
	}

	/**
	 * update the index value
	 * 
	 * @param aClass
	 * @param node
	 * @param name
	 * @param value
	 */
	private void updateIndex(Class<?> aClass, Node node, String name, Object value) {
		if (node.hasProperty(name)) {
			getNodeIndex(aClass).remove(node, name);
			if (value != null) {
				getNodeIndex(aClass).add(node, name, value);
			}
		} else {
			if (value == null) {
				// TODO check back for collections
				Iterator<Relationship> iterator = node.getRelationships(DynamicRelationshipType.withName(name))
						.iterator();
				while (iterator.hasNext()) {
					getRelationshipIndex(aClass, name).remove(iterator.next());
				}
			}
		}
	}

	/**
	 * Provides means of updating an entity whose properties have changed.
	 * 
	 * @param entity
	 *            the new version of the entity
	 * @param existingNode
	 *            the node in the db where the current values are held
	 * @return the updated node
	 */
	public Node update(Object entity, Node existingNode) {
		Transaction transaction = database.beginTx();
		for (Field field : entity.getClass().getDeclaredFields()) {
			try {
				field.setAccessible(true);
				Object value = field.get(entity);
				if (field.isAnnotationPresent(com.ontometrics.db.graph.Index.class)) {
					updateIndex(entity.getClass(), existingNode, field.getName(), value);
				}

				if (!Modifier.isTransient(field.getModifiers())) {
					setProperty(existingNode, field.getName(), value);
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

	private void removeValueIfExists(Node node, final String name) {
		if (node.hasProperty(name)) {
			node.removeProperty(name);
		} else {
			deleteRelationships(node, DynamicRelationshipType.withName(name), Direction.OUTGOING);
		}

	}

	/**
	 * Create relationship from the given node, with a type has the given name.
	 * The end node will be the node for the given value, if a node exists it
	 * will use it, if not it will create new node.
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
			//check if relationship already exists
			if(existingRelationship(node, toNode, DynamicRelationshipType.withName(name)) != null) return;
		}
		Relationship relationship = node.createRelationshipTo(toNode, DynamicRelationshipType.withName(name));
		relationship.setProperty(TYPE_PROPERTY, value.getClass().getName());

	}

	private Relationship existingRelationship(Node fromNode, Node toNode, RelationshipType type) {
		if(fromNode.hasRelationship(type, Direction.OUTGOING)){
			Iterator<Relationship> iterator = fromNode.getRelationships(type, Direction.OUTGOING).iterator();
			while(iterator.hasNext()){
				Relationship relationship = iterator.next();
				if(relationship.getEndNode().getId() == toNode.getId()){
					log.debug("relationship already exists");
					return relationship;
				}
			}
		}
		return null;
	}

	/**
	 * Returns existing node for given entity, it will get the primary key of
	 * the entity and then load the entity from the index using it.
	 * 
	 * If there is no primary key, or the index doesn't have the found primary
	 * key, method will return null
	 * 
	 * @param entity
	 * @return
	 */
	private Node existingNodeFor(Object entity) {
		Object primaryKey = null;
		Class<?> clazz = entity.getClass();
		while (clazz != null && !isCoreType(clazz)) {
			for (Field field : clazz.getDeclaredFields()) {
				try {
					if (isThePrimaryKey(field)) {
						field.setAccessible(true);
						primaryKey = field.get(entity);
						if (primaryKey != null) {
							return getNodeIndex(entity.getClass()).get(PRIMARY_KEY, primaryKey).getSingle();
						}
					}
				} catch (Exception e) {
					log.error("error creating node for entity: " + entity, e);
				}
			}
			clazz = clazz.getSuperclass();
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
		return field.isAnnotationPresent(javax.persistence.Id.class) || field.isAnnotationPresent(Id.class);
	}

	public void setDatabase(EmbeddedGraphDatabase database) {
		this.database = database;
	}

	public static Node getReferenceNode() {
		return referenceNode;
	}

	/**
	 * Provides means of making reference nodes, which are starting points into
	 * areas of the graph.
	 * 
	 * @param type
	 *            the type for which we would have a new starting point in the
	 *            database
	 * @see <a href="http://wiki.neo4j.org/content/Design_Guide">neo4j Design
	 *      Doc</a>
	 */
	public void createReferenceNodeOfType(RelationshipType type) {
		referenceNode = database.createNode();
		database.getReferenceNode().createRelationshipTo(referenceNode, type);
	}

	/**
	 * Provides means of creating associations between nodes in the database.
	 * 
	 * @param fromNode
	 *            where the relationship originates
	 * @param toNode
	 *            where the relation points to
	 * @param type
	 *            the type of relationship between these nodes, e.g. Parent or
	 *            Uses, etc.
	 */
	public void createRelationship(Node fromNode, Node toNode, RelationshipType type) {
		fromNode.createRelationshipTo(toNode, type);
	}

	private boolean isCoreType(Class<?> clazz) {
		return coreTypes.contains(clazz.getName());
	}

	private boolean isTransient(Field field) {
		boolean isTransient = Modifier.isTransient(field.getModifiers()) || field.isAnnotationPresent(Transient.class);
		return isTransient;
	}

}
