#!/bin/sh

javac InitialBot.java
javac MBotA.java
javac MBotB.java
javac ModifiedBot.java

./halite -s 3271081150  "java InitialBot" "java ModifiedBot" "java MBotA" "java MBotB"