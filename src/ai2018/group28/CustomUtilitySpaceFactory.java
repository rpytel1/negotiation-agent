package ai2018.group28;

import genius.core.Bid;
import genius.core.Domain;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Objective;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.utility.EvaluatorDiscrete;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The aim of this class was to introduce our own ideas in creating UtilitySpace in uncertain environment.
 */
public class CustomUtilitySpaceFactory extends AdditiveUtilitySpaceFactory {
    /**
     * UtilitySpace created by the the factory.
     */
    private AdditiveUtilitySpace u;
    private Map<Issue, HashMap<ValueDiscrete, PopularityMeasure>> popularityMap;

    /**
     * Generates an simple Utility Space on the domain, with equal weights and zero values.
     * Everything is zero-filled to already have all keys contained in the utility maps.
     *
     * @param d
     */
    public CustomUtilitySpaceFactory(Domain d) {
        super(d);
        popularityMap = initPopularityMap(d);
        List<Issue> issues = d.getIssues();
        int noIssues = issues.size();
        Map<Objective, Evaluator> evaluatorMap = new HashMap<Objective, Evaluator>();
        for (Issue i : issues) {
            IssueDiscrete issue = (IssueDiscrete) i;
            EvaluatorDiscrete evaluator = new EvaluatorDiscrete();
            evaluator.setWeight(1.0 / noIssues);
            for (ValueDiscrete value : issue.getValues()) {
                evaluator.setEvaluationDouble(value, 0.0);
            }
            evaluatorMap.put(issue, evaluator);
        }

        u = new AdditiveUtilitySpace(d, evaluatorMap);

    }

    /**
     * This method is estimating values of each issue based on the BidRanking provided.
     * This method works as follows it iterates over all bids in their order, over each issue and then it was storing
     * points for each value (number of points added is increasing while getting to the end) and as well number of occurrences
     * For each occurance of certain value evaluator is updated by adding product of aggregated points and number of appearences
     * @param r
     */
    @Override
    public void estimateUsingBidRanks(BidRanking r) {
        int points = 0;
        for (Bid b : r.getBidOrder()) {
            List<Issue> issues = b.getIssues();
            for (Issue i : issues) {
                int no = i.getNumber();
                ValueDiscrete v = (ValueDiscrete) b.getValue(no);
                HashMap<ValueDiscrete, PopularityMeasure> currPopularityForIssueMap = popularityMap.get(i);
                PopularityMeasure currentPopularityMeasure = popularityMap.get(i).get(v);
                currentPopularityMeasure.setAggregatedPoints(currentPopularityMeasure.getAggregatedPoints() + points);
                currentPopularityMeasure.setNumOfAppearences(currentPopularityMeasure.getNumOfAppearences() + 1);
                currPopularityForIssueMap.put(v, currentPopularityMeasure);
                popularityMap.put(i, currPopularityForIssueMap);
                setUtility(i, v, currentPopularityMeasure.calculateWeight());
            }
            points += 1;
        }

        List<Issue> issues = getDomain().getIssues();
        for (Issue i : issues) {
            for (ValueDiscrete vd : ((IssueDiscrete) i).getValues()) {
                PopularityMeasure popularityMeasure = popularityMap.get(i).get(vd);
                setUtility(i, vd, popularityMeasure.calculateWeight());
            }
        }

        normalizeWeightsByMaxValues();
    }

    /**
     * Helper method to update evaluator of certain issue for certain value
     * @param i
     * @param v
     * @param val
     */
    @Override
    public void setUtility(Issue i, ValueDiscrete v, double val) {
        EvaluatorDiscrete evaluator = (EvaluatorDiscrete) u.getEvaluator(i);
        if (evaluator == null) {
            evaluator = new EvaluatorDiscrete();
            u.addEvaluator(i, evaluator);
        }
        evaluator.setEvaluationDouble(v, val);

    }

    /**
     * Method to normalize evaluators of the values by maximal value for certain issue
     */
    @Override
    public void normalizeWeightsByMaxValues() {
        for (Issue i : getIssues()) {
            EvaluatorDiscrete evaluator = (EvaluatorDiscrete) u.getEvaluator(i);
            evaluator.normalizeAll();
        }
        for (Issue i : getIssues()) {
            EvaluatorDiscrete evaluator = (EvaluatorDiscrete) u.getEvaluator(i);
            evaluator.scaleAllValuesFrom0To1();
        }
        u.normalizeWeights();
    }

    /**
     * Helper method to create Popularity Map for all issues and their values
     * @param domain
     * @return
     */
    private HashMap<Issue, HashMap<ValueDiscrete, PopularityMeasure>> initPopularityMap(Domain domain) {
        HashMap<Issue, HashMap<ValueDiscrete, PopularityMeasure>> inp = new HashMap<>();

        for (Issue i : domain.getIssues()) {
            inp.put(i, new HashMap<>());
            HashMap<ValueDiscrete, PopularityMeasure> temp = new HashMap<>();
            for (ValueDiscrete vd : ((IssueDiscrete) i).getValues())
                temp.put(vd, new PopularityMeasure());
            inp.replace(i, temp);
        }

        return inp;
    }

    @Override
    public AdditiveUtilitySpace getUtilitySpace() {
        return u;
    }

    private List<Issue> getIssues() {
        return getDomain().getIssues();
    }

    private Domain getDomain() {
        return u.getDomain();
    }

}
