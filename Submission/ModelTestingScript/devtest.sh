#!/usr/bin/env bash
if [ $# -ne 3 ]
then
    echo "do ./devtest.sh PMIModel numIterations pNull"
    exit -1
fi

AlignmentModel=$1
NumIterations=$2
PNull=$3

java -cp java/classes cs224n.assignments.WordAlignmentTester \
  -dataPath /afs/ir/class/cs224n/data/pa1/ \
  -model cs224n.wordaligner.$AlignmentModel -language french -evalSet dev \
  -trainSentences 10000 -numIterations $NumIterations -pNull $PNull
