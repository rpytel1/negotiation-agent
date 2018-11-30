package ai2018.group28;

/**
 * This helper class aggregates both number of appearances of certain Value and as well points
 * - for bids higher in the ranking they are worth more points
 */
public class PopularityMeasure implements Comparable {
    /**
     * aggregated number of Points
     */
    private long aggregatedPoints;
    /**
     * number of appearences
     */
    private int numberOfAppearances;

    public PopularityMeasure() {
        aggregatedPoints = 0;
        numberOfAppearances = 0;
    }

    public long getAggregatedPoints() {
        return aggregatedPoints;
    }

    public void setAggregatedPoints(long aggregatedPoints) {
        this.aggregatedPoints = aggregatedPoints;
    }

    public int getNumOfAppearences() {
        return numberOfAppearances;
    }

    public void setNumOfAppearences(int numOfAppearences) {
        this.numberOfAppearances = numOfAppearences;
    }

    /**
     * method that calculates weight of the value by returning product of aggregatedPoints and numberOfAppearances
     * @return
     */
    public long calculateWeight() {
        return aggregatedPoints * numberOfAppearances;
    }

    /**
     * method that enables comparing two objects of class Popularity Measure
     * @param o
     * @return
     */
    @Override
    public int compareTo(Object o) {
        PopularityMeasure other = (PopularityMeasure) o;
        if (other.calculateWeight() < this.calculateWeight()) {
            return -1;
        }
        if (other.calculateWeight() == this.calculateWeight()) {
            return 0;
        }
        return 1;
    }
}
