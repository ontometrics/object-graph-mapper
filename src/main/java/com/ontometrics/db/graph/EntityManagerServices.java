package com.ontometrics.db.graph;

import java.io.File;

import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityManagerServices {

	private static final Logger log = LoggerFactory.getLogger(EntityManagerServices.class);

	private static EntityManager entityManager;

	public static EntityManager getEntityManager() {
		
		if (entityManager == null) {
			init();
		}
		return entityManager;
	}


	/**
	 * Not currently used in tests, but eventually we would need a way to have the EntityManagerFactory
	 * initialized with an ImpermanentGraphDatabase for testing.
	 * 
	 */
	private static void init() {
		File root = new File("/tmp/graphdb");
		EntityManagerFactory emf = new EntityManagerFactory();
		emf.setRoot(root);
		log.debug("root: {}", root);
		entityManager = emf.getEntityManager();
	}

	public static void initForTests(GraphDatabaseService graphDatabase) {
		EntityManagerFactory emf = new EntityManagerFactory();
		entityManager = emf.getEntityManager(graphDatabase);
		
	}
	

}
