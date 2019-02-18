package com.howlingsails;


import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.net.ServerAddress;

import java.util.Arrays;
import java.util.HashSet;

import static org.neo4j.driver.v1.Values.parameters;

public class GraphProcessor implements AutoCloseable {

    private final Driver driver;
    String username = "neo4j";
    String password = "some password";

    public GraphProcessor( String uri, String user, String password )
    {
        driver = GraphDatabase.driver( uri, AuthTokens.basic( user, password ) );
        /**
         *         try ( Driver driver = createDriver( "bolt+routing://x.acme.com", username, password, ServerAddress.of( "a.acme.com", 7676 ),
         *                 ServerAddress.of( "b.acme.com", 8787 ), ServerAddress.of( "c.acme.com", 9898 ) ) )
         *         {
         */

    }


    private Driver createDriver( String virtualUri, String user, String password, ServerAddress... addresses )
    {
        Config config = Config.builder()
                .withResolver( address -> new HashSet<>( Arrays.asList( addresses ) ) )
                .build();

        return GraphDatabase.driver( virtualUri, AuthTokens.basic( user, password ), config );
    }

    public void addModule( String name )
    {
        try (Session session = driver.session(AccessMode.WRITE)) {
            session.run("CREATE (a:Module {name: $name})", parameters("name", name));
        }
    }


    public void addProject(String name) {
        try (Session session = driver.session(AccessMode.WRITE)) {
            session.run("CREATE (a:Project {name: $name})", parameters("name", name));
        }

    }

    public void addModuleToProjectLink(String moduleName, String projectName) {
        /**
         * Create a uses relationship between module and project by version
         */

    }

    public void addModuleDualDirectionWorksWithLink(String moduleName, String ModuleName2) {
        /**
         * Create a works with relationship between all modules to every other module in the project
         */

    }



    @Override
    public void close() throws Exception
    {
        driver.close();
    }

}
