package com.ontometrics.db.graph;

import java.lang.reflect.Field;
import java.text.MessageFormat;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityRepository<T> {

	private Logger log = LoggerFactory.getLogger(EntityRepository.class);

	protected EntityManager entityManager;

	public Node create(T entity) {
		return entityManager.create(entity);
	}

	public T read(Class<T> entityClass, Object primaryKey) throws IllegalArgumentException {
		Index<Node> index = entityManager.getNodeIndex(entityClass);
		Node node = index.get(EntityManager.PRIMARY_KEY, primaryKey).getSingle();
		if (node == null) {
			throw new IllegalArgumentException("No node found with key " + primaryKey);
		}
		T entity = null;
		try {
			entity = entityClass.newInstance();
		} catch (Exception e) {
			log.error(MessageFormat.format("error creating entity: {0}", entityClass.getName()), e);
		}
		GraphDBEntityBuilder.buildEntity(node, entity);
		return entity;
	}

	public void update(T entity) {
		// TODO: this should be: entityManager.update(), then we'd have one tx.
		Object primaryKey = getPrimaryKey(entity);
		if(primaryKey == null){
			throw new IllegalArgumentException(MessageFormat.format(
					"No primary key for class {0}", entity.getClass()));
		}
		Transaction transaction = entityManager.getDatabase().beginTx();
		Index<Node> index = entityManager.getNodeIndex(entity.getClass());
		Node node = index.get(EntityManager.PRIMARY_KEY, primaryKey).getSingle();
		if(node == null){
			throw new IllegalArgumentException(MessageFormat.format(
					"No node exists for class {0} with primary key {1}", entity.getClass(), primaryKey));
		}
		entityManager.update(entity, node);
		transaction.success();
		transaction.finish();
	}

	private Object getPrimaryKey(T entity) {
		Class<?> clazz = entity.getClass();
		while (clazz != null && !clazz.isPrimitive() && !clazz.equals(Object.class)) {
			for (Field field : clazz.getDeclaredFields()) {
				try {
					if (isThePrimaryKey(field)) {
						field.setAccessible(true);
						return field.get(entity);
					}
				} catch (Exception e) {
					log.error("error creating node for entity: " + entity, e);
				}
			}
			clazz = clazz.getSuperclass();
		}
		return null;
	}

	private static boolean isThePrimaryKey(Field field) {
		return field.isAnnotationPresent(javax.persistence.Id.class) || field.isAnnotationPresent(Id.class);
	}

	public void destroy(T entity, Object primaryKey) {
		Transaction transaction = entityManager.getDatabase().beginTx();
		Index<Node> index = entityManager.getNodeIndex(entity.getClass());
		Node node = index.get(EntityManager.PRIMARY_KEY, primaryKey).getSingle();
		node.delete();
		transaction.success();
		transaction.finish();
	}

	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

}
