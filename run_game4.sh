#!/bin/sh

javac InitialBot.java
javac ModifiedBot.java
./halite "java InitialBot" "java InitialBot" "java ModifiedBot" "java ModifiedBot"