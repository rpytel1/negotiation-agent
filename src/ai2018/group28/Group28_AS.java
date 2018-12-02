package ai2018.group28;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import genius.core.BidHistory;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.Actions;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Value;

/**
 * This Acceptance Condition will accept an opponent bid in the following cases:
 * if 99% of time has already passed
 * if the negotiation time is under 85% of the deadline time -> Group28_AC = AC_Const || AC_Next || AC_Prev
 * if the negotiation time is over 85% of the deadline time -> Group28_AC = AC_Combi_MaxW
 */

public class Group28_AS extends AcceptanceStrategy {
     /**
      *
      */
    private double a;
     /**
      * The constant value that is multiplied with an negative exponential function to make a decaying threshold over time
      */    
    private double acc_const;
     /**
      * The constant threshold value for last case (99% of time has already passed)
      */
    private double time_const;
     /**
      * The value that determines the size of the window to check if the opponent concedes
      */
    double timeWindow = 0.2;
     /**
      * Empty constructor for the BOA framework.
      */
    public Group28_AS() {
    }
     /**
      * 
      */
    public Group28_AS(NegotiationSession negoSession, OfferingStrategy strat, double alpha, double threshold, double time) {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;
        this.a = alpha;
        this.acc_const = threshold;
        this.time_const = time;
    }
    /**
     * Method which initializes the agent by setting all parameters.
     */
    @Override
    public void init(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel opponentModel,
                     Map<String, Double> parameters) throws Exception {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;

        if (parameters.get("a") != null || parameters.get("threshold") != null || parameters.get("time") != null) {
            a = parameters.get("a");
            acc_const = parameters.get("threshold");
            time_const = parameters.get("time");
        } else {
            a = 1.02;
            acc_const = 0.9;
            time_const = 0.99;
        }
    }
     /**
      * Method which prints the agent's parameters.
      */
    @Override
    public String printParameters() {
        String str = "[a: " + a + " threshold: " + acc_const + " time: " + time_const + "]";
        return str;
    }
     /**
      * Acceptance strategy
      */
    @Override
   public Actions determineAcceptability() {
        // if 99% of time has already passed
        if (negotiationSession.getTime() >= time_const){ 
            return Actions.Accept;
        }
    	else if (negotiationSession.getTime() <= 0.85){
            if (negotiationSession.getOpponentBidHistory() != null && negotiationSession.getOwnBidHistory().getLastBidDetails() != null){
                double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();
                double lastMyBidUtil = negotiationSession.getOwnBidHistory().getLastBidDetails().getMyUndiscountedUtil();
                if (lastOpponentBidUtil * a >= lastMyBidUtil || lastOpponentBidUtil * a >= offeringStrategy.getNextBid().getMyUndiscountedUtil() || lastOpponentBidUtil * a >= acc_const ) {
                	return Actions.Accept;
                }
            }
            return Actions.Reject;
        }
        else{
            // first if clause only for time reasons - to accept more quickly
        	if (!checkConceding()) return Actions.Reject;
            if (negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil() >= offeringStrategy.getNextBid().getMyUndiscountedUtil() && negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil()>= 2.1*Math.exp(-negotiationSession.getTime())) {
            	System.out.println(2.1*Math.exp(-negotiationSession.getTime()));
            	return Actions.Accept;}
            double offeredUndiscountedUtility = negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();
            double now = negotiationSession.getTime();
            double window = 1 - now;
            BidHistory recentBids = negotiationSession.getOpponentBidHistory().filterBetweenTime(now - window, now);
            double max;
            if (recentBids.size() > 0) max = recentBids.getBestBidDetails().getMyUndiscountedUtil();
            else max = 0;
            if (offeredUndiscountedUtility >= max && offeredUndiscountedUtility>= 2.1*Math.exp(-now)) {
            	System.out.println(2.1*Math.exp(-now));
            	return Actions.Accept;}
            return Actions.Reject;
        }
    }
    /**
     * This method checks if the opponent is conceding or not.
     * It calculates how many conceding moves agent it did in certain time window,
     * comparing every move with the following one.
     * At the end, it also check if the opponent is conceding by comparing the first
     * move of the window with the last one.
     * If the number of conceding moves is greater than the half of the moves of the
     * window, we conclude that the opponent is conceding.
     *
     */
    public boolean checkConceding() {
        List<Boolean> isConcedingList = new ArrayList<>();
        double currTime = negotiationSession.getTime();
        List<BidDetails> windowBids = negotiationSession.getOpponentBidHistory().filterBetweenTime(currTime - timeWindow, currTime).getHistory();

        ///check between if it is conceding
        for (int i = 0; i < windowBids.size() - 1; i++) {

            double firstUtil = windowBids.get(i).getMyUndiscountedUtil();
            double seccondUtil = windowBids.get(i + 1).getMyUndiscountedUtil();

            Boolean conceding = new Boolean(firstUtil < seccondUtil);
            isConcedingList.add(conceding);
        }

        //check if it concedes in overall
        double startWindowUtil = windowBids.get(0).getMyUndiscountedUtil();
        double endWindowUtil = windowBids.get(windowBids.size() - 1).getMyUndiscountedUtil();

        Boolean conceding = new Boolean(startWindowUtil < endWindowUtil);
        isConcedingList.add(conceding);
        long numOfConceding = isConcedingList.stream().filter(p -> p.booleanValue() == true).count();
        return numOfConceding > windowBids.size() / 2;
    }

    @Override
    public Set<BOAparameter> getParameterSpec() {

        Set<BOAparameter> set = new HashSet<BOAparameter>();
        set.add(new BOAparameter("a", 1.02,
                "Accept when the opponent's utility * a is greater than the utility of our current or last bid"));
        set.add(new BOAparameter("threshold", 0.9,
                "Accept when the opponent's utility * a is greater than the threshold utility"));
        set.add(new BOAparameter("time", 0.99,
                "Accept any offer after 99% of time has passed"));
        return set;
    }

    @Override
    public String getName() {
        return "Group28 AC Strategy";
    }
}