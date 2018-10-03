package ai2018.group28;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import genius.core.Bid;
import genius.core.analysis.BidPoint;
import genius.core.analysis.BidSpace;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.SortedOutcomeSpace;


public class Group28_BS extends OfferingStrategy {

    double PHASE_1_THRESHOLD = 0.9;
    double PHASE_1_END_TIME = 0.3;
    double MAX_STEP_SIZE = 0.2;
    double PHASE_2_BIDS_AMOUNT = 20;
    /** Outcome space */
    private SortedOutcomeSpace outcomespace;

    /**
     * Method which initializes the agent by setting all parameters. The
     * parameter of concession rate must have been initialized.
     */
    @Override
    public void init(NegotiationSession negoSession, OpponentModel model, OMStrategy oms, Map<String, Double> parameters) throws Exception {
        super.init(negoSession, parameters);
        if (parameters.get("cnc") != null) {
            this.negotiationSession = negoSession;

            outcomespace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
            negotiationSession.setOutcomeSpace(outcomespace);

            this.MAX_STEP_SIZE = parameters.get("cnc");

            if (parameters.get("threshold") != null)
                this.PHASE_1_THRESHOLD = parameters.get("threshold");

            this.opponentModel = model;
            this.omStrategy = oms;
        } else {
            throw new Exception("Constant \"cnc\" for the concession speed was not set.");
        }
    }

    @Override
    public BidDetails determineOpeningBid() {
        BidDetails firstBid = null;
        try {
            firstBid = this.negotiationSession.getMaxBidinDomain();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return firstBid;
    }

    /**
     * Simple offering strategy which retrieves the target utility and looks for
     * the nearest bid if no opponent model is specified. If an opponent model
     * is specified, then the agent return a bid according to the opponent model
     * strategy.
     */
    @Override
    public BidDetails determineNextBid() {
        // if there is no opponent model then
        // nextBid = negotiationSession.getOutcomeSpace().getBidNearUtility(utilityGoal);
        // else
        // nextBid = omStrategy.getBid(outcomespace, utilityGoal);

        double time = this.negotiationSession.getTime();
        BidDetails nextBid = null;
        if (time < PHASE_1_END_TIME) {
            nextBid = getRandomBid(PHASE_1_THRESHOLD);
        } else {
            System.out.println("I got in");
            nextBid = phase2CreateBid();
            System.out.println("I got out");
        }
        return nextBid;
    }

    public BidDetails phase2CreateBid() {
        double currTime = this.negotiationSession.getTime();
        double currThreshold = PHASE_1_THRESHOLD - MAX_STEP_SIZE * (currTime - PHASE_1_END_TIME);
        BidPoint kalaiSmordinskyPoint = null;
        try {
            BidSpace bidSpace = new BidSpace(this.negotiationSession.getUtilitySpace(),this.opponentModel.getOpponentUtilitySpace(), false, true);
            kalaiSmordinskyPoint = bidSpace.getKalaiSmorodinsky();
            //System.out.println("Calculated kalaiSmorodinsky");
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayList<BidDetailsExtended> bidDetails = new ArrayList<BidDetailsExtended>();
        for (int i = 0; i < PHASE_2_BIDS_AMOUNT; i++) {
            BidDetails randomBid = getRandomBid(currThreshold);
            System.out.println("Calculated kalaiSmorodinsky 1");
            //BidDetailsExtended bidDetailsExtended = new BidDetailsExtended(randomBid, kalaiSmordinskyPoint, getBidPoint(randomBid));
            bidDetails.add(new BidDetailsExtended(randomBid, kalaiSmordinskyPoint, getBidPoint(randomBid)));
            System.out.println("Calculated kalaiSmorodinsky 2");
        }
        bidDetails.sort(Comparator.comparingDouble(BidDetailsExtended::getDistance));
        System.out.println("Calculated kalaiSmorodinsky");

        return bidDetails.get(0).getBidDetails();
    }

    private BidDetails getRandomBid(double threshold) {
        BidDetails randomBid = null;
        do {
            randomBid = getRandomBid();
        } while (randomBid.getMyUndiscountedUtil() < threshold);
        return randomBid;
    }

    private BidDetails getRandomBid() {
        Bid bid = this.negotiationSession.getUtilitySpace().getDomain().getRandomBid(null);
        return new BidDetails(bid, this.negotiationSession.getUtilitySpace().getUtility(bid), this.negotiationSession.getTime());
    }

    private BidPoint getBidPoint(BidDetails bidDetails) {
        //Not sure how it will work for more opponents
        return new BidPoint(bidDetails.getBid(),bidDetails.getMyUndiscountedUtil(), this.opponentModel.getBidEvaluation(bidDetails.getBid()));
    }

    public NegotiationSession getNegotiationSession() {
        return negotiationSession;
    }

    @Override
    public Set<BOAparameter> getParameterSpec() {
        Set<BOAparameter> set = new HashSet<BOAparameter>();
        set.add(new BOAparameter("cnc", 0.2, "Concession rate"));
        set.add(new BOAparameter("threshold", 0.9, "Initial Threshold"));

        return set;
    }

    @Override
    public String getName() {
        return "Group28 Bidding Strategy";
    }
}

class BidDetailsExtended {
    private BidDetails bidDetails;
    private double distance;

    BidDetailsExtended(BidDetails bidDetails, BidPoint kalaiSmaridnskyPoint, BidPoint bidPoint) {
        this.bidDetails = bidDetails;
        this.distance = kalaiSmaridnskyPoint.getDistance(bidPoint);
    }

    public double getDistance() {
        return this.distance;
    }

    public BidDetails getBidDetails() {
        return this.bidDetails;
    }
}