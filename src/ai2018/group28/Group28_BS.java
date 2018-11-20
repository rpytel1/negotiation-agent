package ai2018.group28;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

/**
 * The Bidding strategy is composed of 2 phases:
 * The 1st phase lasts until PHASE_1_END_TIME and in this phase random bids are generated above
 * PHASE_1_THRESHOLD constant threshold.
 * In the 2nd phase a new time dependent threshold value is calculated and a number of PHASE_2_BIDS_AMOUNT
 * random bids above this threshold are generated. From these bids the closest to the kalaiSmordinsky point
 * is offered to the opponent as the next bid
 */

public class Group28_BS extends OfferingStrategy {

    /**
     * The constant threshold value for the 1st phase
     */
    double phase1Threshold = 0.93;
    double tolerance = 0.02;
    /**
     * End time parameter of phase 1
     */
    double phase1EndTime = 0.1;
    /**
     * Concession rate
     */
    double cncRate = 0.02;
    /**
     * Number of bids generated in 2nd phase
     */
    double phase2BidsNum = 100;
    /**
     * The minimum utility value
     */
    double minUtil;
    /**
     * k parameter used in the time dependent threshold formula
     */
    double k = 0.05;
    /**
     * Parameter used in the third term of the time dependent threshold formula
     */
    double extraStep = 0.07;
    /**
     * Outcome space
     */
    private SortedOutcomeSpace outcomespace;

    /**
     * Method which initializes the agent by setting all parameters. The
     * parameter of concession rate must have been initialized.
     */
    @Override
    public void init(NegotiationSession negoSession, OpponentModel model, OMStrategy oms, Map<String, Double> parameters) throws Exception {
        super.init(negoSession, parameters);
        this.negotiationSession = negoSession;
        outcomespace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
        negotiationSession.setOutcomeSpace(outcomespace);

        if (parameters.get("cnc") != null)
            this.cncRate = parameters.get("cnc");

        if (parameters.get("threshold") != null)
            this.phase1Threshold = parameters.get("threshold");

        if (parameters.get("k") != null)
            this.k = parameters.get("k");

        if (parameters.get("extra") != null)
            this.extraStep = parameters.get("extra");

        this.minUtil = negoSession.getMinBidinDomain().getMyUndiscountedUtil();
        this.opponentModel = model;
        this.omStrategy = oms;
    }

    @Override
    public BidDetails determineOpeningBid() {
        BidDetails firstBid = null;
        try {
            // The first bid is the one with the maximum utility in the domain
            firstBid = this.negotiationSession.getMaxBidinDomain();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return firstBid;
    }

    /**
     * Method that determines next bid depending on the current time of the negotiation
     * It splits the negotiation time in 2 parts and a different strategy is used in
     * each one of them
     */
    @Override
    public BidDetails determineNextBid() {
        double time = this.negotiationSession.getTime();
        BidDetails nextBid = null;
        if (time < phase1EndTime) {
            nextBid = getRandomBid(phase1Threshold);
        } else {
            nextBid = phase2CreateBid();
        }
        return nextBid;
    }

    /**
     * Method used to determine the next bid in the 2nd phase of the bidding strategy
     * It calculates the time dependent currThreshold value above which phase2BidsNum random bids are
     * created. From this set the closest bid to the kalaiSmordinskyPoint is selected as the next bid
     */
    public BidDetails phase2CreateBid() {
        double currTime = this.negotiationSession.getTime();
        double currThreshold = minUtil + (1 - f(currTime)) * (phase1Threshold - minUtil) - extraStep * (currTime - phase1EndTime);
        BidPoint kalaiSmordinskyPoint = null;
        try {
            BidSpace bidSpace = new BidSpace(this.negotiationSession.getUtilitySpace(), this.opponentModel.getOpponentUtilitySpace(), false, true);
            kalaiSmordinskyPoint = bidSpace.getKalaiSmorodinsky();
        } catch (Exception e) {
            e.printStackTrace();
        }

        /**
         * BidDetailsExtended class is created to hold info about the details and the distance from
         * kalaiSmordinskyPoint of each bid generated. In this section a list of phase2BidsNum random bids
         * above currThreshold are created.
         */
        ArrayList<BidDetailsExtended> bidDetailsExtended = new ArrayList<BidDetailsExtended>();
        for (int i = 0; i < phase2BidsNum; i++) {
            BidDetails randomBid = getRandomBid(currThreshold);
            double oponnentUtility = opponentModel.getBidEvaluation(randomBid.getBid());
            bidDetailsExtended.add(new BidDetailsExtended(randomBid, kalaiSmordinskyPoint, getBidPoint(randomBid), oponnentUtility));
        }
        bidDetailsExtended.sort(Comparator.comparingDouble(BidDetailsExtended::getMeasure));

        List<BidDetails> bidDetails = bidDetailsExtended.stream()
                .map(p -> p.getBidDetails())
                .filter(distinctByKey(p->p.getBid()))
                .collect(Collectors.toList());
        BidDetails bestPreviousBid = negotiationSession.getOpponentBidHistory().getBestBidDetails();
        System.out.println(bidDetails.size()+ ":"+(int)(bidDetails.size()*0.75));
        BidDetails omStrategyBid = omStrategy.getBid(bidDetails.subList(0,(int)(bidDetails.size()*0.75)));

        return bestPreviousBid.getMyUndiscountedUtil() > omStrategyBid.getMyUndiscountedUtil()
                ? bestPreviousBid
                : omStrategyBid;

    }
    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
    /**
     * Method used to create random bids above the given threshold
     * If the 2nd phase has initiated then the created bids are in the
     * range [threshold, phase1Threshold]
     * The getRandomBid() method is called to actually generate a random bid
     */
    private BidDetails getRandomBid(double threshold) {
        BidDetails randomBid = null;
        if (threshold == phase1Threshold) {
            do {
                randomBid = getRandomBid();
            } while (randomBid.getMyUndiscountedUtil() < threshold);
        } else {
            do {
                randomBid = getRandomBid();
            }
            while (randomBid.getMyUndiscountedUtil() < threshold || randomBid.getMyUndiscountedUtil() > phase1Threshold + tolerance);
        }
        return randomBid;
    }

    private BidDetails getRandomBid() {
        Bid bid = this.negotiationSession.getUtilitySpace().getDomain().getRandomBid(null);
        return new BidDetails(bid, this.negotiationSession.getUtilitySpace().getUtility(bid), this.negotiationSession.getTime());
    }

    /**
     * Method used to generate the position of a bid on the utility graph
     */
    private BidPoint getBidPoint(BidDetails bidDetails) {
        return new BidPoint(bidDetails.getBid(), bidDetails.getMyUndiscountedUtil(), this.opponentModel.getBidEvaluation(bidDetails.getBid()));
    }

    /**
     * Time dependent function used in the calculation of the time dependent
     * threshold. The same function is also used by the HardHeaded Agent in
     * ANAC 2011.
     */
    public double f(double t) {
        if (cncRate == 0)
            return k;
        double ft = k + (1 - k) * Math.pow(t, 1.0 / cncRate);
        return ft;
    }

    public NegotiationSession getNegotiationSession() {
        return negotiationSession;
    }

    @Override
    public Set<BOAparameter> getParameterSpec() {
        Set<BOAparameter> set = new HashSet<BOAparameter>();
        set.add(new BOAparameter("cnc", 0.02,
                "Concession rate for the second phase of bidding"));
        set.add(new BOAparameter("threshold", 0.9,
                "The threshold used in the first phase"));
        set.add(new BOAparameter("k", 0.05,
                "Adjustment parameter used in the time dependent threshold"));
        set.add(new BOAparameter("extra", 0.05,
                "Adjustment parameter used in the time dependent threshold"));
        return set;
    }

    @Override
    public String getName() {
        return "Group28 Bidding Strategy";
    }
}