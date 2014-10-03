package edu.stanford.nlp.mt.decoder.feat;

import java.util.List;
import java.util.*;

import edu.stanford.nlp.mt.util.FeatureValue;
import edu.stanford.nlp.mt.util.Featurizable;
import edu.stanford.nlp.mt.util.IString;
import edu.stanford.nlp.mt.decoder.feat.RuleFeaturizer;
import edu.stanford.nlp.util.Generics;

/**
 * A rule featurizer.
 */
public class PhraseDiffFeaturizer implements RuleFeaturizer<IString, String> {
  
  @Override
  public void initialize() {
    // Do any setup here.
  }

  @Override
  public List<FeatureValue<String>> ruleFeaturize(
      Featurizable<IString, String> f) {

  	Integer diff = f.targetPhrase.size() - f.sourcePhrase.size();
    List<FeatureValue<String>> features = Generics.newLinkedList();
    features.add(new FeatureValue<String>(String.format("%s:%d","PHRASE_DIFF",
      diff), 1.0));
    return features;
  }

  @Override
  public boolean isolationScoreOnly() {
    return false;
  }
}
