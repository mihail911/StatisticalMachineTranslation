#!/usr/bin/env sh
java -cp java/classes cs224n.assignments.WordAlignmentTester \
-dataPath /afs/ir/class/cs224n/data/pa1/ \
-model cs224n.wordaligner.IBMModel2 -evalSet miniTest -verbose
