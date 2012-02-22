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

	@SuppressWarnings("unchecked")
	private void build(Node node, Object entity) {
		entitiesMap.put(node, entity);

		log.debug("looking for keys from node: {}", node.toString());
		for (String key : node.getPropertyKeys()) {
			log.info("evaluating key: {}", key);
			try {
				Field field = getField(entity.getClass(), key);
				field.setAccessible(true);
				log.debug("setting field: {} of type {}", field, field.getType());
				setFieldValue(node.getProperty(key), entity, field);
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
				Collection<Object> collection = null;
				Class<?> fieldType = field.getType();
				if (Collection.class.isAssignableFrom(fieldType)) {
					collection = (Collection<Object>) field.get(entity);
					if (collection == null) {
						collection = (Collection<Object>) newInstanceOfCollection(fieldType);
						field.set(entity, collection);
					}
					fieldType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
				}
				Object value = null;
				if (entitiesMap.containsKey(relationship.getEndNode())) {
					value = entitiesMap.get(relationship.getEndNode());
				} else {
					value = newInstanceOfClass(fieldType);
					build(relationship.getEndNode(), value);
				}
				if (collection != null) {
					collection.add(value);
				} else {
					field.set(entity, value);
				}
			} catch (Exception e) {
				log.error("error building relationship for entity: " + entity, e);
			}
		}
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
	 * Set the field with the property value. 
	 * The method uses a converter if the type is not a primitive.
	 * Or convert array property to a collection before saving.
	 * Or just set the field, if the type is primitive.
	 * 
	 * @param node
	 * @param key
	 * @param type
	 * @return
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws InstantiationException 
	 */
	@SuppressWarnings("unchecked")
	private static void setFieldValue(Object property, Object entity, Field field) throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		Class<?> type = field.getType();
		TypeConverter converter = TypeRegistry.getConverter(type);
		if (converter != null) {
			log.debug("get property with name {} and type {} using converter {}",
					new Object[] { field.getName(), type, converter.getClass() });
			Object value = converter.convertFromPrimitive(property);
			field.set(entity, value);
			return;
		}
		
		if(property.getClass().isArray()){
			log.debug("get property with name {} and type array", field.getName());
			Collection<Object> collection = (Collection<Object>) newInstanceOfCollection(field.getType());
			collection.addAll(ArrayUtils.toCollection(property));
			field.set(entity, collection);
			return;
		}
		
		// if doesn't have a converter then it is primitive
		field.set(entity, property);
	}
}
