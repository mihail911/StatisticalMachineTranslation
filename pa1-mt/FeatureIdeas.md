# Features Ideas for Machine Translation System

* Local features can be extracted from individual rules

* Lexicalized alignmnet features allow model to compensate for events;
	firing an indicator for each alignment in a rule
* Map lexical items to classes and fire indicator for each mapping per
 	alignment
* Ratio of targent punctuation tokens to source punctuation tokens for
 	each derivation

 * Some scaled factor of the linear distortion values per rule
 	(put a higher emphasis on the linear distortion)
 * Take the difference between the 'targetPosition' in the
  	target sequence and the 'sourcePosition' in the source sequence,
  	putting an emphasis on proximity of translations