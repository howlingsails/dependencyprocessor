package com.howlingsails;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

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
        printTree();
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
            String cmd = "D:\\dev\\gcp\\apache-maven-3.5.4-bin\\apache-maven-3.5.4\\bin\\mvn.cmd dependency:tree";
            String[] repoParts = repo.split("/");
            String repoName = repoParts[repoParts.length - 1];
            Process process = Runtime.getRuntime().exec(cmd, null, new File("C:\\tmp\\" + repoName));
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            String dependencyLine;
            boolean isProcessingDependency = false;
            while ((dependencyLine = reader.readLine()) != null) {
                // System.out.println("Script output: " + dependencyLine);
                if (dependencyLine.contains("---------------------------------------------")) {
                    isProcessingDependency = false;
                }
                if (isProcessingDependency) {
                    addDependencyLine(repoName, dependencyLine);
                }
                if (dependencyLine.contains("--- maven-dependency-plugin")) {
                    isProcessingDependency = true;
                }
            }

            System.out.println("Exit:" + process.exitValue());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addDependencyLine(String repoName, String dependencyLine) {
        // TODO: parse
        String tmp = dependencyLine.substring(6)
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
        return;
/*        try {
            String cmd = "git clone "+repo;

            Process process = Runtime.getRuntime().exec(cmd,null,new File("C:\\tmp\\"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getErrorStream()));
            String s;
            while ((s = reader.readLine()) != null) {
                System.out.println("Script output: " + s);
            }

            System.out.println("Exit:" + process.exitValue());
        } catch (IOException e) {
            e.printStackTrace();
        }*/

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