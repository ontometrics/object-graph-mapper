package com.ontometrics.db.graph;

import java.lang.reflect.Field;

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

	public static void buildEntity(Node node, Object entity) {
		GraphDBEntityBuilder builder = new GraphDBEntityBuilder();
		builder.build(node, entity);
		// clean after build is done
		builder.entitiesMap = new HashMap<Node, Object>();
	}

	private void build(Node node, Object entity) {
		entitiesMap.put(node, entity);

		log.debug("looking for keys from node: {}", node.toString());
		for (String key : node.getPropertyKeys()) {
			log.info("evaluating key: {}", key);
			try {
				Field field = getField(entity.getClass(), key);
				field.setAccessible(true);
				log.debug("setting field: {} of type {}", field, field.getType());
				Object value = getFieldValue(node.getProperty(key), entity, field.getType());
				field.set(entity, value);
			} catch (Exception e) {
				log.error("error building entity: " + entity, e);
			}
		}
		Iterator<Relationship> iterator = node.getRelationships(Direction.OUTGOING).iterator();
		while (iterator.hasNext()) {
			Relationship relationship = iterator.next();
			log.info("evaluating relationship: {}", relationship.getType().name());
			try {
				Field field = entity.getClass().getDeclaredField(relationship.getType().name());
				field.setAccessible(true);
				log.debug("setting field: {} of type {}", field, field.getType());
				Class<?> fieldType = field.getType();
				if (Collection.class.isAssignableFrom(fieldType)) {
					buildCollectionEntry(entity, relationship, field, fieldType);
				} else if (Map.class.isAssignableFrom(fieldType)) {
					buildMapEntry(entity, relationship, field, fieldType);
				} else {

					Object value = null;
					if (entitiesMap.containsKey(relationship.getEndNode())) {
						value = entitiesMap.get(relationship.getEndNode());
					} else {
						value = newInstanceOfClass(fieldType);
						build(relationship.getEndNode(), value);
					}
					field.set(entity, value);

				}

			} catch (Exception e) {
				log.error("error building relationship for entity: " + entity, e);
			}
		}
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
	 */
	@SuppressWarnings("unchecked")
	private void buildMapEntry(Object entity, Relationship relationship, Field field, Class<?> fieldType)
			throws IllegalArgumentException, IllegalAccessException, InstantiationException {
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
				Node otherNode = entryNode.getSingleRelationship(DynamicRelationshipType.withName(key),
						Direction.OUTGOING).getEndNode();
				Object value = null;
				if (entitiesMap.containsKey(otherNode)) {
					value = entitiesMap.get(otherNode);
				} else {
					Class<?> type = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[i];
					value = newInstanceOfClass(type);
					build(otherNode, value);
				}
				values.put(key, value);
			}
		}
		map.put(values.get("key"), values.get("value"));
	}

	/**
	 * Build an entity corresponding to the relationship and add it to the
	 * collection property in the given entity
	 * 
	 * @param entity
	 * @param relationship
	 * @param field
	 * @param fieldType
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@SuppressWarnings("unchecked")
	private void buildCollectionEntry(Object entity, Relationship relationship, Field field, Class<?> fieldType)
			throws IllegalAccessException, InstantiationException {

		Collection<Object> collection = (Collection<Object>) field.get(entity);
		if (collection == null) {
			collection = (Collection<Object>) newInstanceOfCollection(fieldType);
			field.set(entity, collection);
		}
		fieldType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
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
	 * The method uses a converter if the type is not a primitive. Or convert
	 * array property to a collection before saving. Or return the property
	 * value, if the type is primitive.
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
