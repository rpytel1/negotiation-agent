package ai2018.group28;

import genius.core.analysis.BidPoint;
import genius.core.bidding.BidDetails;

public class BidDetailsExtended {
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
