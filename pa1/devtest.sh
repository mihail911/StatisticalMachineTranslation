#!/usr/bin/env bash
if [ $# -ne 4 ]
then
    echo "do ./devtest.sh language PMIModel numIterations pNull"
    exit -1
fi

language=$1
AlignmentModel=$2
NumIterations=$3
PNull=$4

java -cp java/classes cs224n.assignments.WordAlignmentTester \
  -dataPath /afs/ir/class/cs224n/data/pa1/ \
  -model cs224n.wordaligner.$AlignmentModel -language $language -evalSet dev \
  -trainSentences 10000 -numIterations $NumIterations -pNull $PNull
