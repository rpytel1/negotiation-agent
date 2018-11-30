package ai2018.group28;

import genius.core.analysis.BidPoint;
import genius.core.bidding.BidDetails;

/**
 * The aim of this class is to aggregate both BidDetails entity and its measure by which we grade bids
 */
public class BidDetailsExtended {
    private BidDetails bidDetails;
    /**
     * measure by which we grade the bids. The smaller the better.
     */
    private double measure;
    /**
     * parameter by which the importance of our utility compared to the oponnent utility is indicated
     */
    final double UTILITY_A_IMPORTANCE = 0.8;

    BidDetailsExtended(BidDetails bidDetails, BidPoint kalaiSmaridnskyPoint, BidPoint bidPoint, double opponentUtility) {
        this.bidDetails = bidDetails;

        //Here we calculate the product of the utilities. Utilities are substracted from one to have as small
        // as possible meassure for attractive bids.
        // Later we multiply this product by the distance to Kalai-Smardinsky point.
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
