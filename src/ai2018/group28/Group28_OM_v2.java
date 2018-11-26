package ai2018.group28;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Objective;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.utility.EvaluatorDiscrete;

/**
 * The Opponent Model adopts the Frequency Analysis model
 * of the HeardHeaded agent with some changes in the update 
 * phase of the issue weights. Instead of updating the issue
 * weights at every bid received from the opponent a window
 * of k bids is used and the difference between the frequency distributions 
 * of the values for each issue in two consecutive windows is calculated.
 * If that difference is lower than a constant value then the issue is 
 * considered as unchanged and it is added in a set of unchanged issues for 
 * the current update round. After that the number of the issues in this set is checked 
 * and if at least one issue was changed and a concession in that issue is observed during 
 * the window then the weights of the unchanged issues gets updated. The update value is 
 * given by the formula learnCoef*(1-t^beta))
 */

public class Group28_OM_v2 extends OpponentModel{
	/*
	 * the learning coefficient is the weight that is added each turn to the
	 * issue weights which changed. It's a trade-off between concession speed
	 * and accuracy. This weight is also combined with the time of the negotiation 
	 * meaning that it decays over time so that the problem of the HardHeaded Frequency
	 * model of being less accurate as time passes is minimized.
	 */
	private double learnCoef;
	/*
	 * value which is added to a value if it is found. Determines how fast the
	 * value weights converge.
	 */
	private int learnValueAddition;
	private int amountOfIssues; // the amount of issues in the domain
	private double goldenValue;
	private double beta; // a constant used in the calculation of the update weight
	private int window; // the size of the window used 
	private int currNumOfWindows;
	
	// HashMap used to store the frequency distribution of the previous window
	private HashMap<Issue,HashMap<ValueDiscrete,Double>> FreqOld; 
	// HashMap used to store the frequency distribution of the current window
	private HashMap<Issue,HashMap<ValueDiscrete,Double>> FreqNew;

	@Override
	public void init(NegotiationSession negotiationSession,
			Map<String, Double> parameters) {
		this.negotiationSession = negotiationSession;
		if (parameters != null && parameters.get("l") != null) {
			learnCoef = parameters.get("l");
		} else {
			learnCoef = 0.2;
		}
		learnValueAddition = 1;
		if (parameters != null && parameters.get("beta") != null)
			beta = parameters.get("beta");
		else
			beta = 0.7;
		if (parameters != null && parameters.get("w") != null)
			window = parameters.get("w").intValue();
		else
			window = 5;
		currNumOfWindows = 0;
		opponentUtilitySpace = (AdditiveUtilitySpace) negotiationSession
				.getUtilitySpace().copy();
		amountOfIssues = opponentUtilitySpace.getDomain().getIssues().size();
		
		goldenValue = learnCoef / amountOfIssues;

		initializeModel();
		FreqOld = new HashMap<Issue,HashMap<ValueDiscrete,Double>>();
		FreqNew = new HashMap<Issue,HashMap<ValueDiscrete,Double>>();
		initializeFrequencies(0); // 0 as input so that both frequency tables are initialized

	}

	@Override
	public void updateModel(Bid opponentBid, double time) {
		// if the there are less than 2 bids in the negotiation then the model isn't updated
		if (negotiationSession.getOpponentBidHistory().size() < 2) {
			return;
		}
		int numberOfUnchanged = 0; // number of unchanged issues for this update round
		Set<Issue> unchanged = new HashSet<Issue>(); // the set of unchanged issues
		List<BidDetails> lastKBids = new ArrayList<BidDetails>(); // the last bids in the current window
		BidDetails oppBid;
		boolean concession = false; // variable to check if the opponent conceded in the window
		
		// Now we check if there are at least two windows available so that the comparison can be made 
		if (negotiationSession.getOpponentBidHistory().size() >= window*(currNumOfWindows+1)) {
			for (int i=0; i<window; i++){
				oppBid = negotiationSession.getOpponentBidHistory()
						.getHistory()
						.get(negotiationSession.getOpponentBidHistory().size() - window + i);
				lastKBids.add(oppBid);
			} // we add the last window bids in the list 
			updateFreqNew(lastKBids); // and update the FreqNew HashMap
			//printFreq();
			if (currNumOfWindows!=0){
				double p;
				for (Entry<Objective, Evaluator> e : opponentUtilitySpace
						.getEvaluators()){
					opponentUtilitySpace.unlock(e.getKey());
					Issue iss = (Issue) e.getKey();
					// we conduct the similarity for each issue in the domain
					p = SimilarityTest(iss);
					// if the result of the test is lower than a certain value then the issue is 
					// considered unchanged
					if (p<0.02){
						unchanged.add(iss);
						numberOfUnchanged++;
					}
					else{
						double s1 = 0;
						double s2 = 0;
						int n = 0;
						// if the issue has changed then we check if a concession has been made
						// by estimating the utility of this issue in both the windows
						for (ValueDiscrete vd : ((IssueDiscrete) iss).getValues()){
							try {
								s1 += FreqNew.get(iss).get(vd)*((EvaluatorDiscrete) e.getValue()).getEvaluation(vd);
							} catch (Exception e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							try {
								s2 += FreqOld.get(iss).get(vd)*((EvaluatorDiscrete) e.getValue()).getEvaluation(vd);
							} catch (Exception e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							n++;
						}
						if (s1/n<s2/n) concession = true; // if the approximated utilities show concession 
														  // then the concession flag is set to True
					}
				}
				goldenValue = learnCoef*(1-Math.pow(negotiationSession.getTime(),beta)); // the update value
				double totalSum = 1+numberOfUnchanged*goldenValue;
				if (!unchanged.isEmpty() && concession){ // if an issue was changed and there was at least one
														 // concession observed in an issue then the issue weights 
														 // are updated
					for (Issue i : opponentUtilitySpace.getDomain().getIssues()) {
						double weight = opponentUtilitySpace.getWeight(i);
						double newWeight;
						if (unchanged.contains(i)) {
							newWeight = (weight + goldenValue) / totalSum;
						} else {
							newWeight = weight / totalSum;
						}
						opponentUtilitySpace.setWeight(i, newWeight);
					}
				}
			}
			currNumOfWindows++;
			updateFreqOld();	
		}
		// Then for each issue value that has been offered last time, a constant
		// value is added to its corresponding ValueDiscrete.
		oppBid = negotiationSession.getOpponentBidHistory()
				.getHistory()
				.get(negotiationSession.getOpponentBidHistory().size() - 1);
		try {
			for (Entry<Objective, Evaluator> e : opponentUtilitySpace
					.getEvaluators()) {
				EvaluatorDiscrete value = (EvaluatorDiscrete) e.getValue();
				IssueDiscrete issue = ((IssueDiscrete) e.getKey());
				/*
				 * add constant learnValueAddition to the current preference of
				 * the value to make it more important
				 */
				ValueDiscrete issuevalue = (ValueDiscrete) oppBid.getBid()
						.getValue(issue.getNumber());
				Integer eval = value.getEvaluationNotNormalized(issuevalue);
				value.setEvaluation(issuevalue, (learnValueAddition + eval));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	// Just a helper printing method used for debugging reasons
	public void printFreq(){
		System.out.println("FreqNew has:");
		for (Issue i:FreqNew.keySet()){
			System.out.println("Issue "+ i.getName());
			for (Value v:FreqNew.get(i).keySet()){
				System.out.println("Value "+ v.toString() + " with frequncy "+ FreqNew.get(i).get(v));
			}
		}
		System.out.println("FreqOld has:");
		for (Issue i:FreqOld.keySet()){
			System.out.println("Issue "+ i.getName());
			for (Value v:FreqOld.get(i).keySet()){
				System.out.println("Value "+ v.toString() + " with frequncy "+ FreqOld.get(i).get(v));
			}
		}
	}

	@Override
	public double getBidEvaluation(Bid bid) {
		double result = 0;
		try {
			result = opponentUtilitySpace.getUtility(bid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public String getName() {
		return "Group 28 Opponent Model";
	}

	@Override
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("l", 0.2,
				"The learning coefficient determines how quickly the issue weights are learned"));
		set.add(new BOAparameter("beta", 0.7,
				"A constant used in the calculation of the update of the issue weights. Should be under 1"));
		set.add(new BOAparameter("w", 5.0,
				"The size of the window used"));
		return set;
	}

	/* Init to flat weight and flat evaluation distribution
	 */
	private void initializeModel() {
		double commonWeight = 1D / amountOfIssues;

		for (Entry<Objective, Evaluator> e : opponentUtilitySpace
				.getEvaluators()) {

			opponentUtilitySpace.unlock(e.getKey());
			e.getValue().setWeight(commonWeight);
			try {
				// set all value weights to one (they are normalized when
				// calculating the utility)
				for (ValueDiscrete vd : ((IssueDiscrete) e.getKey())
						.getValues())
					((EvaluatorDiscrete) e.getValue()).setEvaluation(vd, 1);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	/* Method used to initialize the frequency HashMaps of the two windows
	 * It uses a flag to check if also the frequency HashMap of the previous 
	 * window is initialized too.
	 */
	private void initializeFrequencies(int flag){
		HashMap<ValueDiscrete,Double> temp;
		try {
			for (Issue i : opponentUtilitySpace.getDomain().getIssues()) {
				temp = new HashMap<ValueDiscrete,Double>();
				for (ValueDiscrete vd : ((IssueDiscrete) i).getValues()){
					temp.put(vd, 0.0);
				}
				if (flag == 0)FreqOld.put(i, temp);
				FreqNew.put(i, temp);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/* Method used to update the frequency HashMap of the previous window by just assigning the
	 * values of the frequency HashMap of the current window to the previous one
	 */
	private void updateFreqOld(){
		FreqOld.clear();
		for (Issue i: FreqNew.keySet()){
			FreqOld.put(i, FreqNew.get(i));
		}
	}
	
	/* Method used to update the frequency HashMap of the current window by taking as input  
	 * a number of window bids and incrementing the values of each issue in these bids by the
	 * number of times they appeared during the window
	 */
	private void updateFreqNew(List<BidDetails> bids){
		initializeFrequencies(1);
		for (Issue i : opponentUtilitySpace.getDomain().getIssues()) {
			HashMap<Value,Integer> temp = new HashMap<Value,Integer>();
			for (BidDetails b:bids){
				Value value1 = b.getBid().getValue(i.getNumber());
				if (temp.containsKey(value1)){
					temp.put(value1, temp.get(value1)+1);
				}
				else{
					temp.put(value1, 1);
				}
			}
			HashMap<ValueDiscrete,Double> t = FreqNew.get(i);
			for (Value v: temp.keySet()){
				// normalization of the times a value appeared using Laplace Smoothing
				double f = ((double)(temp.get(v)+1))/(window+FreqNew.get(i).size()); 
				t.put((ValueDiscrete) v, f);
			}
			FreqNew.put(i,t);
		}
	}
	
	/* Method used to calculate the similarity between the distributions of the values 
	 * of issue i in the two windows being under examination
	 */
	private double SimilarityTest(Issue i){
		HashMap<ValueDiscrete,Double> t1 = FreqOld.get(i);
		HashMap<ValueDiscrete,Double> t2 = FreqNew.get(i);
		double s=0.0;
		for (ValueDiscrete vd:t1.keySet()){
				s = s + Math.pow(t1.get(vd)-t2.get(vd),2); // the squared difference of the number of 
		}												   // times value vd appeared in each window
		return s/FreqOld.get(i).size();
	}
	
}
