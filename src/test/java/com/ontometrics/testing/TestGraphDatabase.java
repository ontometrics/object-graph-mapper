package com.ontometrics.testing;

import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom @Rule implementation for making an embeddable graph database for use
 * in tests.
 * <p>
 * Does the setup and teardown of the database, creates a transaction.
 * 
 * @see EmbeddedGraphDatabase, {@link ExternalResource}
 * 
 * @author Rob
 * 
 */
public class TestGraphDatabase extends ExternalResource {

	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(TestGraphDatabase.class);

	public TemporaryFolder tempFolder;

	private EmbeddedGraphDatabase database;
	private Transaction transaction;

	public TestGraphDatabase(TemporaryFolder tempFolder) {
		this.tempFolder = tempFolder;
	}

	@Override
	protected void before() throws Throwable {
		super.before();
	}

	@Override
	protected void after() {
		super.after();
		transaction.finish();
		// database.shutdown();
	}

	public EmbeddedGraphDatabase getDatabase() {
		if (database == null) {
			database = new EmbeddedGraphDatabase(tempFolder.getRoot().getAbsolutePath());
			transaction = database.beginTx();
		}
		return database;
	}

	public Transaction getTransaction() {
		return transaction;
	}

}
