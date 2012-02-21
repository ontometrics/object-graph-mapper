package com.ontometrics.db.graph;

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
		Index<Node> index = entityManager.getDatabase().index().forNodes(entityClass.getName());
		Node node = index.get(GraphDBEntityBuilder.PRIMARY_KEY, primaryKey).getSingle();
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

	public void update(T entityClass, Object primaryKey) {
		// TODO: this should be: entityManager.update(), then we'd have one tx.
		Transaction transaction = entityManager.getDatabase().beginTx();
		Index<Node> index = entityManager.getDatabase().index().forNodes(entityClass.getClass().getName());
		Node node = index.get(GraphDBEntityBuilder.PRIMARY_KEY, primaryKey).getSingle();
		entityManager.update(entityClass, node);
		transaction.success();
		transaction.finish();
	}

	public void destroy(T entityClass, Object primaryKey) {
		Transaction transaction = entityManager.getDatabase().beginTx();
		Index<Node> index = entityManager.getDatabase().index().forNodes(entityClass.getClass().getName());
		Node node = index.get(GraphDBEntityBuilder.PRIMARY_KEY, primaryKey).getSingle();
		node.delete();
		transaction.success();
		transaction.finish();
	}

	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

}
