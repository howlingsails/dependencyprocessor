# Dependency Processor

## Summary

Build a map of the versioned relationship between all dependency map/counts between n by n for all release branches.
This should be use a weighted list of the what versions of what dependencies should be used together for other projects.

## Basic Process

1. Read each repo from golden-repos
2. Pull each repo from cloud
3. Get List of all release tags
4. For each release tag checkout tag.
5. Run "mvn dependency:tree"
6. Process Output and build Graph of relationship version
7. Clear Directory

## Relationship Process Mapping





