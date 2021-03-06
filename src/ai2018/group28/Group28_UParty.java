package ai2018.group28;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.Domain;
import genius.core.actions.*;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.*;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.persistent.PersistentDataType;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.utility.AbstractUtilitySpace;

import java.util.HashMap;
import java.util.List;

/**
 * The aim of this class is to have agent which manages uncertainty in his own way and consists of components we created.
 * These class was based on class BOAParty code
 */
public class Group28_UParty extends AbstractNegotiationParty {
    /**
     * Our Acceptance strategy
     */
    AcceptanceStrategy group28AS;
    /**
     * Our Bidding Strategy
     */
    OfferingStrategy group28BS;
    /**
     * Our Oponnent Model
     */
    OpponentModel group28_OM;
    /**
     * Our Opponent Model Strategy
     */
    OMStrategy group28_OMS;
    /**
     * negotiation session object used in different methods
     */
    NegotiationSession negotiationSession;
    Bid oppBid;

    public Group28_UParty(){

        group28_OMS = new Group28_OMS();
        group28AS = new Group28_AS();
        group28BS = new Group28_BS();
        group28_OM = new Group28_OM();
    }

    @Override
    public void init(NegotiationInfo info) {
        super.init(info);

        SessionData sessionData = null;
        if (info.getPersistentData()
                .getPersistentDataType() == PersistentDataType.SERIALIZABLE) {
            sessionData = (SessionData) info.getPersistentData().get();
        }
        if (sessionData == null) {
            sessionData = new SessionData();
        }
        negotiationSession = new NegotiationSession(sessionData, utilitySpace,
                timeline, null, info.getUserModel());
        initStrategies();
    }

    /**
     * Method initalizing all our strategies
     */
    private void initStrategies() {
        try {
            group28_OM.init(negotiationSession, new HashMap<String, Double>());
            group28_OMS.init(negotiationSession, group28_OM, new HashMap<String, Double>());
            group28BS.init(negotiationSession, group28_OM, group28_OMS, new HashMap<String, Double>());
            group28AS.init(negotiationSession, group28BS, group28_OM, new HashMap<String, Double>());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is copy-paste of BOAPArty method with slight changes to use our strategies
     * @param sender
     * @param opponentAction
     * @return
     */
    @Override
    public void receiveMessage(AgentID sender, Action opponentAction) {
        if (opponentAction instanceof Offer) {
            oppBid = ((Offer) opponentAction).getBid();
            try {
                BidDetails opponentBid = new BidDetails(oppBid,
                        negotiationSession.getUtilitySpace().getUtility(oppBid),
                        negotiationSession.getTime());
                negotiationSession.getOpponentBidHistory().add(opponentBid);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (group28_OM != null && !(group28_OM instanceof NoModel)) {
                if (group28_OMS.canUpdateOM()) {
                    group28_OM.updateModel(oppBid);
                } else {
                    if (!group28_OM.isCleared()) {
                        group28_OM.cleanUp();
                    }
                }
            }
        }
    }

    /**
     * This method is copy-paste of BOAPArty method with slight changes to use our strategies
     * @param possibleActions
     * @return
     */
    @Override
    public Action chooseAction(List<Class<? extends Action>> possibleActions) {
        BidDetails bid;

        if (negotiationSession.getOwnBidHistory().getHistory().isEmpty()) {
            bid = group28BS.determineOpeningBid();
        } else {
            bid = group28BS.determineNextBid();
            if (group28BS.isEndNegotiation()) {
                return new EndNegotiation(getPartyId());
            }
        }

        if (bid == null) {
            System.out.println("Error in code, null bid was given");
            return new Accept(getPartyId(), oppBid);
        } else {
            group28BS.setNextBid(bid);
        }

        Actions decision = Actions.Reject;
        if (!negotiationSession.getOpponentBidHistory().getHistory()
                .isEmpty()) {
            decision = group28AS.determineAcceptability();
        }

        if (decision.equals(Actions.Break)) {
            System.out.println("send EndNegotiation");
            return new EndNegotiation(getPartyId());
        }

        if (decision.equals(Actions.Reject)) {
            negotiationSession.getOwnBidHistory().add(bid);
            return new Offer(getPartyId(), bid.getBid());
        } else {
            return new Accept(getPartyId(), oppBid);
        }
    }

    @Override
    public String getDescription() {
        return "Group 28 Uncertainty Agent";
    }

    /**
     * Our own estimaton of Utility Space for uncertain environment
     * The algorithm works really similary as in AbstractNegotiationPart but instead of using
     * AdditiveUtilitySpaceFactory it uses our own version of the class which can be found in file
     * CustomUtiiltySpaceFactory. A lot in this class was drawn from AdditiveUtilitySpaceFactory,
     * but some of our own ideas were introduced
     * @return
     */
    public AbstractUtilitySpace estimateUtilitySpace() {
        Domain domain = getDomain();
        AdditiveUtilitySpaceFactory factory = new CustomUtilitySpaceFactory(domain);
        BidRanking bidRanking = userModel.getBidRanking();
        factory.estimateUsingBidRanks(bidRanking);
        return factory.getUtilitySpace();
    }
}
