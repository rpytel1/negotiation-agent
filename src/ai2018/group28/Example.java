//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ai2018.group28;

import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.ExperimentalUserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.UncertainAdditiveUtilitySpace;
import java.util.List;

public class Example extends AbstractNegotiationParty {
    public Example() {
    }

    public Action chooseAction(List<Class<? extends Action>> var1) {
        this.log("This is the UncertaintyAgentExample.");
        this.log("The user model is: " + this.userModel);
        this.log("The default estimated utility space is: " + this.getUtilitySpace());
        Bid var2 = this.getUtilitySpace().getDomain().getRandomBid(this.rand);
        this.log("The default estimate of the utility of a random bid + " + var2 + " is: " + this.getUtility(var2));
        if (this.userModel instanceof ExperimentalUserModel) {
            this.log("You have given the agent access to the real utility space for debugging purposes.");
            ExperimentalUserModel var3 = (ExperimentalUserModel)this.userModel;
            UncertainAdditiveUtilitySpace var4 = var3.getRealUtilitySpace();
            this.log("The real utility space is: " + var4);
            this.log("The real utility of the random bid is: " + var4.getUtility(var2));
        }

        if (this.getLastReceivedAction() instanceof Offer) {
            Bid var7 = ((Offer)this.getLastReceivedAction()).getBid();
            List var8 = this.userModel.getBidRanking().getBidOrder();
            if (var8.contains(var7)) {
                double var5 = (double)(var8.size() - var8.indexOf(var7)) / (double)var8.size();
                if (var5 < 0.1D) {
                    return new Accept(this.getPartyId(), var7);
                }
            }
        }

        return new Offer(this.getPartyId(), this.generateRandomBid());
    }

    private void log(String var1) {
        System.out.println(var1);
    }

    public AbstractUtilitySpace estimateUtilitySpace() {
        return (new AdditiveUtilitySpaceFactory(this.getDomain())).getUtilitySpace();
    }

    public String getDescription() {
        return "Example agent that can deal with uncertain preferences";
    }
}
