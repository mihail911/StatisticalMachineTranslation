#!/usr/bin/env bash
if [ $# -ne 5 ]
then
    echo "do ./holdouttest.sh language PMIModel numIterations pNull AlignmentFile"
    exit -1
fi

language=$1
AlignmentModel=$2
NumIterations=$3
PNull=$4
AlignmentFile=$5

java -cp java/classes cs224n.assignments.WordAlignmentTester \
  -dataPath /afs/ir/class/cs224n/data/pa1/ \
  -model cs224n.wordaligner.$AlignmentModel -language $language -evalSet test \
  -trainSentences 10000 -numIterations $NumIterations -pNull $PNull -outputAlignments $AlignmentFile
