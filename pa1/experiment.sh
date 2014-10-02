#!/usr/bin/env bash
if [ $# -ne 3 ]
then
    echo "do ./devtest.sh PMIModel numIterations pNull"
    exit -1
fi

for language in french hindi chinese
do

  java -cp java/classes cs224n.assignments.WordAlignmentTester \
    -dataPath /afs/ir/class/cs224n/data/pa1/ \
    -model cs224n.wordaligner.BaselineWordAligner -language $language -evalSet dev \
    -trainSentences 10000 -numIterations 1 -pNull 0.2 > $language.Baseline.tuning.out

  java -cp java/classes cs224n.assignments.WordAlignmentTester \
    -dataPath /afs/ir/class/cs224n/data/pa1/ \
    -model cs224n.wordaligner.PMIModel -language $language -evalSet dev \
    -trainSentences 10000 -numIterations 1 -pNull 0.2 > $language.PMIModel.tuning.out

  for alignmentModel in IBMModel1 IBMModel2
  do
    for pNull in 0.1 0.2 0.3 0.4
    do
      for numIterations in 1 3 6 10 30
      do
        java -cp java/classes cs224n.assignments.WordAlignmentTester \
          -dataPath /afs/ir/class/cs224n/data/pa1/ \
          -model cs224n.wordaligner.$alignmentModel -language $language -evalSet dev \
          -trainSentences 10000 -numIterations $numIterations -pNull $pNull > $language.$alignmentModel.tuning.out
      done
    done
  done

done
