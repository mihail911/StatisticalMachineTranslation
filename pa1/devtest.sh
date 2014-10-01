#!/usr/bin/env bash
if [ $# -ne 1 ] 
then 
    echo "do ./devtest.sh PMIModel"
    exit -1
fi

echo $#

java -cp java/classes cs224n.assignments.WordAlignmentTester \
  -dataPath /afs/ir/class/cs224n/data/pa1/ \
  -model cs224n.wordaligner.$1 -language french -evalSet dev \
  -trainSentences 10000
