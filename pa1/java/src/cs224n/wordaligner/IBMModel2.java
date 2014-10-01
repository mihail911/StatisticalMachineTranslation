package cs224n.wordaligner;

import cs224n.util.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Random;

public class IBMModel2 implements WordAligner{

  //TODO: HANDLE THE NULL ALIGNMENT CASE!!!


  // t(f|e): {e, f} note that this is actually indexed in reverse
  // the probability of producing translation f given word e
  private CounterMap<String,String> t_given_e_of_f =
              new CounterMap<String,String>();
  // C(e, f): {e, f}
  // the accumulator for fractional counts of coocurring english and french words
  private CounterMap<String,String> count_of_e_and_f =
              new CounterMap<String,String>();

  //This counter will store distortion probabilities: q(i|j,l,m)
  private static ArrayList<ArrayList<CounterMap<Integer, Integer>>> q_j_given_i
              = new ArrayList<ArrayList<CounterMap<Integer,Integer>>>();

  private static ArrayList<ArrayList<CounterMap<Integer, Integer>>> count_j_given_i
              = new ArrayList<ArrayList<CounterMap<Integer,Integer>>>();

  private static int numIterations = 5;
  private static int nullIndex = -1;
  private static Double PNull = 0.2;

  /**
   * Determines the optimal alignment for the given sentence pair.
   */
  public Alignment align(SentencePair sentencePair){
    Alignment alignments = new Alignment();

    // beware: there is a flip here
    List<String> englishWords = sentencePair.getSourceWords();
    List<String> foreignWords = sentencePair.getTargetWords();

    int foreignIndex = 0;
    for (int j=0; j<foreignWords.size(); j++) {
      String foreignWord = foreignWords.get(j);
      Double maxScoreSoFar = -1.0;
      int indexOfBestAlignment = -1;
      int englishIndex = 0;

      for (int i=0; i<englishWords.size(); i++) {
        String englishWord = englishWords.get(i);
        Double score = (1 - PNull) * q_j_given_i.get(englishWords.size()-1).get(foreignWords.size()-1).getCount(i, j) * t_given_e_of_f.getCount(englishWord, foreignWord);
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

    return alignments;
  }

  /**
   * Trains the model using the algorithm for IBM Model 2.
   * We first estimate the parameters t(e|f) using M1 and then we run
   * the algorithm provided in Figure 2 of the Collins' handout.
   */
  public void train(List<SentencePair> trainingdata){
    int maxFSentLength = 0;
    int maxESentLength = 0;
    Random randGen = new Random();


    //NOTE: WHEN ACCESSING q_j_given_i ArrayList REMEMEMBER INDEX
    // OFF BY 1 for USING SENTENCE LENGTHS
    for (SentencePair pair: trainingdata) {
      // beware: there is a flip here
      List<String> englishWords = pair.getSourceWords();
      List<String> foreignWords = pair.getTargetWords();
      int engLength = englishWords.size();
      int forLength = foreignWords.size();

      if(engLength > maxESentLength)
        maxESentLength = engLength;
      if(forLength > maxFSentLength)
        maxFSentLength = forLength;
    }

    //Initialize q_j_given_i array with empty counters
    for(int i = 0; i < maxESentLength; ++i){
      q_j_given_i.add(new ArrayList<CounterMap<Integer,Integer>>());
      count_j_given_i.add(new ArrayList<CounterMap<Integer,Integer>>());

      for(int j = 0; j < maxFSentLength; ++j){
        q_j_given_i.get(i).add(new CounterMap<Integer,Integer>());
        count_j_given_i.get(i).add(new CounterMap<Integer,Integer>());
      }
    }

    // Initialize all counters for training samples.
    for(SentencePair sentence: trainingdata){
      List<String> englishWords = sentence.getSourceWords();
      List<String> foreignWords = sentence.getTargetWords();
      int engLength = englishWords.size();
      int forLength = foreignWords.size();

      for (int j=0; j<forLength; j++) {
        String foreignWord = foreignWords.get(j);
        for (int i=0; i<engLength; i++) {
          String englishWord = englishWords.get(i);
          q_j_given_i.get(engLength-1).get(forLength-1).
                  setCount(i, j, 1.0);
          t_given_e_of_f.setCount(englishWord, foreignWord, 1.0); // TODO: should really use Model 1
        }
        t_given_e_of_f.setCount(NULL_WORD, foreignWord, 1.0); // TODO: should really use Model 1
      }
    }

    // normalize initial distributions
    for (int lenF=0; lenF<maxFSentLength; lenF++)
      for (int lenE=0; lenE<maxESentLength; lenE++)
        q_j_given_i.get(lenE).set(lenF, Counters.conditionalNormalize(q_j_given_i.get(lenE).get(lenF)));
    t_given_e_of_f = Counters.conditionalNormalize(t_given_e_of_f);

    // TODO: should really check that q_j_given_i is normalized properly

    //RUN EM to determine new parameters t(e|f) and q(j|i,l,m)
    for (int iter = 0; iter < numIterations; ++iter) {
      for (SentencePair pair: trainingdata) {
        // beware: there is a flip here
        List<String> englishWords = pair.getSourceWords();
        List<String> foreignWords = pair.getTargetWords();
        int engLength = englishWords.size();
        int forLength = foreignWords.size();

        Double Z  = 0.0;
        Double epsilon = 0.0;
        Double epsilon_numerator = 0.0;
        Double epsilon_denominator = 0.0;
        for (int j = 0; j<forLength; j++) {
          String foreignWord = foreignWords.get(j);
          // precompute normalization constant for parameter update
          Z = 0.0;
          for (int i = 0; i<engLength; i++) {
            String englishWord = englishWords.get(i);
            Z += (1-PNull) * q_j_given_i.get(engLength-1).get(forLength-1).getCount(i,j) *
                    t_given_e_of_f.getCount(englishWord, foreignWord);
          }

          epsilon_denominator = PNull * t_given_e_of_f.getCount(NULL_WORD, foreignWord) + Z;

          // Compute parameter updates
          for (int i = 0; i<engLength; i++) {
            String englishWord = englishWords.get(i);
            epsilon_numerator = (1-PNull) * (q_j_given_i.get(engLength-1).get(forLength-1).getCount(i,j)*
                    t_given_e_of_f.getCount(englishWord, foreignWord));
            epsilon = epsilon_numerator/epsilon_denominator;

            count_of_e_and_f.incrementCount(englishWord, foreignWord, epsilon);
            count_j_given_i.get(engLength-1).get(forLength-1).incrementCount(i,j,epsilon);
          }

          // Compute parameter updates for NULL
          epsilon_numerator = PNull * t_given_e_of_f.getCount(NULL_WORD, foreignWord);
          epsilon = epsilon_numerator/epsilon_denominator;

          count_of_e_and_f.incrementCount(NULL_WORD, foreignWord, epsilon);
        }
      }
      // conditionally normalize model
      for (int lenF=0; lenF<maxFSentLength; lenF++)
        for (int lenE=0; lenE<maxESentLength; lenE++)
          q_j_given_i.get(lenE).set(lenF, Counters.conditionalNormalize(q_j_given_i.get(lenE).get(lenF)));
      t_given_e_of_f = Counters.conditionalNormalize(count_of_e_and_f);
    }

    //System.out.println(t_given_e_of_f);
    //System.out.println(q_j_given_i);
  }

}
