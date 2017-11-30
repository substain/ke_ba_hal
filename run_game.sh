#!/bin/sh

javac InitialBot.java
javac ModifiedBot.java
./halite -d "240 160" "java InitialBot" "java ModifiedBot"