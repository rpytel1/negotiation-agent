package ai2018.group28;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import genius.core.BidHistory;
import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.Actions;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;

/**
 * This Acceptance Condition will accept an opponent bid in the following cases:
 * if 99% of time has already passed
 * if the negotiation time is under 75% of the deadline time -> Group28_AC = AC_Const || AC_Next || AC_Prev
 * if the negotiation time is over 75% of the deadline time -> Group28_AC = AC_Combi_MaxW
 */

public class Group28_AS extends AcceptanceStrategy {

    private double a;
    private double acc_const;
    private double time_const;

    /**
     * Empty constructor for the BOA framework.
     */
    public Group28_AS() {
    }

    public Group28_AS(NegotiationSession negoSession, OfferingStrategy strat, double alpha, double threshold, double time) {
        this.negotiationSession = negoSession;
        this.offeringStrategy = strat;
        this.a = alpha;
        this.acc_const = threshold;
        this.time_const = time;
    }

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

    @Override
    public String printParameters() {
        String str = "[a: " + a + " threshold: " + acc_const + " time: " + time_const + "]";
        return str;
    }

    @Override
    public Actions determineAcceptability() {
        if (negotiationSession.getTime() >= time_const){
            return Actions.Accept;
        }
        else if (negotiationSession.getTime() <= 0.75){
            if (negotiationSession.getOpponentBidHistory() != null && negotiationSession.getOwnBidHistory().getLastBidDetails() != null){
                double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();
                double lastMyBidUtil = negotiationSession.getOwnBidHistory().getLastBidDetails().getMyUndiscountedUtil();
                if (lastOpponentBidUtil * a >= lastMyBidUtil || lastOpponentBidUtil * a >= offeringStrategy.getNextBid().getMyUndiscountedUtil() || lastOpponentBidUtil * a >= acc_const) {
                    return Actions.Accept;
                }
            }
            return Actions.Reject;
        }
        else{
            // first if clause only for time reasons - to accept more quickly
            if (negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil() >= offeringStrategy.getNextBid().getMyUndiscountedUtil())
                return Actions.Accept;
            double offeredUndiscountedUtility = negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();
            double now = negotiationSession.getTime();
            double window = 1 - now;
            BidHistory recentBids = negotiationSession.getOpponentBidHistory().filterBetweenTime(now - window, now);
            double max;
            if (recentBids.size() > 0) max = recentBids.getBestBidDetails().getMyUndiscountedUtil();
            else max = 0;
            if (offeredUndiscountedUtility >= max) return Actions.Accept;
            return Actions.Reject;
        }
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