#!/bin/sh

abort()
{
    echo -e "\nExiting run_game_ga.sh."
    exit 1
}

trap 'abort' 0
      	
echo "please specify the HaliteGenAlgo arguments: [1] number of iterations [2] number of batches [3] a name for this GA Run"
read N_ITS N_BATCHES RUNID

let ITCOUNT=0
let ITMAX=N_ITS*N_BATCHES

javac genAlgo/TournamentSelector.java
javac genAlgo/HaliteGenAlgo.java
java genAlgo/HaliteGenAlgo $N_IND $N_ITS $N_BATCHES $RUNID

while [ $ITCOUNT -lt $ITMAX ]
do
	java genAlgo/TournamentSelector 1
	chmod +x configs/matches.sh
	#echo "loop, it=$ITCOUNT "
	source configs/matches.sh
	java genAlgo/HaliteGenAlgo $ITCOUNT
	

	let ITCOUNT++
done

trap : 0

echo "finished"
#javac InitialBot.java
#javac ModifiedBot.java
#./halite "java ModifiedBot" "java ModifiedBot"