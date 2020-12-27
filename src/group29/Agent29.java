package group29;

import java.util.*;

import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader;
import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.EndNegotiation;
import genius.core.actions.Offer;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;

public class Agent29 extends AbstractNegotiationParty
{
    private static double MINIMUM_TARGET = 0.8;
    private Bid lastOffer;
    private double threshold = 0.1;
    private double targetUtility = 1;
    private AbstractUtilitySpace userUtilitySpace;

    /**
     * Initializes a new instance of the agent.
     */
    @Override
    public void init(NegotiationInfo info)
    {
        super.init(info);

        AgentID agentID = info.getAgentID();
        this.getUserModel().getBidRanking();

        // output
        if (hasPreferenceUncertainty()) {
            System.out.println("Preference uncertainty is enabled  " + agentID);
        }


        //TO DO: userModel
//        GeneticAlgorithm genetic = new GeneticAlgorithm(this.getUserModel());
//        userUtilitySpace = genetic.genUtilitySpace();
    }

    @Override
    public Action chooseAction(List<Class<? extends Action>> possibleActions)
    {
        double time = timeline.getTime();

        if (lastOffer != null) {
            //TO DO:
            //jonny black evaluate
        }

        // 时间小于0.5 只发最高的offer 不接受
        if (time < 0.5) {
            threshold = getThresholdByTime(time);
            return new Offer(getPartyId(), generateRandomBidByRank(threshold));
        } else if (time < 0.9999) {
            if (checkIfBidCanBeAccepted(lastOffer, time)) {
                return new Accept(getPartyId(), lastOffer);
            } else {
                // TO DO: nash
                return new Offer(getPartyId(), generateRandomBidByRank(threshold));
            }
        } else {
            return new EndNegotiation(getPartyId());
        }

    }

    private double getThresholdByTime(double time) {
        if (time < 0.05) {
            return 0.01;
        } else if (time < 0.1) {
            return 0.03;
        } else if (time < 0.15) {
            return 0.05;
        } else if (time < 0.2) {
            return 0.08;
        } else if (time < 0.25) {
            return 0.13;
        } else if (time < 0.3) {
            return 0.18;
        }
        return 0.3;
    }

    private Boolean checkIfBidCanBeAccepted(Bid bid, double time) {

        //TO DO:
        return false;
    }

    private Bid generateRandomBidByRank(double threshold) {
        List<Bid> rankList = getBidOrder();
        int bidOrderSize = rankList.size();
        Random rand = new Random();
        int min = (int) Math.ceil(bidOrderSize * (1 - threshold));
        int randomInt = rand.nextInt((bidOrderSize - min) + 1) + min;

        return rankList.get(randomInt - 1);
    }

    /**
     * Remembers the offers received by the opponent.
     */
    @Override
    public void receiveMessage(AgentID sender, Action action)
    {
        if (action instanceof Offer)
        {
            lastOffer = ((Offer) action).getBid();
        }
    }

    @Override
    public String getDescription()
    {
        return "I am the greatest negotiation agent!";
    }

    @Override
    public AbstractUtilitySpace estimateUtilitySpace()
    {
        return super.estimateUtilitySpace();
    }

    private List<Bid> getBidOrder() {
        return this.getUserModel().getBidRanking().getBidOrder();
    }
}
