package com.ontometrics.db.graph.model;

import java.io.File;

import org.junit.After;
import org.junit.Before;

import com.ontometrics.db.graph.EntityManager;
import com.ontometrics.db.graph.EntityManagerFactory;

public abstract class RepositoryTestCase{

	protected EntityManager entityManager;

	@Before
	public void setUp() {
		EntityManagerFactory emf = new EntityManagerFactory();
		deleteFileOrDirectory(emf.getRoot());
		entityManager = emf.getEntityManager();
	}

	@After
	public void tearDown(){
		entityManager.getDatabase().shutdown();
	}

	private void deleteFileOrDirectory(File path) {
		if (path.exists()) {
			if (path.isDirectory()) {
				for (File child : path.listFiles()) {
					deleteFileOrDirectory(child);
				}
			}
			path.delete();
		}
	}

}
