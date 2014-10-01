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
  private static ArrayList<ArrayList<CounterMap<String, String>>> q_j_given_i
              = new ArrayList<ArrayList<CounterMap<String,String>>>();

  private static ArrayList<ArrayList<CounterMap<String, String>>> count_j_given_i
              = new ArrayList<ArrayList<CounterMap<String,String>>>();
  private static ArrayList<ArrayList<Counter<String>>> count_i
              = new ArrayList<ArrayList<Counter<String>>>();

  private static int numIterations = 5;
  private static Double PNull = 0.2;

  /**
   * Determines the optimal alignment for the given sentence pair.
   */
  public Alignment align(SentencePair sentencePair){
    Alignment alignments = new Alignment();
    // beware: there is a flip here
    List<String> englishWords = sentencePair.getSourceWords();
    List<String> foreignWords = sentencePair.getTargetWords();
    int engLength = englishWords.size();
    int forLength = foreignWords.size();

    int foreignIndex = 0;
    for (String foreignWord: foreignWords) {
      Double maxScoreSoFar = -1.0;
      int indexOfBestAlignment = -1;
      int englishIndex = 0; //TODO: What's the point of maintaining this counter?

      for (String englishWord: englishWords) {
        //System.out.println("Q(j|i) : "+ q_j_given_i.get(engLength-1).get(forLength-1));
        //System.out.println("T(e|f): " + t_given_e_of_f.getCount(englishWord, foreignWord));
        Double score = q_j_given_i.get(engLength-1).get(forLength-1).
                    getCount(englishWord,foreignWord) * t_given_e_of_f.getCount(englishWord, foreignWord);
        //System.out.println("SCORE: " + score);
        if (score > maxScoreSoFar) {
          maxScoreSoFar = score;
          indexOfBestAlignment = englishIndex;
        }
        englishIndex += 1;
      }

      //TODO: FIX COMPUTATION OF NULL SCORE
      Double nullScore = PNull * q_j_given_i.get(engLength-1).get(forLength-1).
                    getCount(NULL_WORD,foreignWord) *
                    t_given_e_of_f.getCount(NULL_WORD, foreignWord);
      //System.out.println("NULL: " + nullScore);

      if (nullScore < maxScoreSoFar) {
        //System.out.println("SOME ALIGNMENT ADDED");
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
      q_j_given_i.add(new ArrayList<CounterMap<String,String>>());
      count_j_given_i.add(new ArrayList<CounterMap<String,String>>());

      for(int j = 0; j < maxFSentLength; ++j){
        q_j_given_i.get(i).add(new CounterMap<String,String>());
        count_j_given_i.get(i).add(new CounterMap<String,String>());
      }
    }


    //TRAINING t(e|f) USING MODEL 1 ------------
    //--------------------------------------

    for (SentencePair pair: trainingdata) {
      // beware: there is a flip here
      List<String> englishWords = pair.getSourceWords();
      List<String> foreignWords = pair.getTargetWords();
      int engLength = englishWords.size();
      int forLength = foreignWords.size();

      for (String foreignWord: foreignWords) {
        for (String englishWord: englishWords) {
          // iterate over every word pair and record occurrence
          t_given_e_of_f.setCount(englishWord, foreignWord, 1.0);
        }
        // add a count for NULL
        t_given_e_of_f.setCount(NULL_WORD, foreignWord, 1.0);
        q_j_given_i.get(engLength-1).get(forLength-1).
                  setCount(NULL_WORD,foreignWord,1.0);
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
    }
    System.out.println("AFTER M1 TRAINING"+ t_given_e_of_f);
    //---------------------------------------------------------
    // END MODEL 1 TRAINING---------------------------




    //START TRAINING OF REMAINING PARAMETERS FOR M2

    //Randomly initialize all counters for training samples.
    for(SentencePair sentence: trainingdata){
      List<String> englishWords = sentence.getSourceWords();
      List<String> foreignWords = sentence.getTargetWords();
      int engLength = englishWords.size();
      int forLength = foreignWords.size();

      for (String foreignWord: foreignWords) {
        for (String englishWord: englishWords) {
          q_j_given_i.get(engLength-1).get(forLength-1).
                  setCount(englishWord,foreignWord, randGen.nextDouble());
        }
        q_j_given_i.get(engLength-1).get(forLength-1).
                  setCount(NULL_WORD, foreignWord, randGen.nextDouble());
      }
    }


    //RUN EM to determine new parameters t(e|f) and q(j|i,l,m)
    for (int iter = 0; iter < numIterations; ++iter) {
      for (SentencePair pair: trainingdata) {
        // beware: there is a flip here
        List<String> englishWords = pair.getSourceWords();
        List<String> foreignWords = pair.getTargetWords();
        int engLength = englishWords.size();
        int forLength = foreignWords.size();
        PAlignment = (1-PNull)/engLength;

        Z  = 0.0;
        epsilon = 0.0;
        for (String foreignWord: foreignWords) {
          // precompute normalization constant for parameter update
          Z = 0.0;
          for (String englishWord: englishWords) {
            Z += (t_given_e_of_f.getCount(englishWord, foreignWord) *
                  q_j_given_i.get(engLength-1).get(forLength-1).
                    getCount(englishWord,foreignWord));
          }

          // Compute parameter updates
          for (String englishWord: englishWords) {
            epsilon = (PAlignment * q_j_given_i.get(engLength-1).get(forLength-1).
                    getCount(englishWord,foreignWord)*
                    t_given_e_of_f.getCount(englishWord, foreignWord)) 
                    / (PNull * q_j_given_i.get(engLength-1).get(forLength-1).
                        getCount(NULL_WORD,foreignWord) * 
                        t_given_e_of_f.getCount(NULL_WORD, foreignWord)
                        + PAlignment * Z);

            count_of_e_and_f.incrementCount(englishWord, foreignWord, epsilon);
            q_j_given_i.get(engLength-1).get(forLength-1).
                        incrementCount(englishWord,foreignWord,epsilon);
          }

          // Compute parameter updates for NULL
          epsilon = (PNull * q_j_given_i.get(engLength-1).get(forLength-1).
                        getCount(NULL_WORD,foreignWord)*
                        t_given_e_of_f.getCount(NULL_WORD, foreignWord)) /
                  (PNull * q_j_given_i.get(engLength-1).get(forLength-1).
                        getCount(NULL_WORD,foreignWord) * 
                        t_given_e_of_f.getCount(NULL_WORD, foreignWord)
                        + PAlignment * Z);
          count_of_e_and_f.incrementCount(NULL_WORD, foreignWord, epsilon);
          q_j_given_i.get(engLength-1).get(forLength-1).
                        incrementCount(NULL_WORD,foreignWord,epsilon);

        }
      }
      // conditionally normalize model
      t_given_e_of_f = Counters.conditionalNormalize(count_of_e_and_f);
      System.out.println("Iter: "+ iter + t_given_e_of_f);

      // Normalize distortion values as well
      for(int e = 0; e < maxESentLength; ++e){
        for(int f = 0; f < maxFSentLength; ++f){
          q_j_given_i.get(e).set(f, 
              Counters.conditionalNormalize(count_j_given_i.get(e).get(f)));
        }
      }
    }


    System.out.println(t_given_e_of_f);
    System.out.println(q_j_given_i);
  }

}
