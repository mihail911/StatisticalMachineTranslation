package cs224n.wordaligner;

import cs224n.util.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class IBMModel1 implements WordAligner {

  // t(f|e): {e, f} note that this is actually indexed in reverse
  // the probability of producing translation f given word e
  private CounterMap<String,String> t_given_e_of_f =
              new CounterMap<String,String>();
  // C(e, f): {e, f}
  // the accumulator for fractional counts of coocurring english and french words
  private CounterMap<String,String> count_of_e_and_f =
              new CounterMap<String,String>(); // This actually P(English | Foreign)

  private int numIterations = 5;
  private Double PNull = 0.2;

  public CounterMap<String,String> get_t_given_e_of_f() {
    return t_given_e_of_f;
  }

  public Alignment align(SentencePair sentencePair) {
    Alignment alignments = new Alignment();
    // beware: there is a flip here
    List<String> englishWords = sentencePair.getSourceWords();
    List<String> foreignWords = sentencePair.getTargetWords();

    int foreignIndex = 0;
    for (String foreignWord: foreignWords) {
      Double maxScoreSoFar = -1.0;
      int indexOfBestAlignment = -1;
      int englishIndex = 0;

      for (String englishWord: englishWords) {
        Double score = (1 - PNull)/englishWords.size() * t_given_e_of_f.getCount(englishWord, foreignWord);
        if (score > maxScoreSoFar) {
          maxScoreSoFar = score;
          indexOfBestAlignment = englishIndex;
        }
        englishIndex += 1;
      }

      Double nullScore = PNull * t_given_e_of_f.getCount(NULL_WORD, foreignWord);

      if (nullScore < maxScoreSoFar) {
        // beware: there is a flip here
        // System.out.println("score: " + maxScoreSoFar + " nullScore: " + nullScore);
        alignments.addPredictedAlignment(foreignIndex, indexOfBestAlignment);
      }

      foreignIndex += 1;
    }
    // System.out.println(alignments);
    return alignments;
  }

  public void train(List<SentencePair> trainingdata) {
    // Step 1: Initialize
    for (SentencePair pair: trainingdata) {
      // beware: there is a flip here
      List<String> englishWords = pair.getSourceWords();
      List<String> foreignWords = pair.getTargetWords();

      for (String foreignWord: foreignWords) {
        for (String englishWord: englishWords) {
          // iterate over every word pair and record occurence
          t_given_e_of_f.setCount(englishWord, foreignWord, 1.0);
        }
        // add a count for NULL
        t_given_e_of_f.setCount(NULL_WORD, foreignWord, 1.0);
      }
    }
    t_given_e_of_f = Counters.conditionalNormalize(t_given_e_of_f);

    // Step 2: EM
    Double epsilon = 0.0;
    Double Z = 0.0;
    Double PAlignment = 0.0;

    for (int iter=0; iter<numIterations; iter++) {

      for (SentencePair pair: trainingdata) {
        // beware: there is a flip here
        List<String> englishWords = pair.getSourceWords();
        List<String> foreignWords = pair.getTargetWords();
        PAlignment = (1-PNull)/englishWords.size();

        for (String foreignWord: foreignWords) {
          // precompute normalization constant for parameter update
          Z = 0.0;
          for (String englishWord: englishWords) {
            Z += t_given_e_of_f.getCount(englishWord, foreignWord);
          }

          // compute parameter updates
          for (String englishWord: englishWords) {
            epsilon = (PAlignment*t_given_e_of_f.getCount(englishWord, foreignWord)) / (PNull*t_given_e_of_f.getCount(NULL_WORD, foreignWord) + PAlignment*Z);
            count_of_e_and_f.incrementCount(englishWord, foreignWord, epsilon);
          }

          // compute parameter update for NULL
          epsilon = (PNull*t_given_e_of_f.getCount(NULL_WORD, foreignWord)) /
                  (PNull*t_given_e_of_f.getCount(NULL_WORD, foreignWord) + PAlignment*Z);
          count_of_e_and_f.incrementCount(NULL_WORD, foreignWord, epsilon);
        }
      }

      // conditionally normalize model
      t_given_e_of_f = Counters.conditionalNormalize(count_of_e_and_f);

      // System.out.println(t_given_e_of_f);
    }
  }

}

