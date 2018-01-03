#!/bin/sh

javac InitialBot.java
javac ModifiedBot.java
./halite "java ModifiedBot" "java ModifiedBot"