package group29;

import java.awt.geom.Point2D;
import java.util.*;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.EndNegotiation;
import genius.core.actions.Offer;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;

public class Agent29 extends AbstractNegotiationParty
{
    private AbstractUtilitySpace predictAbstractSpace;
    private AdditiveUtilitySpace predictAddtiveSpace;

    private IaMap iaMap;

    private Bid lastOffer;
    private double threshold = 0.1;
    private int jonnyBlackRound = 10;  //计数 每10轮重新计算

    List<Bid> bidList = new ArrayList<>(); // bid总列表
    private HashMap<Bid, Double> userUtilities = new HashMap<Bid, Double>(); //user所有bid的utility
    private HashMap<Bid, Double> opponentUtilities = new HashMap<Bid, Double>(); // 对手所有bid的utility
    private List<Bid> opponentBidRank = new ArrayList<>(); //对手根据utility排序后的bid列表
    private double[][] endPoints = new double[2][2]; //筛选bid的直线的两个点
    List<Bid> availableBids = new ArrayList<>();  //可发出的bid
    double[] concessionUtility = {0.8, 0.95};

    public class OpponentBidComparetor implements Comparator<Bid> {
        @Override
        public int compare(Bid o1, Bid o2) {
            if (opponentUtilities.get(o1) > opponentUtilities.get(o2)) {
                return 1;
            } else if (opponentUtilities.get(o1) < opponentUtilities.get(o2)) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    @Override
    public void init(NegotiationInfo info)
    {
        super.init(info);
        bidList = userModel.getBidRanking().getBidOrder();

        //userModel
        UserPrefElicit userPref = new UserPrefElicit(userModel);
        predictAbstractSpace = userPref.geneticAlgorithm();
        predictAddtiveSpace = (AdditiveUtilitySpace) predictAbstractSpace;
        calculateAllUserUtilities();

        // TO DO:
        // jonny black
        iaMap = new IaMap(userModel);
    }

    @Override
    public Action chooseAction(List<Class<? extends Action>> possibleActions)
    {
        double time = timeline.getTime();
        concessionByTime(time);

        // 时间小于0.5 只发最高的offer 不接受
        if (time < 0.5) {
            threshold = getThresholdByTime(time);
            return new Offer(getPartyId(), generateRandomBidByRank(threshold));
        } else if (time < 0.9999) {
            if (checkIfBidCanBeAccepted(lastOffer)) {
                return new Accept(getPartyId(), lastOffer);
            } else {
                // TO DO:
                if (jonnyBlackRound == 10) {
                    jonnyBlackRound = 0;
                    // TO DO: jonny black
                    calculateAllOpponentUtilities();
                    getEndPoints();
                    getAvailableBids();
                }
                jonnyBlackRound += 1;
                return new Offer(getPartyId(), generateRandomBidByAvailable());
            }
        } else {
            return new EndNegotiation(getPartyId());
        }

    }
    @Override
    public void receiveMessage(AgentID sender, Action action)
    {
        if (action instanceof Offer)
        {
            lastOffer = ((Offer) action).getBid();
            if (lastOffer != null) {
                //TO DO:
                //jonny black evaluate
                iaMap.JonnyBlack(lastOffer);
            }
        }
    }

    @Override
    public String getDescription()
    {
        return "I am the greatest negotiation agent!";
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
        return 0.2;
    }

    private Boolean checkIfBidCanBeAccepted(Bid bid) {
        Point2D.Double one = new Point2D.Double(endPoints[0][0], endPoints[0][1]);
        Point2D.Double two = new Point2D.Double(endPoints[1][0], endPoints[1][1]);

        Point2D.Double target = new Point2D.Double(iaMap.JBpredict(bid), predictAddtiveSpace.getUtility(bid));
        double v = (two.x - one.x) * (target.y - one.y) - (target.x - one.x) * (two.y - one.y);

        double userLastOfferUtility = predictAddtiveSpace.getUtility(bid);
        if (v >= 0 && userLastOfferUtility <= concessionUtility[1]
            && userLastOfferUtility >= concessionUtility[0]) {
            return true;
        }
        
        return false;
    }

    // TO DO: 调整
    private void concessionByTime(double time) {
//         if (time >= 0.5 && time <0.7) {
//            concessionUtility[0] = 0.8;
//            concessionUtility[1] = 0.95;
//        } else if (time >= 0.7 && time <0.9) {
//            concessionUtility[0] = 0.8;
//            concessionUtility[1] = 0.9;
//        } else if (time >= 0.9 && time <0.95) {
//            concessionUtility[0] = 0.75;
//            concessionUtility[1] = 0.85;
//        } else {
//            concessionUtility[0] = 0.7;
//            concessionUtility[1] = 0.8;
//        }
    }

    private Bid generateRandomBidByRank(double threshold) {
        int bidOrderSize = bidList.size();
        Random rand = new Random();
        int min = (int) Math.ceil(bidOrderSize * (1 - threshold));
        int randomInt = rand.nextInt(bidOrderSize - min) + min;

        return bidList.get(randomInt - 1);
    }

    // elicit rank 会产生额外cost
    private void elicitRank(Bid bid) {
        if (!bidList.contains(bid)) {
            userModel = user.elicitRank(bid, userModel);
        }

    }

    private double disagreeUtility(double disagreePercent) {
        int bidListSize = bidList.size();
        int disagreeIndex = (int)Math.ceil(bidListSize * disagreePercent);
        double ret = this.predictAddtiveSpace.getUtility(bidList.get(disagreeIndex));
        return ret;
    }

    private void calculateAllUserUtilities() {
        for (Bid bid: bidList) {
            userUtilities.put(bid, predictAddtiveSpace.getUtility(bid));
        }
    }
    private void calculateAllOpponentUtilities() {
        for (Bid bid:bidList) {
            //TO DO:
            //opponent utilities
            opponentUtilities.put(bid, iaMap.JBpredict(bid));
        }
        opponentBidRank.addAll(bidList);

        Collections.sort(opponentBidRank, new OpponentBidComparetor());
    }


    private void getEndPoints() {
        // 我方最高时 对方最高
        List<Bid> highestUserBids = bidList.subList((int) Math.ceil(bidList.size() * 0.9), bidList.size() - 1);
        List<Double> oppUtility = new ArrayList<>();
        for (Bid bid:highestUserBids) {
            //TO DO:
            // jonny black
            double utility = iaMap.JBpredict(bid);
            oppUtility.add(utility);
        }
        double maxOppUtility = Collections.max(oppUtility);
        Bid maxOppBid = highestUserBids.get(oppUtility.indexOf(maxOppUtility));
//        Bid userMax = bidList.get(bidList.size() - 1);
        endPoints[0][0] = opponentUtilities.get(maxOppBid);
        endPoints[0][1] = userUtilities.get(maxOppBid);

        // 对方最高时 我方最高
        List<Double> userUtility = new ArrayList<>();
        List<Bid> highestOppBids = opponentBidRank.subList((int) Math.ceil(bidList.size() * 0.9), bidList.size() - 1);
        for (Bid bid:highestUserBids) {
            //TO DO:
            // jonny black
            double utility = predictAddtiveSpace.getUtility(bid);
            userUtility.add(utility);
        }
        double maxUserUtility = Collections.max(userUtility);
        Bid maxUserBid = highestUserBids.get(userUtility.indexOf(maxUserUtility));
//        Bid opponentMax = opponentBidRank.get(opponentBidRank.size() - 1);
        endPoints[1][0] = opponentUtilities.get(maxUserBid);
        endPoints[1][1] = userUtilities.get(maxUserBid);
    }

    private void getAvailableBids() {
        Point2D.Double one = new Point2D.Double(endPoints[0][0], endPoints[0][1]);
        Point2D.Double two = new Point2D.Double(endPoints[1][0], endPoints[1][1]);

        for (Bid bid: bidList) {
            Point2D.Double target = new Point2D.Double(opponentUtilities.get(bid), userUtilities.get(bid));
            double v = (two.x - one.x) * (target.y - one.y) - (target.x - one.x) * (two.y - one.y);

            if (v >= 0) {
                availableBids.add(bid);
            }
        }
    }

    // TO DO: 调试
    private Bid generateRandomBidByAvailable() {
        List<Bid> list = new ArrayList<>();
        for (Bid bid: availableBids) {
            double userUtility = userUtilities.get(bid);
            if (userUtility >= concessionUtility[0] && userUtility <= concessionUtility[1]) {
                list.add(bid);
            }
        }
        int boundSize = list.size() - 1;

        if (boundSize >= 0) {
            int index = rand.nextInt(boundSize);
            return list.get(index);
        } else {
            //如果不能取到这样的点 则在妥协范围里面找
            for (Bid bid: bidList) {
                double utility = userUtilities.get(bid);
                if (utility >= concessionUtility[0] && utility <= concessionUtility[1]) {
                    list.add(bid);
                }
            }
            Collections.sort(list, new OpponentBidComparetor());
            for (Bid bid:list) {
                System.out.println(opponentUtilities.get(bid));
            }
            return list.get(list.size() - 1);
        }

//        int index = rand.nextInt(availableBids.size() - 1);
//        return availableBids.get(index);

    }

}



