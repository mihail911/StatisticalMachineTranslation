package cs224n.wordaligner;

import cs224n.util.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class IBMModel1 implements WordAligner{

  private Counter<String> foreignCount = new Counter<String>();
  private CounterMap<String,String> coocurrenceCount =
              new CounterMap<String,String>();
  private CounterMap<String,String> translationProb =
              new CounterMap<String,String>();

  Integer numIterations = 5;

	public Alignment align(SentencePair sentencePair){
    return new Alignment();
	}

	public void train(List<SentencePair> trainingdata){

    for (SentencePair pair: trainingdata) {
      List<String> englishWords = pair.getTargetWords();
      List<String> foreignWords = pair.getSourceWords();

      foreignWords.add(0, NULL_WORD);

      System.out.println(englishWords);
      System.out.println(foreignWords);

      for (String foreignWord: foreignWords) {
        for (String englishWord: englishWords) {
          translationProb.incrementCount(foreignWord, englishWord, 1.0);
        }
      }

      foreignWords.remove(0);
    }

    Set<String> foreignWordsSet = translationProb.keySet();

    for (String foreignWord: foreignWordsSet) {
      Counter<String> englishWordCounters = translationProb.getCounter(foreignWord);
      Double englishWordPairs = englishWordCounters.totalCount();
      Set<String> englishWords = englishWordCounters.keySet();
      for(String englishWord: englishWords) {
        Double oldCount = englishWordCounters.getCount(englishWord);
        translationProb.setCount(foreignWord, englishWord, oldCount/englishWordPairs);
      }
    }

    for (int s = 0; s < numIterations; s += 1) {
      // clear counts
      coocurrenceCount = new CounterMap<String,String>();
      foreignCount = new Counter<String>();

      for (SentencePair pair: trainingdata) {
        List<String> englishWords = pair.getTargetWords();
        List<String> foreignWords = pair.getSourceWords();

        foreignWords.add(0, NULL_WORD);

        for (String foreignWord: foreignWords) {
          for (String englishWord: englishWords) {
            Counter<String> englishWordCounters = translationProb.getCounter(foreignWord);
            Double englishWordPairs = englishWordCounters.totalCount();
            Double delta = translationProb.getCount(foreignWord, englishWord) / englishWordPairs;

            Double oldCount = coocurrenceCount.getCount(foreignWord, englishWord);
            coocurrenceCount.setCount(foreignWord, englishWord, oldCount + delta);
            oldCount = foreignCount.getCount(foreignWord);
            foreignCount.setCount(foreignWord, oldCount + delta);
          }
        }

        foreignWords.remove(0);
      }

      for (String foreignWord: foreignWordsSet) {
        Counter<String> englishWordCounters = translationProb.getCounter(foreignWord);
        Double englishWordPairs = englishWordCounters.totalCount();
        Set<String> englishWords = englishWordCounters.keySet();

        Double fCount = foreignCount.getCount(foreignWord);

        for(String englishWord: englishWords) {
          Double cCount = coocurrenceCount.getCount(foreignWord, englishWord);

          translationProb.setCount(foreignWord, englishWord, cCount/fCount);
        }
      }

      System.out.println(translationProb);
    }



	}

}
