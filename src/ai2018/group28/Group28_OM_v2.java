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

public class Group28_OM_v2 extends OpponentModel{
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
	private double beta;
	private int window;
	private int currNumOfWindows;
	private HashMap<Issue,HashMap<ValueDiscrete,Double>> FreqOld;
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
		beta = 0.9;
		window = 5;
		currNumOfWindows = 0;
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
		FreqOld = new HashMap<Issue,HashMap<ValueDiscrete,Double>>();
		FreqNew = new HashMap<Issue,HashMap<ValueDiscrete,Double>>();
		initializeFrequencies(0);

	}

	@Override
	public void updateModel(Bid opponentBid, double time) {
		if (negotiationSession.getOpponentBidHistory().size() < 2) {
			return;
		}
		int numberOfUnchanged = 0;
		Set<Issue> unchanged = new HashSet<Issue>();
		List<BidDetails> lastKBids = new ArrayList<BidDetails>();
		BidDetails oppBid;
		boolean concession = false;
		if (negotiationSession.getOpponentBidHistory().size() >= window*(currNumOfWindows+1)) {
			for (int i=0; i<window; i++){
				oppBid = negotiationSession.getOpponentBidHistory()
						.getHistory()
						.get(negotiationSession.getOpponentBidHistory().size() - window + i);
				lastKBids.add(oppBid);
			}
			updateFreqNew(lastKBids);
			//printFreq();
			if (currNumOfWindows!=0){
				double p;
				for (Entry<Objective, Evaluator> e : opponentUtilitySpace
						.getEvaluators()){
					opponentUtilitySpace.unlock(e.getKey());
					Issue iss = (Issue) e.getKey();
					p = SimilarityTest(iss);
					if (p<0.03){
						unchanged.add(iss);
						numberOfUnchanged++;
					}
					else{
						double s1 = 0;
						double s2 = 0;
						int n = 0;
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
						if (s1/n<s2/n) concession = true;
					}
				}
				goldenValue = learnCoef*(1-Math.pow(negotiationSession.getTime(),beta));
				double totalSum = 1+numberOfUnchanged*goldenValue;
				if (!unchanged.isEmpty() && concession){
					System.out.println("Weights updated");
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
		return "Group 28 Revised Opponent Model";
	}

	@Override
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("l", 0.2,
				"The learning coefficient determines how quickly the issue weights are learned"));
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
	
	private void updateFreqOld(){
		FreqOld.clear();
		for (Issue i: FreqNew.keySet()){
			FreqOld.put(i, FreqNew.get(i));
		}
	}
	
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
				double f = ((double)(temp.get(v)+1))/(window+FreqNew.get(i).size());
				t.put((ValueDiscrete) v, f);
			}
			FreqNew.put(i,t);
		}
	}
	
	private double SimilarityTest(Issue i){
		HashMap<ValueDiscrete,Double> t1 = FreqOld.get(i);
		HashMap<ValueDiscrete,Double> t2 = FreqNew.get(i);
		double s=0.0;
		for (ValueDiscrete vd:t1.keySet()){
				s = s + Math.pow(t1.get(vd)-t2.get(vd),2);
		}
		return s/FreqOld.get(i).size();
	}
	
}
