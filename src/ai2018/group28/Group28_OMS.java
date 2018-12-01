package ai2018.group28;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.OMStrategy;

import java.util.*;

public class Group28_OMS extends OMStrategy {
    double updateThreshold = 1.1D;
    double timeWindow = 0.2;

    /**
     * This method evaluates a list of bids and chooses the best one in certain situation.
     * The algorithm works as follows:
     * 1. Check if opponent is conceding.
     * 2a. If so then choose the best bid for you.
     * 2b. Else, choose the best bid for the opponent, by using the opponent model, 
     *     to encourage it to offer us later a better bid for us
     *
     * @param bidsInRange , the list of bid that are considered for offering
     * @return the bid we select from the given ones
     */

    @Override
    public BidDetails getBid(List<BidDetails> bidsInRange) {

        BidDetails bestBid;
        
        //If the given list of bids has only one bid, the choose this one
        if (bidsInRange.size() == 1) {
            return bidsInRange.get(0);
        }

        //Check if the opponent is conceding
        boolean isConceding = checkConceding();

        //Decide which is the best bid according to value of isConceding boolean
        if (isConceding) {
            bestBid = selfishMove(bidsInRange);
        } else {
            bestBid = encouragingMove(bidsInRange);
        }
        return bestBid;
    }

    @Override
    public boolean canUpdateOM() {
        return this.negotiationSession.getTime() < this.updateThreshold;
    }

    @Override
    public String getName() {
        return "Group 28 OMS";
    }

    /**
     * This method is used when the opponent is conceding.
     * It chooses the bid which is the most in our favor(highest utility for our agent).
     * If evaluations somehow fail, we choose a random bid among the given ones.
     * @param bidsInRange , the list of bid that are considered for offering
     * @return the bid we select from the given ones  
     */
    public BidDetails selfishMove(List<BidDetails> bidsInRange) {

        double bestUtil = -1;
        BidDetails bestBid = bidsInRange.get(0);
        boolean allWereZero = false;

        for (BidDetails bid : bidsInRange) {
        	// Get the evaluation of the bid for our agent
            double evaluation = bid.getMyUndiscountedUtil();

            if (evaluation > 0.0001) {
                allWereZero = false;
            }
            // Find the one with highest utility
            if (evaluation > bestUtil) {
                bestBid = bid;
                bestUtil = evaluation;
            }
        }
        if (allWereZero) {
            return chooseRandom(bidsInRange);
        }
        return bestBid;
    }

    /**
     * This method is used when the opponent is not concedeing.
     * It chooses the  bid which is the most in opponent favor, by estimating 
     * its utility for the opponent by using the Opponent Model Strategy.
     * If evaluations somehow fail, we choose a random bid among the given ones
     * @param bidsInRange , the list of bid that are considered for offering
     * @return the bid we select from the given ones  
     */
    public BidDetails encouragingMove(List<BidDetails> bidsInRange) {

        double bestUtil = -1;
        BidDetails bestBid = bidsInRange.get(0);
        boolean allWereZero = true;

        for (BidDetails bid : bidsInRange) {
        	// Get the evaluation of the bid for the opponent by using the OM
            double evaluation = model.getBidEvaluation(bid.getBid());

            if (evaluation > 0.0001) {
                allWereZero = false;
            }
            // Find the one with highest utility
            if (evaluation > bestUtil) {
                bestBid = bid;
                bestUtil = evaluation;
            }
        }

        if (allWereZero) {
            return chooseRandom(bidsInRange);
        }
        return bestBid;
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
     * @return if agent is conceding
     */
    public boolean checkConceding() {
        List<Boolean> isConcedingList = new ArrayList<>();
        double currTime = negotiationSession.getTime();
        List<BidDetails> windowBids = negotiationSession.getOpponentBidHistory().filterBetweenTime(currTime - timeWindow, currTime).getHistory();

        // Find the number of conceding moves within the window
        for (int i = 0; i < windowBids.size() - 1; i++) {
            double firstUtil = model.getBidEvaluation(windowBids.get(i).getBid());
            double seccondUtil = model.getBidEvaluation(windowBids.get(i + 1).getBid());

            Boolean Conceding = new Boolean(firstUtil > seccondUtil);
            isConcedingList.add(Conceding);
        }

        //Compare the utility of its last move with the one of the first
        double startWindowUtil = model.getBidEvaluation(windowBids.get(0).getBid());
        double endWindowUtil = model.getBidEvaluation(windowBids.get(windowBids.size() - 1).getBid());

        //Check if it concedes in overall
        Boolean Conceding = new Boolean(startWindowUtil > endWindowUtil);
        isConcedingList.add(Conceding);
        long numOfConceding = isConcedingList.stream().filter(p -> p.booleanValue() == true).count();
        return numOfConceding > windowBids.size() / 2;
    }

    /** Method chooses random bid from
     * @param bidsInRange
     * @return
     */
    public BidDetails chooseRandom(List<BidDetails> bidsInRange) {
        Random r = new Random();
        return bidsInRange.get(r.nextInt(bidsInRange.size()));
    }

    public Set<BOAparameter> getParameterSpec() {
        HashSet parameters = new HashSet();
        parameters.add(new BOAparameter("t", 1.1D, "Time after which the OM should not be updated"));
        return parameters;
    }

}
