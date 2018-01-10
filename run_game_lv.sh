#!/bin/sh
echo "[1] LOADNAME1 [2] LOADNAME2"
read LN1 LN2
javac ModifiedBot.java
./halite "java ModifiedBot $LN1" "java ModifiedBot $LN2"