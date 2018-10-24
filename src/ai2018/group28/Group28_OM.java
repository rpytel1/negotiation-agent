package ai2018.group28;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
 * considered as unchanged and it gets updated.
 */
public class Group28_OM extends OpponentModel {

	/*
	 * the learning coefficient is the weight that is added each turn to the
	 * issue weights which changed. It's a trade-off between concession speed
	 * and accuracy.
	 */
	private double learnCoef;
	/*
	 * value which is added to a value if it is found. Determines how fast the
	 * value weights converge.
	 */
	private int learnValueAddition;
	private int amountOfIssues;
	private double goldenValue;
	HashMap<Issue,HashMap<Value,Integer>> freqOld, freqNew;
	private int window = 5;
	private double diffThreshold;

	@Override
	public void init(NegotiationSession negotiationSession,
			Map<String, Double> parameters) {
		this.negotiationSession = negotiationSession;
		if (parameters != null){ 
			if(parameters.get("l") != null) {
				learnCoef = parameters.get("l");
			} else {
				learnCoef = 0.2;
			}
			if (parameters != null && parameters.get("threshold") != null) {
				diffThreshold = parameters.get("threshold");
			} else {
				diffThreshold = 0.1;
			}
		}
		learnValueAddition = 1;
		opponentUtilitySpace = (AdditiveUtilitySpace) negotiationSession
				.getUtilitySpace().copy();
		amountOfIssues = opponentUtilitySpace.getDomain().getIssues().size();
		/*
		 * This is the value to be added to weights of unchanged issues before
		 * normalization. Also the value that is taken as the minimum possible
		 * weight, (therefore defining the maximum possible also).
		 */
		goldenValue = learnCoef / amountOfIssues;
		
		initializeModel();

	}

	@Override
	public void updateModel(Bid opponentBid, double time) {
		int numberOfUnchanged = 0;
		// A set used to hold the issues that are considered unchanged
		// in every update of the model
		Set<Issue> unchanged = new HashSet<Issue>();
		if (negotiationSession.getOpponentBidHistory().size() < 1){
			return;
		}
		// The update of the values is done after every bid proposed by the opponent
		BidDetails oppBid = negotiationSession.getOpponentBidHistory()
				.getHistory()
				.get(negotiationSession.getOpponentBidHistory().size() - 1);
		// Then for each issue value that has been offered last time, a constant
		// value is added to its corresponding ValueDiscrete.
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
		// The case when the number of bids permits the existence of 2 windows 
		// of bids. Thus, the frequencies in both windows are calculated.
		if (negotiationSession.getOpponentBidHistory().size() >= 2*window) {
			
			BidDetails tmp;
			double diff;
			int n;
			freqNew = initFreqVector();
			for (int i = 1; i < window + 1; i++){
				tmp = negotiationSession.getOpponentBidHistory()
						.getHistory()
						.get(negotiationSession.getOpponentBidHistory().size() - i);
				for (Issue e : opponentUtilitySpace.getDomain().getIssues()) {
					Value value1 = tmp.getBid().getValue(e.getNumber());
					HashMap<Value,Integer> temp = freqNew.get(e);
					temp.put(value1, temp.get(value1)+1);
					freqNew.replace(e, temp);
				}	
			}
			freqOld = initFreqVector();
			for (int i = window+1; i < 2*window + 1; i++){
				tmp = negotiationSession.getOpponentBidHistory()
						.getHistory()
						.get(negotiationSession.getOpponentBidHistory().size() - i);
				for (Issue e : opponentUtilitySpace.getDomain().getIssues()) {
					Value value1 = tmp.getBid().getValue(e.getNumber());
					HashMap<Value,Integer> temp = freqOld.get(e);
					temp.put(value1, temp.get(value1)+1);
					freqOld.replace(e, temp);
				}	
			}
			for (Issue e : opponentUtilitySpace.getDomain().getIssues()){
				diff = 0;
				n = 0;
				for (Value v: freqOld.get(e).keySet()){
					n = n + 1;
					diff = diff + Math.abs(freqOld.get(e).get(v)/window-freqNew.get(e).get(v)/window);
				}
				diff = diff/n;
				if (diff<diffThreshold){
					unchanged.add(e);
					numberOfUnchanged++;
				}
			}
		}
		// The case when the number of bids permits the existence of only 1 window
		// of bids. Thus, the frequencies in that window is calculated so that it can 
		// be available when a second window is formed.
		else if (negotiationSession.getOpponentBidHistory().size() >= window){
			BidDetails tmp;
			freqOld = initFreqVector();
			for (int i = 1; i < window + 1; i++){
				tmp = negotiationSession.getOpponentBidHistory()
						.getHistory()
						.get(negotiationSession.getOpponentBidHistory().size() - i);
				for (Issue e : opponentUtilitySpace.getDomain().getIssues()) {
					Value value1 = tmp.getBid().getValue(e.getNumber());
					HashMap<Value,Integer> temp = freqOld.get(e);
					temp.put(value1, temp.get(value1)+1);
					freqOld.replace(e, temp);
				}	
			}
		}
		else
			return;
		
		// The update part of the issue weights 
		
		// The total sum of weights before normalization.
		double totalSum = 1D + goldenValue * numberOfUnchanged;
		// The maximum possible weight
		double maximumWeight = 1D - (amountOfIssues) * goldenValue / totalSum;
		for (Issue e : opponentUtilitySpace.getDomain().getIssues()){
			double weight = opponentUtilitySpace.getWeight(e.getNumber());
			double newWeight;
			if (unchanged.contains(e) && weight < maximumWeight){
				newWeight = (weight + goldenValue) / totalSum;
			}
			else{
				newWeight = weight / totalSum;
			}
			opponentUtilitySpace.setWeight(e, newWeight);
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
		set.add(new BOAparameter("threshold", 0.1,
				"The threshold below of which the difference between the frequency distribution "
				+ "of an issue in two windows is considered minimal"));
		return set;
	}

	/**
	 * Init to flat weight and flat evaluation distribution
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
	
	/**
	 * Initialization of the frequency HashMap of a window
	 */
	public HashMap<Issue,HashMap<Value,Integer>> initFreqVector(){
		HashMap<Issue,HashMap<Value,Integer>> inp = new HashMap<Issue,HashMap<Value,Integer>>();
		for (Issue i : opponentUtilitySpace.getDomain().getIssues()) {
			inp.put(i, new HashMap<Value,Integer>());
			HashMap<Value,Integer> temp = new HashMap<Value,Integer>();
			for (Value vd : ((IssueDiscrete) i).getValues())
				temp.put(vd, 0);
			inp.replace(i, temp);
		}
		return inp;
	}
}