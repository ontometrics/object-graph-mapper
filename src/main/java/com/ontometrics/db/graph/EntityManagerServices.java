package com.ontometrics.db.graph;

import java.io.File;

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

	private static void init() {
		File root = new File("/tmp/graphdb");
		EntityManagerFactory emf = new EntityManagerFactory();
		emf.setRoot(root);
		log.debug("root: {}", root);
		entityManager = emf.getEntityManager("testdb");
	}

}
