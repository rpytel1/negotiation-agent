package ai2018.group28;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.OMStrategy;

import java.util.*;

public class Group28_OMS extends OMStrategy {
    double updateThreshold = 1.1D;
    double timeWindow = 0.2;

    /**
     * Method evaluating list of bids and choosing the best one in certain situation.
     * Algorithm works as follows:
     * 1. check if opponent is conceding
     * 2a. if so then choose best bid for you
     * 2b. else choose best bid for opponent to encourage him to later offer us better bid for us
     *
     * @param bidsInRange
     * @return
     */

    @Override
    public BidDetails getBid(List<BidDetails> bidsInRange) {

        BidDetails bestBid;

        if (bidsInRange.size() == 1) {
            return bidsInRange.get(0);
        }

        boolean isConciding = checkConceeding();

        if (isConciding) {
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
     * This method chooses bid which is the most in our favour(highest utility for our agent)
     * if evaluations somehow fails choose random bid
     * @param bidsInRange
     * @return
     */
    public BidDetails selfishMove(List<BidDetails> bidsInRange) {
        double bestUtil = -1;
        BidDetails bestBid = bidsInRange.get(0);
        boolean allWereZero = false;

        for (BidDetails bid : bidsInRange) {
            double evaluation = bid.getMyUndiscountedUtil();

            if (evaluation > 0.0001) {
                allWereZero = false;
            }

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
     * Method choosing bid which is the most in opponent favour
     *  if evaluations somehow fails choose random bid
     * @param bidsInRange
     * @return
     */
    public BidDetails encouragingMove(List<BidDetails> bidsInRange) {

        double bestUtil = -1;
        BidDetails bestBid = bidsInRange.get(0);
        boolean allWereZero = false;

        for (BidDetails bid : bidsInRange) {
            double evaluation = model.getBidEvaluation(bid.getBid());

            if (evaluation > 0.0001) {
                allWereZero = false;
            }

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
     * Method checking if agent is conceding or not.
     * It calculates how many conceeding moves agent did in certain time window, comparing one move and the following one.
     * At the end igt also compares if agent is conceding comparing first move of the window and the last.
     * If the value is greater than half of the moves we say that agent is conceding.
     *
     * @return if agent is conceeding
     */
    public boolean checkConceeding() {
        List<Boolean> isConceedingList = new ArrayList<>();
        double currTime = negotiationSession.getTime();
        List<BidDetails> windowBids = negotiationSession.getOpponentBidHistory().filterBetweenTime(currTime - timeWindow, currTime).getHistory();

        ///check between if it is conceeding
        for (int i = 0; i < windowBids.size() - 1; i++) {
            double firstUtil = model.getBidEvaluation(windowBids.get(i).getBid());
            double seccondUtil = model.getBidEvaluation(windowBids.get(i + 1).getBid());

            Boolean conceeding = new Boolean(firstUtil < seccondUtil);
            isConceedingList.add(conceeding);
        }

        //check if it conceeds in overall
        double startWindowUtil = model.getBidEvaluation(windowBids.get(0).getBid());
        double endWindowUtil = model.getBidEvaluation(windowBids.get(windowBids.size() - 1).getBid());

        Boolean conceeding = new Boolean(startWindowUtil < endWindowUtil);
        isConceedingList.add(conceeding);
        long numOfConceeding = isConceedingList.stream().filter(p -> p.booleanValue() == true).count();
        return numOfConceeding > windowBids.size() / 2;
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
