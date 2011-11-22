package org.thobe.playthings.aether;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.thobe.playthings.aether.NeoRepo;

public class TestNeoRepo
{
    private static AbstractGraphDatabase graphdb;

    @BeforeClass
    public static void startGraphdb()
    {
        graphdb = new EmbeddedGraphDatabase( "target/test-data/" + TestNeoRepo.class.getName() );
    }

    @AfterClass
    public static void stopGraphdb()
    {
        try
        {
            if ( graphdb != null ) graphdb.shutdown();
        }
        finally
        {
            graphdb = null;
        }
    }

    private NeoRepo<Person> repo;

    @Before
    public void createRepo()
    {
        repo = new NeoRepo<Person>( graphdb, Person.class );
    }

    @Test
    public void repositoryReturnsSameInstance()
    {
        Person john = repo.getOrCreate( "Name", "John" );
        assertEquals( "John", john.getName() );
        Person otherJohn = repo.getOrCreate( "Name", "John" );
        assertEquals( john, otherJohn );
    }
}
