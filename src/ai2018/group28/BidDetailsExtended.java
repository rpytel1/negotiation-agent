package ai2018.group28;

import genius.core.analysis.BidPoint;
import genius.core.bidding.BidDetails;

public class BidDetailsExtended {
    private BidDetails bidDetails;
    private double measure;
    final double UTILITY_A_IMPORTANCE = 0.8;

    BidDetailsExtended(BidDetails bidDetails, BidPoint kalaiSmaridnskyPoint, BidPoint bidPoint, double opponentUtility) {
        this.bidDetails = bidDetails;

        double measure = (1D - UTILITY_A_IMPORTANCE * bidDetails.getMyUndiscountedUtil()) * (1D - opponentUtility);
        this.measure = kalaiSmaridnskyPoint.getDistance(bidPoint) * measure;
    }

    public double getMeasure() {
        return this.measure;
    }

    public BidDetails getBidDetails() {
        return this.bidDetails;
    }
}
