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
import genius.core.uncertainty.ExperimentalUserModel;
import genius.core.utility.UncertainAdditiveUtilitySpace;

public class Agent29 extends AbstractNegotiationParty
{
    private AbstractUtilitySpace predictAbstractSpace;
    private AdditiveUtilitySpace predictAddtiveSpace;
//    private ExperimentalUserModel e;
//    private UncertainAdditiveUtilitySpace realUSpace;

    private IaMap iaMap;
    private Random rand = new Random();

    private Bid lastOffer;
    private double threshold = 0.1;
    private int jonnyBlackRound = 10;  //计数 每10轮重新计算

    List<Bid> bidList = new ArrayList<>(); // bid总列表
//    private HashMap<Bid, Double> userUtilities = new HashMap<Bid, Double>(); //user所有bid的utility
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
//        calculateAllUserUtilities();

        // TO DO:
        // jonny black
        iaMap = new IaMap(userModel);

        //test
//        e = (ExperimentalUserModel) userModel;
//        realUSpace = e.getRealUtilitySpace();
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
            if (checkIfBidCanBeAccepted(lastOffer, time)) {
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
                return new Offer(getPartyId(), generateRandomBidByAvailable(time));
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
        return 0.25;
    }

    private Boolean checkIfBidCanBeAccepted(Bid bid, Double time) {
        // TO DO
        // 考虑小的domain
        // 有个concession 需要改成比例
        if (bidList.size() > 100) {
            Point2D.Double one = new Point2D.Double(endPoints[0][0], endPoints[0][1]);
            Point2D.Double two = new Point2D.Double(endPoints[1][0], endPoints[1][1]);

            Point2D.Double target = new Point2D.Double(iaMap.JBpredict(bid), predictAddtiveSpace.getUtility(bid));
            double v = (two.x - one.x) * (target.y - one.y) - (target.x - one.x) * (two.y - one.y);

            double userLastOfferUtility = predictAddtiveSpace.getUtility(bid);
            System.out.println("last offer utility: "+ userLastOfferUtility);

            if (time < 0.95) {
                if (v >= 0 && userLastOfferUtility <= concessionUtility[1]
                        && userLastOfferUtility >= concessionUtility[0]) {
                    return true;
                }
            } else if (time < 0.99){
                return userLastOfferUtility >= concessionUtility[0];
            } else {
                return true;
            }

        } else {
            // TO DO
            // 如果初始给的bids数量太小 直接按排序找
            if (! bidList.contains(bid)) {
                elicitRank(bid);
            }
            int index = bidList.indexOf(bid);
            return ((double) index / bidList.size()) >= concessionUtility[0];

        }


        return false;
    }

    // TO DO: 调整
    private void concessionByTime(double time) {
         if (time >= 0.5 && time <0.7) {
            concessionUtility[0] = 0.9;
            concessionUtility[1] = 1;
        } else if (time >= 0.7 && time <0.9) {
            concessionUtility[0] = 0.85;
            concessionUtility[1] = 0.95;
        } else if (time >= 0.9 && time <0.95) {
            concessionUtility[0] = 0.75;
            concessionUtility[1] = 0.9;
        } else {
            concessionUtility[0] = 0.7;
            concessionUtility[1] = 0.9;
        }
    }

    private Bid generateRandomBidByRank(double threshold) {
        int bidOrderSize = bidList.size();
        int min = (int) Math.floor(bidOrderSize * (1 - threshold));
        int randomInt = rand.nextInt(bidOrderSize - min) + min - 1;

        return bidList.get(randomInt);
    }

    // TO DO: 调试
    private Bid generateRandomBidByAvailable(double time) {
        List<Bid> list = new ArrayList<>();
        for (Bid bid: availableBids) {
            double userUtility = predictAddtiveSpace.getUtility(bid);
            if (userUtility >= concessionUtility[0] && userUtility <= concessionUtility[1]) {
                list.add(bid);
            }
        }
        int boundSize = list.size() - 1;

        if (boundSize > 0) {
            Collections.sort(list, new OpponentBidComparetor());
            // 对手的utility大的里面随机一个
            int minIndex = (int) Math.floor(boundSize * 0.8);
            int index = 0;
            if (boundSize - minIndex > 0) {
                index = rand.nextInt(boundSize - minIndex) + minIndex - 1;
            }
            System.out.println("test index "+ index + " "+ list.size());
            Bid bid = list.get(index);

            System.out.println("my offer:"+ predictAddtiveSpace.getUtility(bid));
            return bid;
        } else {
            //如果不能取到这样的点 则在妥协范围里面找
            for (Bid bid: bidList) {
                double utility = predictAddtiveSpace.getUtility(bid);
                if (utility >= concessionUtility[0] && utility <= concessionUtility[1]) {
                    list.add(bid);
                }
            }
            Collections.sort(list, new OpponentBidComparetor());
            System.out.println("opponent utilities " + opponentUtilities.get(list.get(0)) + " " + opponentUtilities.get(list.get(list.size() - 1)));
            boundSize = list.size() - 1;
            int minIndex = (int) Math.floor(boundSize * 0.8);
            int index = rand.nextInt(boundSize - minIndex) + minIndex - 1;

            return list.get(index);
        }

//        int index = rand.nextInt(availableBids.size() - 1);
//        return availableBids.get(index);

    }

    // elicit rank 会产生额外cost
    private void elicitRank(Bid bid) {
        if (!bidList.contains(bid)) {
            userModel = user.elicitRank(bid, userModel);
            bidList = userModel.getBidRanking().getBidOrder();
        }

    }

    private double disagreeUtility(double disagreePercent) {
        int bidListSize = bidList.size();
        int disagreeIndex = (int)Math.floor(bidListSize * disagreePercent);
        double ret = this.predictAddtiveSpace.getUtility(bidList.get(disagreeIndex));
        return ret;
    }

//    private void calculateAllUserUtilities() {
//        for (Bid bid: bidList) {
//            userUtilities.put(bid, predictAddtiveSpace.getUtility(bid));
//        }
//    }
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
        List<Bid> highestUserBids = bidList.subList((int) Math.floor((bidList.size() - 1) * 0.98), bidList.size() - 1);
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
        endPoints[0][1] = predictAddtiveSpace.getUtility(maxOppBid);

        // 对方最高时 我方最高
        List<Double> userUtility = new ArrayList<>();
        List<Bid> highestOppBids = opponentBidRank.subList((int) Math.floor((bidList.size() - 1) * 0.98), bidList.size() - 1);
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
        endPoints[1][1] = predictAddtiveSpace.getUtility(maxUserBid);

        System.out.println("endPoints" + endPoints);

    }

    private void getAvailableBids() {
        Point2D.Double one = new Point2D.Double(endPoints[0][0], endPoints[0][1]);
        Point2D.Double two = new Point2D.Double(endPoints[1][0], endPoints[1][1]);

        for (Bid bid: bidList) {
            // 注意 bid很少的时候会用elicit
            Point2D.Double target = new Point2D.Double(opponentUtilities.get(bid), predictAddtiveSpace.getUtility(bid));
            double v = (two.x - one.x) * (target.y - one.y) - (target.x - one.x) * (two.y - one.y);

            if (v >= 0) {
                availableBids.add(bid);
            }
        }
    }

}



