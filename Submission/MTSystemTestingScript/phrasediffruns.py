#!/usr/bin/env python
import os

#Runs the target size + punctuation  MT system a number of times
for i in range(1,4):
	command = "phrasal.sh cs224n.vars 1-5 myfeature.ini phrasediffsquaredrun_" + str(i)
	os.system(command)	
