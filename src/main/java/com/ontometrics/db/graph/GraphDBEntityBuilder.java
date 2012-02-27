package com.ontometrics.db.graph;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ontometrics.db.graph.conversion.TypeConverter;
import com.ontometrics.db.graph.conversion.TypeRegistry;
import com.ontometrics.utils.ArrayUtils;

public class GraphDBEntityBuilder {

	private static final Logger log = LoggerFactory.getLogger(GraphDBEntityBuilder.class);

	// the map is to hold created entities to use it to avoid circular
	// references
	private Map<Node, Object> entitiesMap = new HashMap<Node, Object>();
	private static Set<String> ignoredClasses = new HashSet<String>();
	static {
		ignoredClasses.add("ch.qos.logback.classic.Logger");
		ignoredClasses.add("org.slf4j.Logger");
	}
	public static void buildEntity(Node node, Object entity) {
		GraphDBEntityBuilder builder = new GraphDBEntityBuilder();
		builder.build(node, entity);
		// clean after build is done
		builder.entitiesMap = new HashMap<Node, Object>();
	}

	private void build(Node node, Object entity) {
		log.debug("building entity: {}", entity.getClass().getName());
		entitiesMap.put(node, entity);

		log.debug("looking for keys from node: {}", node.toString());
		for (String key : node.getPropertyKeys()) {
			log.info("evaluating key: {}", key);
			try {
				Field field = getField(entity.getClass(), key);
				if(isSettable(field)) {
					field.setAccessible(true);
					Object value = getFieldValue(node.getProperty(key), entity, field.getType());
					log.debug("setting field: {}, of type: {}, to value: {}", new Object[]{field, field.getType(), value});
					field.set(entity, value);
				}
			} catch (Exception e) {
				log.error("error building entity: " + entity, e);
			}
		}
		Iterator<Relationship> iterator = node.getRelationships(Direction.OUTGOING).iterator();
		while (iterator.hasNext()) {
			Relationship relationship = iterator.next();
			log.info("evaluating relationship: {}", relationship.getType().name());

			// need to see if the field is in a super class
			Class<?> entityClass = entity.getClass();
			while (entityClass != null && !Object.class.getName().equals(entityClass.getName())) {
				try {
					log.debug("EntityClass is {}", entityClass);
					log.debug("processing field: {}, for class: {}", relationship.getType().name(), entityClass.getName());
					Field field = entityClass.getDeclaredField(relationship.getType().name());
					field.setAccessible(true);
					log.debug("setting field: {} of type {}", field, field.getType());
					if(ignoredClasses(field.getType())) {
						continue;
					}
						
					Class<?> fieldType = field.getType();
					if (Collection.class.isAssignableFrom(fieldType)) {
						buildCollectionEntry(entity, relationship, field, fieldType);
					} else if (Map.class.isAssignableFrom(fieldType)) {
						buildMapEntry(entity, relationship, field, fieldType);
					} else if (fieldType.isEnum() || (fieldType.isInterface() && isEnum(relationship))) {
						buildEnumProperty(entity, relationship, field);
					} else {
						
						Object value = null;
						if (entitiesMap.containsKey(relationship.getEndNode())) {
							value = entitiesMap.get(relationship.getEndNode());
						} else {
							if (relationship.hasProperty(EntityManager.TYPE_PROPERTY)) {
								value = newInstanceOfClass(Class.forName((String) relationship
										.getProperty(EntityManager.TYPE_PROPERTY)));
							} else {
								value = newInstanceOfClass(fieldType);
							}
							build(relationship.getEndNode(), value);
						}
						field.set(entity, value);
					}
				} catch (NoSuchFieldException e) {
					log.info("error building relationship for entity: {}, try {}, {} ", new Object[]{entityClass, entityClass.getSuperclass(), e.getMessage()});
				} catch(Exception ex) {
					log.error("error building relationship for entity: " + entityClass + ", try  " + entityClass.getSuperclass(), ex);
				} finally {
					entityClass = entityClass.getSuperclass();
				}
			}

		}
	}
	
	/**
	 * @param relationship
	 * @return
	 */
	private boolean isEnum(Relationship relationship) {
		//enum can implements an interface, so checking the interface is an enum will not be enough
		
		if(relationship.hasProperty(EntityManager.TYPE_PROPERTY)){
			try {
				Class<?> _class = Class.forName((String) relationship.getProperty(EntityManager.TYPE_PROPERTY));
				return _class.isEnum();
			} catch (ClassNotFoundException e) {
				return false;
			}
		}
		return false;
	}

	/**
	 * set enum property in the given entity
	 * @param entity
	 * @param relationship
	 * @param field
	 * @param fieldType
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void buildEnumProperty(Object entity, Relationship relationship, Field field) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException {
		String enumClass = (String) relationship.getProperty(EntityManager.TYPE_PROPERTY);
		
		Class<? extends Enum> _class = (Class<? extends Enum>) Class.forName(enumClass);
		Object value = Enum.valueOf(_class, (String) relationship.getEndNode().getProperty("name"));
		field.set(entity, value);
	}

	private boolean isSettable(Field field) {
		int modifiers = field.getModifiers();
		if (Modifier.isStatic(modifiers)) {
			log.debug("field {} is stati, so not settting this field", field.getName());
			return false;
		}
		return true;
	}

	private boolean ignoredClasses(Class<?> entityClass) {
		boolean isIgnored = ignoredClasses.contains(entityClass.getName());
		if(isIgnored) {
			log.debug("ignoring field for processing of type: {}", entityClass);
		}
		return isIgnored;
	}

	/**
	 * Build the key/value pair and add it to the map
	 * 
	 * @param entity
	 * @param relationship
	 * @param field
	 * @param fieldType
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	private void buildMapEntry(Object entity, Relationship relationship, Field field, Class<?> fieldType)
			throws IllegalArgumentException, IllegalAccessException, InstantiationException, ClassNotFoundException {
		Map<Object, Object> map = (Map<Object, Object>) field.get(entity);
		if (map == null) {
			map = HashMap.class.newInstance();
			field.set(entity, map);
		}
		Node entryNode = relationship.getEndNode();
		String[] keys = new String[] { "key", "value" };// same order like
														// actualTypeArguments
														// (classes)
		Map<String, Object> values = new HashMap<String, Object>();
		for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
			if (entryNode.hasProperty(key)) {
				Object value = getFieldValue(entryNode.getProperty(key), entity, field.getType());
				values.put(key, value);
			}

			if (entryNode.hasRelationship(Direction.OUTGOING, DynamicRelationshipType.withName(key))) {
				Relationship keyRelationship = entryNode.getSingleRelationship(DynamicRelationshipType.withName(key),
						Direction.OUTGOING);
				Node otherNode = keyRelationship.getEndNode();
				Object value = null;
				if (entitiesMap.containsKey(otherNode)) {
					value = entitiesMap.get(otherNode);
				} else {
					if (keyRelationship.hasProperty(EntityManager.TYPE_PROPERTY)) {
						value = newInstanceOfClass(Class.forName((String) keyRelationship
								.getProperty(EntityManager.TYPE_PROPERTY)));
					} else {
						Class<?> type = (Class<?>) ((ParameterizedType) field.getGenericType())
								.getActualTypeArguments()[i];
						value = newInstanceOfClass(type);
					}

					build(otherNode, value);
				}
				values.put(key, value);
			}
		}
		map.put(values.get("key"), values.get("value"));
	}

	/**
	 * Build an entity corresponding to the relationship and add it to the collection property in the given entity
	 * 
	 * @param entity
	 * @param relationship
	 * @param field
	 * @param fieldType
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	private void buildCollectionEntry(Object entity, Relationship relationship, Field field, Class<?> fieldType)
			throws IllegalAccessException, InstantiationException, ClassNotFoundException {

		Collection<Object> collection = (Collection<Object>) field.get(entity);
		if (collection == null) {
			collection = (Collection<Object>) newInstanceOfCollection(fieldType);
			field.set(entity, collection);
		}
		if (relationship.hasProperty(EntityManager.TYPE_PROPERTY)) {
			fieldType = Class.forName((String) relationship.getProperty(EntityManager.TYPE_PROPERTY));
		} else {
			fieldType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
		}

		Object value = null;
		if (entitiesMap.containsKey(relationship.getEndNode())) {
			value = entitiesMap.get(relationship.getEndNode());
		} else {
			value = newInstanceOfClass(fieldType);
			build(relationship.getEndNode(), value);
		}
		collection.add(value);
	}

	@SuppressWarnings("rawtypes")
	private static Field getField(Class clazz, String fieldName) throws NoSuchFieldException {
		try {
			return clazz.getDeclaredField(fieldName);
		} catch (NoSuchFieldException e) {
			Class superClass = clazz.getSuperclass();
			if (superClass == null) {
				throw e;
			} else {
				return getField(superClass, fieldName);
			}
		}
	}

	/**
	 * Returns an instance of a collection based on the given type
	 * 
	 * @param type
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private static Object newInstanceOfCollection(Class<?> type) throws InstantiationException, IllegalAccessException {
		if (Set.class.isAssignableFrom(type)) {
			return HashSet.class.newInstance();
		}
		if (List.class.isAssignableFrom(type)) {
			return ArrayList.class.newInstance();
		}
		// TODO implement more collections
		return null;
	}

	/**
	 * Create new instance of the given class.
	 * 
	 * @param type
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private static Object newInstanceOfClass(Class<?> type) throws InstantiationException, IllegalAccessException {
		if (type.isInterface()) {
			throw new InstantiationException("Type " + type + " is interface");
		}
		return type.newInstance();
	}

	/**
	 * Get the field's value corresponding to given property.
	 * 
	 * The method uses a converter if the type is not a primitive. Or convert array property to a collection before
	 * saving. Or return the property value, if the type is primitive.
	 * 
	 * @param property
	 * @param entity
	 * @param type
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@SuppressWarnings("unchecked")
	private static Object getFieldValue(Object property, Object entity, Class<?> type) throws IllegalArgumentException,
			IllegalAccessException, InstantiationException {
		TypeConverter converter = TypeRegistry.getConverter(type);
		if (converter != null) {
			log.debug("get property of type {} using converter {}", new Object[] { type, converter.getClass() });
			return converter.convertFromPrimitive(property);
		}

		if (property.getClass().isArray()) {
			log.debug("get property of type array");
			Collection<Object> collection = (Collection<Object>) newInstanceOfCollection(type);
			collection.addAll(ArrayUtils.toCollection(property));
			return collection;
		}

		// if doesn't have a converter then it is primitive
		return property;
	}
}
