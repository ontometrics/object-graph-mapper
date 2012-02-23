package com.ontometrics.util;

import org.junit.rules.TemporaryFolder;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ontometrics.db.graph.EntityManager;
import com.ontometrics.db.graph.EntityManagerFactory;
import com.ontometrics.db.graph.EntityRepository;

public class TemporaryRepository<T> extends TemporaryFolder {
	
	private static final Logger log = LoggerFactory.getLogger(TemporaryRepository.class);
	
	private EntityManager entityManager;
	private EmbeddedGraphDatabase database;
	private EntityRepository<T> repository;

	@Override
	protected void before() throws Throwable {
		super.before();
		EntityManagerFactory emf = new EntityManagerFactory();
		emf.setRoot(getRoot());
		log.debug("root: {}", getRoot());
		entityManager = emf.getEntityManager("testdb");
		log.debug("database: {}", database);
		repository = new EntityRepository<T>();
		repository.setEntityManager(entityManager);
		log.debug("repository: {}", repository);
	}
	
	@Override
	protected void after() {
		super.after();
	}
	
	public EmbeddedGraphDatabase getDatabase() {
		return database;
	}
	
	public EntityManager getEntityManager() {
		return entityManager;
	}
	
	public EntityRepository<T> getRepository() {
		return repository;
	}

}
