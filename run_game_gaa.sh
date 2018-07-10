#!/bin/sh

abort()
{
    echo -e "\nExiting run_game_ga.sh." | tee -a configs/results/shout.txt
    exit 1
}

trap 'abort' 0

echo "please specify the HaliteGenAlgo/Analysis arguments: [1] number of iterations [2] number of batches [3] a name for this GA Run [4] num A-matches [5] num B-matches [6] num C-matches"
read N_ITS N_BATCHES RUNID A_NM B_NM C_NM

echo "#############################" | tee configs/results/shout.txt
echo "StartTime" | tee -a configs/results/shout.txt
date | tee -a configs/results/shout.txt
echo "#############################" | tee -a configs/results/shout.txt
echo "########## GenALgo ##########" | tee -a configs/results/shout.txt
echo "#############################" | tee -a configs/results/shout.txt


let ITCOUNT=0
let ITMAX=N_ITS*N_BATCHES

javac genAlgo/TournamentSelector.java
javac genAlgo/HaliteGenAlgo.java
java genAlgo/HaliteGenAlgo $N_IND $N_ITS $N_BATCHES $RUNID | tee -a configs/results/shout.txt

while [ $ITCOUNT -lt $ITMAX ]
do
	java genAlgo/TournamentSelector 1
	chmod +x configs/matches.sh
	#echo "loop, it=$ITCOUNT "
	source configs/matches.sh
	java genAlgo/HaliteGenAlgo $ITCOUNT | tee -a configs/results/shout.txt
	

	let ITCOUNT++
done

echo "#############################" | tee -a configs/results/shout.txt
echo "Time After GA" | tee -a configs/results/shout.txt
date | tee -a configs/results/shout.txt
echo "#############################" | tee -a configs/results/shout.txt
echo "######### Analysis ##########" | tee -a configs/results/shout.txt
echo "#############################" | tee -a configs/results/shout.txt

java genAlgo/HaliteGenAlgo $RUNID $A_NM $B_NM $C_NM | tee -a configs/results/shout.txt
chmod +x configs/a_matches.sh
source configs/a_matches.sh
java genAlgo/HaliteGenAlgo $RUNID 0 | tee -a configs/results/shout.txt
chmod +x configs/b_matches.sh
source configs/b_matches.sh
java genAlgo/HaliteGenAlgo $RUNID 1 | tee -a configs/results/shout.txt
chmod +x configs/c_matches.sh
source configs/c_matches.sh
java genAlgo/HaliteGenAlgo $RUNID 2 | tee -a configs/results/shout.txt


echo "#############################" | tee -a configs/results/shout.txt
echo "Time After Analysis" | tee -a configs/results/shout.txt
date | tee -a configs/results/shout.txt
echo "######### finished. #########" | tee -a configs/results/shout.txt
trap : 0
