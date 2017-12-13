#!/bin/sh

javac InitialBot.java
javac MBotA.java
javac MBotB.java
javac ModifiedBot.java

./halite "java InitialBot" "java ModifiedBot" "java MBotA" "java MBotB"