#!/bin/sh

abort()
{
    echo -e "\nExiting run_game_ga.sh." | tee -a configs/results/shoutao.txt
    exit 1
}

trap 'abort' 0

echo "please specify the Analysis arguments: [1] num A-matches [2] num B-matches [3] num C-matches [4] name of the run"
read  A_NM B_NM C_NM RUNID

echo "#############################" | tee configs/results/shoutao.txt
echo "#############################" | tee -a configs/results/shoutao.txt
echo "StartTime" | tee -a configs/results/shoutao.txt
date | tee -a configs/results/shoutao.txt
echo "#############################" | tee -a configs/results/shoutao.txt
echo "######### Analysis ##########" | tee -a configs/results/shoutao.txt
echo "#############################" | tee -a configs/results/shoutao.txt

javac genAlgo/HaliteGenAlgo.java
java genAlgo/HaliteGenAlgo $RUNID $A_NM $B_NM $C_NM | tee -a configs/results/shoutao.txt
chmod +x configs/a_matches.sh
echo "Time before A-Matches:" | tee -a configs/results/shoutao.txt
date | tee -a configs/results/shoutao.txt
source configs/a_matches.sh
echo "Time after A-Matches:" | tee -a configs/results/shoutao.txt
date | tee -a configs/results/shoutao.txt
java genAlgo/HaliteGenAlgo $RUNID 0 | tee -a configs/results/shoutao.txt


chmod +x configs/b_matches.sh
source configs/b_matches.sh
echo "Time after B-Matches:" | tee -a configs/results/shoutao.txt
date | tee -a configs/results/shoutao.txt
java genAlgo/HaliteGenAlgo $RUNID 1 | tee -a configs/results/shoutao.txt


chmod +x configs/c_matches.sh
source configs/c_matches.sh
echo "Time after C-Matches:" | tee -a configs/results/shoutao.txt
date | tee -a configs/results/shoutao.txt
java genAlgo/HaliteGenAlgo $RUNID 2 | tee -a configs/results/shoutao.txt


echo "#############################"| tee -a configs/results/shoutao.txt
echo "Time After Analysis" | tee -a configs/results/shoutao.txt
date | tee -a configs/results/shoutao.txt
echo "######### finished. #########" | tee -a configs/results/shoutao.txt
trap : 0

