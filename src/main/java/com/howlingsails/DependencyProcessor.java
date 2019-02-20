package com.howlingsails;



import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.springframework.data.annotation.Id;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class DependencyProcessor {

    HashMap<String, HashMap<String, TreeSet<String>>> dependencyTree = new HashMap<>();

    private File getFileFromURL(String name) {
        URL url = this.getClass().getClassLoader().getResource(name);
        File file = null;
        try {
            file = new File(url.toURI());
        } catch (URISyntaxException e) {
            file = new File(url.getPath());
        } finally {
            return file;
        }
    }

    public void go() {
        ArrayList<String> repoList = new ArrayList<>();
        loadRepoList(repoList);
        for (String repo : repoList) {
            processRepo(repo);
            processRepoDependency(repo);
        }
        //hprintTree();
        storetoNeo4j();
    }



    private void storetoNeo4j() {
        GraphProcessor gp = new GraphProcessor("bolt://localhost:7687","neo4j","vagrant"); //??? 7474, 7687
        gp.cleanPreviousRun();
        gp.setConstraints();
        System.out.println("**********************************************************************");
        TreeSet<String> orderedList = new TreeSet<>(dependencyTree.keySet());
        for (String dtiKey:orderedList) {
            HashMap<String, TreeSet<String>> artifactList = dependencyTree.get(dtiKey);
            TreeSet<String> secondOrderedList = new TreeSet<>(artifactList.keySet());
            for (String artifact: secondOrderedList) {
                TreeSet<String> users = artifactList.get(artifact);
                for(String repo:users) {
                    System.out.println(dtiKey+"++"+artifact+"++"+repo);
                    gp.addModule(dtiKey);
                    gp.addProject(repo);
                    gp.addModuleVersion(dtiKey+":"+artifact);
                    gp.addModuleToProjectLink(dtiKey,repo);
                    gp.addModuleVersionToProjectLink(dtiKey+":"+artifact,repo);

                }
            }

        }



    }

    private void printTree() {
        System.out.println("**********************************************************************");
        TreeSet<String> orderedList = new TreeSet<>(dependencyTree.keySet());
        for (String dtiKey:orderedList) {
            HashMap<String, TreeSet<String>> artifactList = dependencyTree.get(dtiKey);
            TreeSet<String> secondOrderedList = new TreeSet<>(artifactList.keySet());
            for (String artifact: secondOrderedList) {
                TreeSet<String> users = artifactList.get(artifact);
                for(String repo:users) {
                    System.out.println(dtiKey+"++"+artifact+"++"+repo);
                }
            }

        }

    }

    private void processRepoDependency(String repo) {
        try {
            String cmd = "/usr/bin/mvn dependency:tree";
            String[] repoParts = repo.split("/");
            String repoName = repoParts[repoParts.length - 1];
            Process process = Runtime.getRuntime().exec(cmd, null, new File("/tmp/" + repoName));
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            String dependencyLine;
            boolean isProcessingDependency = false;
            while(process.isAlive()) {
                while ((dependencyLine = reader.readLine()) != null) {
                    System.out.println("Script output: " + dependencyLine);
                    if (dependencyLine.contains("---------------------------------------------")) {
                        isProcessingDependency = false;
                    }
                    if (isProcessingDependency) {
                        addDependencyLine(repoName, dependencyLine);
                    }
                    if (dependencyLine.contains("maven-dependency-plugin")) {
                        isProcessingDependency = true;
                    }
                }
                process.waitFor(100, TimeUnit.MILLISECONDS);
            }
            System.out.println("Exit:" + process.exitValue());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void addDependencyLine(String repoName, String dependencyLine) {
        // TODO: parse
        String tmp = dependencyLine.substring(16)
                .replace(" ", "")
                .replace("+", "")
                .replace("-", "")
                .replace("|", "")
                .replace("\\", "");
        if (tmp.contains(":")) {
            System.out.println(repoName + "  " + tmp);
            String[] results = tmp.split(":");
            if (results.length < 4) {
                System.out.println(repoName + "  " + tmp);

                return;
            }
            String group = results[0];
            String artifact = results[1];
            String version = results[3];
            addDependency(group + "+" + artifact, version, repoName);

        }

    }

    private void addDependency(String groupArtifact, String version, String repoName) {
        if (!dependencyTree.containsKey(groupArtifact)) {
            dependencyTree.put(groupArtifact, new HashMap<String, TreeSet<String>>());
        }

        HashMap<String, TreeSet<String>> dependencyMap = dependencyTree.get(groupArtifact);

        if (!dependencyMap.containsKey(version)) {
            dependencyMap.put(version, new TreeSet<String>());
        }

        dependencyMap.get(version).add(repoName);

    }

    private void processRepo(String repo) {
        System.out.println(repo);

        try {
            String cmd = "git clone " + repo;

            Process process = Runtime.getRuntime().exec(cmd, null, new File("/tmp/"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getErrorStream()));
            String s;
            while(process.isAlive()) {
                while ((s = reader.readLine()) != null) {
                    System.out.println("Script output: " + s);
                }
                process.waitFor(100,TimeUnit.MICROSECONDS);
            }
            System.out.println("Exit:" + process.exitValue());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRepoList(ArrayList<String> repoList) {
        File repoFile = getFileFromURL("golden-repos");
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(repoFile));
            String line = reader.readLine();
            while (line != null) {
                // read next line
                line = reader.readLine();
                if (line != null &&
                        !line.startsWith("#")) { // Skip Comment out lines
                    repoList.add(line);
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
