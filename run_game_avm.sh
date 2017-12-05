#!/bin/sh

javac MBotA.java
javac ModifiedBot.java
./halite "java MBotA" "java ModifiedBot"