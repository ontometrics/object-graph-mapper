package com.ontometrics.testing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.Node;

import com.ontometrics.db.graph.Person;
import com.ontometrics.util.TemporaryRepository;

public class TemporaryRepositoryTest {

	@Rule
	public TemporaryRepository<Person> repo = new TemporaryRepository<Person>();
	
	@Test
	public void canUseRepository(){
		Person person = new Person("bozo");
		assertThat(repo, is(notNullValue()));
		Node node = repo.getRepository().create(person);
		assertThat(node, is(notNullValue()));
	}
	
	
	
}
