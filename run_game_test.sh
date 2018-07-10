#!/bin/sh

javac ModifiedBot.java
javac SitBot.java

./halite "java ModifiedBot" "java SitBot"