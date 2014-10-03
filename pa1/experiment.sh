#!/usr/bin/env bash

for language in french hindi chinese
do
  echo ----- Running $language.BaselineWordAligner

  if [ -f $language.Baseline.tuning.out ];
  then
    echo "skipping..."
  else
    java -cp java/classes cs224n.assignments.WordAlignmentTester \
      -dataPath /afs/ir/class/cs224n/data/pa1/ \
      -model cs224n.wordaligner.BaselineWordAligner -language $language -evalSet dev \
      -trainSentences 10000 -numIterations 1 -pNull 0.2 > $language.Baseline.tuning.out
  fi

  echo ----- Running $language.PMIModel

  if [ -f $language.PMIModel.tuning.out ];
  then
    echo "skipping..."
  else
    java -cp java/classes cs224n.assignments.WordAlignmentTester \
      -dataPath /afs/ir/class/cs224n/data/pa1/ \
      -model cs224n.wordaligner.PMIModel -language $language -evalSet dev \
      -trainSentences 10000 -numIterations 1 -pNull 0.2 > $language.PMIModel.tuning.out
  fi

  for alignmentModel in IBMModel1 IBMModel2
  do
    for pNull in 0.1 0.12 0.14 0.16 0.18 0.20 0.22 0.24 0.26
    do
      for numIterations in 1 3 10 30 100
      do
        echo ----- Running $language.$alignmentModel with pNull=$pNull, numIterations=$numIterations

        if [ -f $language.$alignmentModel.$pNull.$numIterations.tuning.out ];
        then
          echo "skipping..."
        else
          java -cp java/classes cs224n.assignments.WordAlignmentTester \
            -dataPath /afs/ir/class/cs224n/data/pa1/ \
            -model cs224n.wordaligner.$alignmentModel -language $language -evalSet dev \
            -trainSentences 10000 -numIterations $numIterations -pNull $pNull > $language.$alignmentModel.$pNull.$numIterations.tuning.out
        fi
      done
    done
  done

done
