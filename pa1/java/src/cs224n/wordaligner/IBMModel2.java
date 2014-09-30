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
        Double score = q_j_given_i.get(engLength).get(forLength).
                    getCount(englishWord,foreignWord) * t_given_e_of_f.getCount(englishWord, foreignWord);
        if (score > maxScoreSoFar) {
          maxScoreSoFar = score;
          indexOfBestAlignment = englishIndex;
        }
        englishIndex += 1;
      }

      //TODO: FIX COMPUTATION OF NULL SCORE
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

  public void train(List<SentencePair> trainingdata){
    int maxFSentLength = 0;
    int maxESentLength = 0;
    Random randGen = new Random();



    //REMEMBER TO TRAIN t(e|f) USING MODEL 1
    //--------------------------------------

    //Literally copy-paste Victor's code right here

    //--------------------------------------



    //Iterate over all training data to find max sentence lengths
    for(SentencePair sentence: trainingdata){
      List<String> englishWords = sentence.getSourceWords();
      List<String> foreignWords = sentence.getTargetWords();
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

    //Randomly initialize all counters for training samples.
    for(SentencePair sentence: trainingdata){
      List<String> englishWords = sentence.getSourceWords();
      List<String> foreignWords = sentence.getTargetWords();
      int engLength = englishWords.size();
      int forLength = foreignWords.size();

      for (String foreignWord: foreignWords) {
        for (String englishWord: englishWords) {
          q_j_given_i.get(engLength).get(forLength).
                  setCount(englishWord,foreignWord, randGen.nextDouble());
        }
        q_j_given_i.get(engLength).get(forLength).
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
        Double PAlignment = (1-PNull)/engLength;

        Double Z  = 0.0;
        Double epsilon = 0.0;
        for (String foreignWord: foreignWords) {
          // precompute normalization constant for parameter update
          Z = 0.0;
          for (String englishWord: englishWords) {
            Z += (t_given_e_of_f.getCount(englishWord, foreignWord) *
                  q_j_given_i.get(engLength).get(forLength).
                    getCount(englishWord,foreignWord));
          }

          // compute parameter updates
          for (String englishWord: englishWords) {
            epsilon = (q_j_given_i.get(engLength).get(forLength).
                    getCount(englishWord,foreignWord)*
                    t_given_e_of_f.getCount(englishWord, foreignWord)) 
                    / (t_given_e_of_f.getCount(NULL_WORD, foreignWord));

            count_of_e_and_f.incrementCount(englishWord, foreignWord, epsilon);
            q_j_given_i.get(engLength).get(forLength).
                          incrementCount(englishWord,foreignWord,epsilon);
          }

          // Compute parameter updates for NULL

        }
      }
      // conditionally normalize model
      t_given_e_of_f = Counters.conditionalNormalize(count_of_e_and_f);

      //Update distortion prob as well
      for(int e = 0; e < maxESentLength; ++e){
        for(int f = 0; f < maxFSentLength; ++f){

          q_j_given_i.get(e).set(f, Counters.conditionalNormalize(count_j_given_i.get(e).get(f)));
        }
      }
    }
  }

}
