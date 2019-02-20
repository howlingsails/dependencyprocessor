package com.howlingsails;


import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class DependencyProcessor {

    String workDirectory = "/tmp/";

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
        GraphProcessor gp = new GraphProcessor("bolt://localhost:7687", "neo4j", "vagrant"); //??? 7474, 7687
        gp.cleanPreviousRun();
        gp.setConstraints();
        System.out.println("**********************************************************************");
        TreeSet<String> orderedList = new TreeSet<>(dependencyTree.keySet());
        for (String dtiKey : orderedList) {
            HashMap<String, TreeSet<String>> artifactList = dependencyTree.get(dtiKey);
            TreeSet<String> secondOrderedList = new TreeSet<>(artifactList.keySet());
            for (String artifact : secondOrderedList) {
                TreeSet<String> users = artifactList.get(artifact);
                for (String repo : users) {
                    System.out.println(dtiKey + "++" + artifact + "++" + repo);
                    gp.addModule(dtiKey);
                    gp.addProject(repo);
                    gp.addModuleVersion(dtiKey + ":" + artifact);
                    gp.addModuleToProjectLink(dtiKey, repo);
                    gp.addModuleVersionToProjectLink(dtiKey + ":" + artifact, repo);

                }
            }

        }


    }

    private void printTree() {
        System.out.println("**********************************************************************");
        TreeSet<String> orderedList = new TreeSet<>(dependencyTree.keySet());
        for (String dtiKey : orderedList) {
            HashMap<String, TreeSet<String>> artifactList = dependencyTree.get(dtiKey);
            TreeSet<String> secondOrderedList = new TreeSet<>(artifactList.keySet());
            for (String artifact : secondOrderedList) {
                TreeSet<String> users = artifactList.get(artifact);
                for (String repo : users) {
                    System.out.println(dtiKey + "++" + artifact + "++" + repo);
                }
            }

        }

    }

    private void processRepoDependency(String repo) {
        if (isRepoMaven(repo)) {
            processMavenRepoDependency(repo);
        } else if (isGradleRepo(repo)) {
            processGradleRepoDependency(repo);
        }
    }

    private void processGradleRepoDependency(String repo) {
        try {
            String cmd = "for D in *; do if [ -d \"${D}\" ]; then gradle \"$D\":dependencies; fi done";
            String[] repoParts = repo.split("/");
            String repoName = repoParts[repoParts.length - 1];
            String[] fullcmd = {"bash","-c","for D in *; do if [ -d \"${D}\" ]; then gradle \"$D\":dependencies; fi done"};
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new File(workDirectory + repoName));
            pb.command(fullcmd);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            String dependencyLine;
            boolean isProcessingDependency = false;
            while (process.isAlive()) {
                while ((dependencyLine = reader.readLine()) != null) {
                    System.out.println("Script output: " + dependencyLine);
                    if (isProcessingDependency && dependencyLine.isEmpty()) {
                        isProcessingDependency = false;
                    }
                    if (isProcessingDependency) {
                        addGradleDependencyLine(repoName, dependencyLine);
                    }
                    if (dependencyLine.startsWith("+-")) {
                        isProcessingDependency = true;
                        addGradleDependencyLine(repoName, dependencyLine);
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

    private void addGradleDependencyLine(String repoName, String dependencyLine) {
        String tmp = dependencyLine.substring(2)
                .replace(" ", "")
                .replace("+", "")
                .replace("-", "")
                .replace("|", "")
                .replace("\\", "");

        if (tmp.contains(":")) {
            System.out.println(repoName + "  " + tmp);
            String[] results = tmp.split(":");
            if (results.length < 3) {
                System.out.println(repoName + "  " + tmp);

                return;
            }
            String group = results[0];
            String artifact = results[1];
            String version = results[2];
            addDependency(group + "+" + artifact, version, repoName);

        }

    }

    private boolean isGradleRepo(String repo) {
        String filetoFind = "build.gradle";
        return findFile(repo, filetoFind);
    }


    private boolean isRepoMaven(String repo) {
        String filetoFind = "pom.xml";
        return findFile(repo, filetoFind);
    }

    private boolean findFile(String repo, String filetoFind) {
        String[] repoParts = repo.split("/");
        String repoName = repoParts[repoParts.length - 1];
        File f = new File(workDirectory + repoName);
        File[] matchingFiles = f.listFiles((file, filename) -> {
            if (filename.toLowerCase().equals(filetoFind)) return true;
            return false;
        });
        return (matchingFiles != null && matchingFiles.length > 0);
    }

    private void processMavenRepoDependency(String repo) {
        try {
            String cmd = "/usr/bin/mvn dependency:tree";
            String[] repoParts = repo.split("/");
            String repoName = repoParts[repoParts.length - 1];
            Process process = Runtime.getRuntime().exec(cmd, null, new File(workDirectory + repoName));
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            String dependencyLine;
            boolean isProcessingDependency = false;
            while (process.isAlive()) {
                while ((dependencyLine = reader.readLine()) != null) {
                    System.out.println("Script output: " + dependencyLine);
                    if (dependencyLine.contains("---------------------------------------------")) {
                        isProcessingDependency = false;
                    }
                    if (isProcessingDependency) {
                        addMavenDependencyLine(repoName, dependencyLine);
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

    private void addMavenDependencyLine(String repoName, String dependencyLine) {
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

            Process process = Runtime.getRuntime().exec(cmd, null, new File(workDirectory));
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getErrorStream()));
            String s;
            while (process.isAlive()) {
                while ((s = reader.readLine()) != null) {
                    System.out.println("Script output: " + s);
                }
                process.waitFor(100, TimeUnit.MICROSECONDS);
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
