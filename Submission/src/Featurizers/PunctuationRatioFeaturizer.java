package edu.stanford.nlp.mt.decoder.feat;

import java.util.List;
import java.util.*;
import java.lang.Math;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import edu.stanford.nlp.mt.util.FeatureValue;
import edu.stanford.nlp.mt.util.Featurizable;
import edu.stanford.nlp.mt.util.IString;
import edu.stanford.nlp.mt.decoder.feat.RuleFeaturizer;
import edu.stanford.nlp.util.Generics;

/**
 * A rule featurizer.
 */
public class PunctuationRatioFeaturizer implements RuleFeaturizer<IString, String> {
  
  @Override
  public void initialize() {
    // Do any setup here.
  }

  @Override
  public List<FeatureValue<String>> ruleFeaturize(
      Featurizable<IString, String> f) {

    Pattern p = Pattern.compile("\\p{Punct}", Pattern.UNICODE_CHARACTER_CLASS);
  	Double numTargetPunct = 0.0;
  	Double numSourcePunct = 0.0;
  	
    String source = f.sourcePhrase.toString();
    String target = f.targetPhrase.toString();	
    Matcher ms = p.matcher(source);
    Matcher mt = p.matcher(target);
    while(ms.find()){
      ++numSourcePunct;
    }
    while(mt.find()){
      ++numTargetPunct;
    }

    Double ratio = numTargetPunct / numSourcePunct;
    List<FeatureValue<String>> features = Generics.newLinkedList();
    if(!Double.isNaN(ratio) && !Double.isInfinite(ratio)){
      features.add(new FeatureValue<String>(String.format("%s:%f",
        "PUNCTUATION_RATIO", ratio), 1.0));  
    }
    
    return features;
  }

  @Override
  public boolean isolationScoreOnly() {
    return false;
  }
}
