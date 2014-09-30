#!/usr/bin/env sh
java -cp java/classes cs224n.assignments.WordAlignmentTester \
  -dataPath /afs/ir/class/cs224n/data/pa1/ \
  -model cs224n.wordaligner.IBMModel1 -language french -evalSet dev \
  -trainSentences 10000
