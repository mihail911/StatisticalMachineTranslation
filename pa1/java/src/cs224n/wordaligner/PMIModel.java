package cs224n.wordaligner;

import cs224n.util.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * Source file implementing a basic Pointwise mutual information model
 * for word alignment.
 */
public class PMIModel implements WordAligner {
	private Counter<String> srcWordProbability = new Counter<String>();
	private Counter<String> targetWordProbability = new Counter<String>();
	private CounterMap<String,String> simOccurCounts =
							new CounterMap<String,String>();

	/**
	 * For every sentence pair, computes the optimal alignment
	 * for every word in the target sentence based on the
	 * pointwise mutual information measure.
	 */
	public Alignment align(SentencePair sentencePair){
		List<String> targetWords = sentencePair.getTargetWords();
		List<String> srcWords = sentencePair.getSourceWords();

		// System.out.println(srcWords);
		// System.out.println(targetWords);

		//Make sure to add null word at beginning of sentence
		//srcWords.add(0, NULL_WORD);
		Alignment alignment = new Alignment();

		//Don't actually need scores given how we are computing max
		//List<Double> scores = new ArrayList<Double>();
		final int sizeTargets = targetWords.size();
		final int sizeSrc = srcWords.size();
		for(int targetIndex = 0; targetIndex < sizeTargets;
							++targetIndex){
			//scores.clear();
			String target = targetWords.get(targetIndex);
			Double maxSoFar = -1.0;
			int indexOfMax = -1;

			for(int srcIndex = 0; srcIndex < sizeSrc; ++srcIndex){
				String src = srcWords.get(srcIndex);

				Double score = simOccurCounts.getCount(src,target)
							/(srcWordProbability.getCount(src)*
								targetWordProbability.getCount(target));

				//TODO: Erase me. All probabilities computed per sentence pair
				// System.out.println("source: "+ src + " " +
				// 	String.valueOf(srcWordProbability.getCount(src)));
				// System.out.println("target: "+ target+ " " +
				// 	String.valueOf(targetWordProbability.getCount(target)));
				// System.out.println("cooccur: "+
				// 	String.valueOf(simOccurCounts.getCount(src,target)));
				// System.out.println("score: "+ String.valueOf(score));
				// System.out.println("--------------");

				if (score > maxSoFar){
					maxSoFar = score;
					indexOfMax = srcIndex;
				}
			}

			// Only add an alignment if the best score is better than the NULL alignment
			Double null_score = simOccurCounts.getCount(NULL_WORD, target)
							/(srcWordProbability.getCount(NULL_WORD)*
								targetWordProbability.getCount(target));

			if (null_score < maxSoFar) {
				alignment.addPredictedAlignment(targetIndex,indexOfMax);
			}

		}
		// System.out.println(alignment);
		return alignment;
	}

	/**
	 * Trains the parameters of the model by computing
	 * the probability of each source word (# occurrences / total words)
	 * and similarly for target words. Also counts co-occurrences of
	 * all possible pairs of source and target words.
	 */
	public void train(List<SentencePair> trainingData){
		int numPairs = trainingData.size();

		for(SentencePair pair: trainingData){
			List<String> targetWords = pair.getTargetWords();
			List<String> srcWords = pair.getSourceWords();



			//Add an instance of the null word for the source counts (f_0)
			srcWords.add(0, NULL_WORD);

			for(String sword: srcWords){
				//For every instance of word, increment corresponding counter
				srcWordProbability.incrementCount(sword,1.0);
			}
			for(String tword: targetWords){
				targetWordProbability.incrementCount(tword,1.0);
				for(String sword: srcWords){
					simOccurCounts.incrementCount(sword, tword, 1.0);
				}
			}

			// remove the null word
			srcWords.remove(0);

			// TODO: erase me, sentences
			//System.out.println(srcWords);
			//System.out.println(targetWords);
		}

		//Normalize counts to get probabilities
		srcWordProbability = Counters.normalize(srcWordProbability);
		targetWordProbability = Counters.normalize(targetWordProbability);
		// TODO: erase me, unigram occurences
		//System.out.println(srcWordProbability);
		//System.out.println(targetWordProbability);

		//Iterate over key set of co-occurrence counter map and normalize
		//appropriately.
		Set<String> allKeys = simOccurCounts.keySet();
		Double totalCount = simOccurCounts.totalCount();
		for(String key: allKeys){
			Counter<String> allValues = simOccurCounts.getCounter(key);
			Set<String> vals = allValues.keySet();
			for(String v: vals){
				Double oldCount = allValues.getCount(v);
				simOccurCounts.setCount(key,v,oldCount/totalCount);
			}
		}

		// TODO: erase me, coocurrences
		// System.out.println(simOccurCounts);
	}
}
