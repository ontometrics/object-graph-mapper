package com.ontometrics.db.graph;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;

/**
 * Can return appropriate {@link EntityManager} for use with the underlying
 * database.
 * 
 * @author Rob
 * 
 */
public class EntityManagerFactory {

	/**
	 * The folder where the various databases reside on the file system.
	 */
	private File root = new File("target/var");

	/**
	 * We want to hang on to databases that we have open.
	 */
	private Map<String, GraphDatabaseService> databases = new HashMap<String, GraphDatabaseService>();;

	/**
	 * Get the EntityManager for a given database.
	 * 
	 * @param databaseName
	 *            the name of the database that is sought
	 * @return a database if it was found
	 */
	public EntityManager getEntityManager(String databaseName) {
		return new EntityManager(getDatabase(databaseName));
	}

	/**
	 * Attempts to get database from cache, then opens a new one if we don't
	 * have an open one.
	 * 
	 * @param databaseName
	 *            the name of the database that is sought
	 * @return a database
	 */
	private GraphDatabaseService getDatabase(String databaseName) {
		GraphDatabaseService database = databases.get(databaseName);
		if (database == null) {
			database = openDatabase(databaseName);
		}
		return database;
	}

	/**
	 * Opens the database from location on disk for use in session.
	 * 
	 * @param databaseName
	 * @return
	 */
	private GraphDatabaseService openDatabase(String databaseName) {
		GraphDatabaseService database = new EmbeddedGraphDatabase(root.getAbsolutePath());
		databases.put(databaseName, database);
		return database;
	}

	/**
	 * Provides means of telling the factory where all the databases reside.
	 * 
	 * @param root
	 *            the file system uri
	 */
	public void setRoot(File root) {
		this.root = root;
	}

	public File getRoot() {
		return root;
	}

}
