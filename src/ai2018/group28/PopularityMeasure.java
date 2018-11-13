package ai2018.group28;

public class PopularityMeasure implements Comparable {
    private long aggregatedPoints;
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

    public long calculateWeight() {
        return aggregatedPoints * numberOfAppearances;
    }

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
