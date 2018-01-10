#!/bin/sh
      	
echo "please specify the HaliteGenAlgo arguments: [1] number of Individuals [2] number of iterations [3] number of batches [4] a name for this GA Run"
read N_IND N_ITS N_BATCHES RUNID

let ITCOUNT=0
let ITMAX=N_ITS*N_BATCHES

javac genAlgo/HaliteGenAlgo.java
java genAlgo/HaliteGenAlgo $N_IND $N_ITS $N_BATCHES $RUNID

while [ $ITCOUNT -lt $ITMAX ]
do
	chmod +x configs/matches.sh
	#echo "loop, it=$ITCOUNT "
	source configs/matches.sh
	java genAlgo/HaliteGenAlgo $ITCOUNT
	

	let ITCOUNT++
done

echo "finished"
#javac InitialBot.java
#javac ModifiedBot.java
#./halite "java ModifiedBot" "java ModifiedBot"