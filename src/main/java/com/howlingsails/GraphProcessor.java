package com.howlingsails;


import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.exceptions.ClientException;
import org.neo4j.driver.v1.net.ServerAddress;

import java.util.Arrays;
import java.util.HashSet;

import static org.neo4j.driver.v1.Values.parameters;

/**
 * Following:
 *
 * https://neo4j.com/docs/driver-manual/1.7/client-applications/
 */
public class GraphProcessor implements AutoCloseable {

    private final Driver driver;
    String username = "neo4j";
    String password = "varant";

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

    public void cleanPreviousRun() {

        try (Session session = driver.session(AccessMode.WRITE)) {
            session.run("MATCH (a:ModuleVersion) DETACH DELETE a");
        }
        try (Session session = driver.session(AccessMode.WRITE)) {
            session.run("MATCH (a:Module) DETACH DELETE a");
        }
        try (Session session = driver.session(AccessMode.WRITE)) {
            session.run("MATCH (a:Project) DETACH DELETE a");
        }

    }

    public void setConstraints() {
        try (Session session = driver.session(AccessMode.WRITE)) {
            session.run("CREATE CONSTRAINT ON (a:Module ) ASSERT a.name IS UNIQUE");
        }
        try (Session session = driver.session(AccessMode.WRITE)) {
            session.run("CREATE CONSTRAINT ON (a:Project ) ASSERT a.name IS UNIQUE");
        }
        try (Session session = driver.session(AccessMode.WRITE)) {
            session.run("CREATE CONSTRAINT ON (a:ModuleVersion ) ASSERT a.name IS UNIQUE");
        }

    }


    public void addModule( String name )
    {
        try (Session session = driver.session(AccessMode.WRITE)) {
            session.run("CREATE (a:Module {name: $name})", parameters("name", name));
        } catch(ClientException ce) {
        }
    }
    public void addModuleVersion( String name )
    {
        try (Session session = driver.session(AccessMode.WRITE)) {
            session.run("CREATE (a:ModuleVersion {name: $name})", parameters("name", name));
        } catch(ClientException ce) {
        }
    }


    public void addProject(String name) {
        try (Session session = driver.session(AccessMode.WRITE)) {
            session.run("CREATE (a:Project {name: $name})", parameters("name", name));
        } catch(ClientException ce) {
        }

    }



    public void addModuleToProjectLink(String moduleName, String projectName) {
        /**
         * Create a uses relationship between module and project by version
         */
        try (Session session = driver.session(AccessMode.WRITE)) {
            StringBuffer sb = new StringBuffer();
           sb.append("MATCH (m:Module),(p:Project)\n");
           sb.append("WHERE m.name = $moduleName AND p.name = $projectName\n");
           sb.append("CREATE UNIQUE (m)-[r:UTILIZE]->(p)\n");
           session.run(sb.toString(), parameters("moduleName", moduleName,"projectName",projectName));
        }
        /**
         *
         * Query to get it back out in Neo4j
         *
         * MATCH (m)-[r:UTILIZE]->(p) RETURN m,r,p LIMIT 10000
         *
         *
         * https://stackoverflow.com/questions/30783783/show-nodes-with-more-than-one-relationship-using-neo4j
         * MATCH (m)-[r:UTILIZE]->(p) WITH m, count(r) AS count WHERE count > 1 MATCH (m)-[r:UTILIZE]->(p) RETURN m,r,p
         *
         * MATCH (p)<-[r:UTILIZE]-(m) return m,r,p LIMIT 50000
         * MATCH (m:m)<-[r:UTILIZE]-(m:Module) with m,count(*) as rel_cnt where rel_cnt > 2 RETURN m,r,rel_count,p LIMIT 1000
         *
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

    public void addModuleVersionToProjectLink(String moduleVersion, String projectName) {
        /**
         * Create a uses relationship between module and project by version
         */
        try (Session session = driver.session(AccessMode.WRITE)) {
            StringBuffer sb = new StringBuffer();
            sb.append("MATCH (mv:ModuleVersion),(p:Project)\n");
            sb.append("WHERE mv.name = $moduleName AND p.name = $projectName\n");
            sb.append("CREATE UNIQUE (mv)-[r:USED_BY]->(p)\n");
            session.run(sb.toString(), parameters("moduleName", moduleVersion,"projectName",projectName));
        }
        /**
         *
         * Query to get it back out in Neo4j
         *
         * MATCH (m)-[r:USED_BY]->(p) RETURN m,r,p LIMIT 10000
         *
         *
         */

    }
}
