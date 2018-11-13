package ai2018.group28;

import genius.core.Bid;
import genius.core.Domain;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.ExperimentalUserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.UncertainAdditiveUtilitySpace;

import java.util.List;

public class Group28_UParty extends AbstractNegotiationParty {

    @Override
    public Action chooseAction(List<Class<? extends Action>> possibleActions) {
        this.log("This is the UncertaintyAgentExample.");
        this.log("The user model is: " + this.userModel);
        this.log("The default estimated utility space is: " + this.getUtilitySpace());
        Bid randomBid = this.getUtilitySpace().getDomain().getRandomBid(this.rand);
        this.log("The default estimate of the utility of a random bid + " + randomBid + " is: " + this.getUtility(randomBid));
        if (this.userModel instanceof ExperimentalUserModel) {
            this.log("You have given the agent access to the real utility space for debugging purposes.");
            ExperimentalUserModel experimentalUserModel = (ExperimentalUserModel) this.userModel;
            UncertainAdditiveUtilitySpace uncertainAdditiveUtilitySpace = experimentalUserModel.getRealUtilitySpace();
            this.log("The real utility space is: " + uncertainAdditiveUtilitySpace);
            this.log("The real utility of the random bid is: " + uncertainAdditiveUtilitySpace.getUtility(randomBid));
        }

        if (this.getLastReceivedAction() instanceof Offer) {
            Bid lastBid = ((Offer) this.getLastReceivedAction()).getBid();
            List bidRanking = this.userModel.getBidRanking().getBidOrder();
            if (bidRanking.contains(lastBid)) {
                double lastBidId = (double) (bidRanking.size() - bidRanking.indexOf(lastBid)) / (double) bidRanking.size();
                if (lastBidId < 0.1D) {
                    return new Accept(this.getPartyId(), lastBid);
                }
            }
        }

        return new Offer(this.getPartyId(), this.generateRandomBid());
    }

    @Override
    public String getDescription() {
        return "Group 28 Uncertainty Agent";
    }

    public AbstractUtilitySpace estimateUtilitySpace() {
        Domain domain = getDomain();
        CustomUtilitySpaceFactory factory = new CustomUtilitySpaceFactory(domain);
        BidRanking bidRanking = userModel.getBidRanking();
        factory.estimateUsingBidRanks(bidRanking);
        return factory.getUtilitySpace();
    }

    private void log(String var1) {
        System.out.println(var1);
    }
}
