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
import genius.core.uncertainty.BidRanking;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;

public class Agent29 extends AbstractNegotiationParty
{
    private AbstractUtilitySpace predictAbstractSpace;
    private AdditiveUtilitySpace predictAddtiveSpace;

    private JonnyBlack jonnyBlack;
    private Random rand = new Random();

    private Bid lastOffer;
    private double threshold = 0.1;
    private int jonnyBlackRound = 10;  //计数 每10轮重新计算
    private double userResValue;
    private double opponentResValue;
    private Bid maxBidForMe;
    private double acceptNashDis = 0;
    private Bid myNashBid;

    List<Bid> bidList = new ArrayList<>(); // bid总列表
    private HashMap<Bid, Double> opponentUtilities = new HashMap<Bid, Double>(); // 对手所有bid的utility
    private List<Bid> opponentBidRank = new ArrayList<>(); //对手根据utility排序后的bid列表
    private double[][] endPoints = new double[2][2]; //筛选bid的直线的两个点
    List<Bid> availableBids = new ArrayList<>();  //可发出的bid
    double[] concessionUtility = {0.9, 1};

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

        // jonny black
        jonnyBlack = new JonnyBlack(predictAddtiveSpace);
        this.maxBidForMe = userModel.getBidRanking().getMaximalBid();
        this.myNashBid = maxBidForMe;

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
                //jonny black evaluate
                jonnyBlack.updateLastOffer(lastOffer);
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

    // TO DO
    private Boolean checkIfBidCanBeAccepted(Bid bid, Double time) {
        // TO DO
        // 考虑小的domain
        // 有个concession 需要改成比例
        if (bidList.size() > 100) {
            Point2D.Double one = new Point2D.Double(endPoints[0][0], endPoints[0][1]);
            Point2D.Double two = new Point2D.Double(endPoints[1][0], endPoints[1][1]);

            Point2D.Double target = new Point2D.Double(jonnyBlack.getOpponentUtility(bid), predictAddtiveSpace.getUtility(bid));
            double v = (two.x - one.x) * (target.y - one.y) - (target.x - one.x) * (two.y - one.y);

            double userLastOfferUtility = predictAddtiveSpace.getUtility(bid);
            System.out.println("last offer utility: "+ userLastOfferUtility);

            if (time < 0.65) {
                return checkIfNearNashPoint(bid);
            } else {
//            if (time < 0.95) {
                if (v >= 0 && userLastOfferUtility <= concessionUtility[1]
                        && userLastOfferUtility >= concessionUtility[0]) {
                    return true;
                }
            }
//            } else {
//                return userLastOfferUtility >= concessionUtility[0];
//            }
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

    private Boolean checkIfNearNashPoint(Bid bid) {
        double a = predictAddtiveSpace.getUtility(bid) - predictAddtiveSpace.getUtility(myNashBid);
        if (!opponentUtilities.containsKey(bid)) {
            opponentUtilities.put(bid, jonnyBlack.getOpponentUtility(bid));
        }
        if (!opponentUtilities.containsKey(myNashBid)) {
            opponentUtilities.put(myNashBid, jonnyBlack.getOpponentUtility(myNashBid));
        }
        double b = opponentUtilities.get(bid) - opponentUtilities.get(myNashBid);
        double dis = Math.sqrt(Math.pow(a, a) + Math.pow(b, b));
        return dis <= acceptNashDis;
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
        int min = (int) Math.floor((bidOrderSize - 1) * (1 - threshold));
        for (int i = min; i < bidOrderSize; i ++) {
            double Pi = (i-min+1) / (bidOrderSize-min-1);
            if (rand.nextDouble() < Pi) {
                return bidList.get(i);
            }
        }
        return bidList.get(bidOrderSize-1);
    }

    private Bid generateRandomBidByAvailable(double time) {
        List<Bid> list = new ArrayList<>();
        // 1. try to offer the Nash Point
        for (Bid bid: availableBids) {
            double userUtility = predictAddtiveSpace.getUtility(bid);
            if (userUtility >= concessionUtility[0] && userUtility <= concessionUtility[1]) {
                list.add(bid);
            }
        }
        if (list.size() > 0) {
            List<Double> nashValueList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                double nashValue = predictAddtiveSpace.getUtility(list.get(i)) * opponentUtilities.get(list.get(i));
                nashValueList.add(nashValue);
            }
            double bestNashValue = Collections.max(nashValueList);
            int index = nashValueList.indexOf(bestNashValue);
            int nashValueListSize = nashValueList.size();
            int nashNeighbourSize = nashValueListSize;
            if (nashValueListSize >= 4) {
                nashNeighbourSize = 3;
            }
            myNashBid = list.get(index);
            List<Bid> nashNeighbourList = new ArrayList<>();
            List<Double> sortNashValueList = new ArrayList<>();
            for (int i = 0; i < nashValueListSize; i ++) {
                sortNashValueList.add(nashValueList.get(i));
            }
            Collections.sort(sortNashValueList);
            double disTmp = 0;
            for (int i = 0; i < nashNeighbourSize; i ++) {
                int tmpIndex = 0;
                if (nashNeighbourSize > 3) {
                    tmpIndex = (sortNashValueList.size()-i-2);
                } else {
                    tmpIndex = i;
                    if (tmpIndex == sortNashValueList.size()) {
                        break;
                    }
                }
                double tmpValue = sortNashValueList.get(tmpIndex);
                tmpIndex = nashValueList.indexOf(tmpValue);
                Bid tmpBid = list.get(tmpIndex);
                double a = predictAddtiveSpace.getUtility(tmpBid) - predictAddtiveSpace.getUtility(myNashBid);
                double b = opponentUtilities.get(tmpBid) - opponentUtilities.get(myNashBid);
                disTmp += Math.sqrt(Math.pow(a, a) + Math.pow(b, b));
            }
            this.acceptNashDis = disTmp / (double) nashNeighbourSize;
            return myNashBid;
        } else {
            // 2. if no possible Nash Points, offer some bids that
            for (Bid bid: bidList) {
                double utility = predictAddtiveSpace.getUtility(bid);
                double oppUtility = opponentUtilities.get(bid);
                if (utility >= concessionUtility[0] && utility <= concessionUtility[1] && utility > oppUtility) {
                    list.add(bid);
                }
            }
            if (list.size() == 0) {
                return this.maxBidForMe;
            }
            Collections.sort(list, new OpponentBidComparetor());
            return list.get(list.size()-1);
        }

//        if (boundSize > 0) {
//            Collections.sort(list, new OpponentBidComparetor());
//            // 对手的utility大的里面随机一个
//            int minIndex = (int) Math.ceil(boundSize * 0.8);
//            int index = 0;
//            if (boundSize - minIndex > 0) {
//                index = rand.nextInt(boundSize - minIndex + 1) + minIndex;
//            }
//            System.out.println("test index "+ index + " "+ list.size());
//            Bid bid = list.get(index);
//
//            System.out.println("my offer:"+ predictAddtiveSpace.getUtility(bid));
//            return bid;
//        } else {
//            //如果不能取到这样的点 则在妥协范围里面找
//            for (Bid bid: bidList) {
//                double utility = predictAddtiveSpace.getUtility(bid);
//                if (utility >= concessionUtility[0] && utility <= concessionUtility[1]) {
//                    list.add(bid);
//                }
//            }
//            Collections.sort(list, new OpponentBidComparetor());
//            System.out.println("opponent utilities " + opponentUtilities.get(list.get(0)) + " " + opponentUtilities.get(list.get(list.size() - 1)));
//            boundSize = list.size() - 1;
//            int minIndex = (int) Math.ceil(boundSize * 0.8);
//            int index = rand.nextInt(boundSize - minIndex + 1) + minIndex;
//
//            return list.get(index);
//        }

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

    private void calculateAllOpponentUtilities() {
        for (Bid bid:bidList) {
            //opponent utilities
            opponentUtilities.put(bid, jonnyBlack.getOpponentUtility(bid));
        }
        opponentBidRank.addAll(bidList);

        Collections.sort(opponentBidRank, new OpponentBidComparetor());
    }


    private void getEndPoints() {
        // 我方最高时 对方最高
        List<Bid> highestUserBids = bidList.subList((int) Math.floor((bidList.size() - 1) * 0.98), bidList.size() - 1);
        List<Double> oppUtility = new ArrayList<>();
        for (Bid bid:highestUserBids) {
            // jonny black
            double utility = jonnyBlack.getOpponentUtility(bid);
            oppUtility.add(utility);
        }
        double maxOppUtility = Collections.max(oppUtility);
        // Bid maxOppBid = highestUserBids.get(oppUtility.indexOf(maxOppUtility));
        Bid userMax = bidList.get(bidList.size() - 1);
        endPoints[0][0] = opponentUtilities.get(userMax);
        endPoints[0][1] = predictAddtiveSpace.getUtility(userMax);

        // 对方最高时 我方最高
        List<Double> userUtility = new ArrayList<>();
        List<Bid> highestOppBids = opponentBidRank.subList((int) Math.floor((bidList.size() - 1) * 0.98), bidList.size() - 1);
        for (Bid bid:highestUserBids) {
            // user
            double utility = predictAddtiveSpace.getUtility(bid);
            userUtility.add(utility);
        }
        double maxUserUtility = Collections.max(userUtility);
        // Bid maxUserBid = highestUserBids.get(userUtility.indexOf(maxUserUtility));
        Bid opponentMax = opponentBidRank.get(opponentBidRank.size() - 1);
        endPoints[1][0] = opponentUtilities.get(opponentMax);
        endPoints[1][1] = predictAddtiveSpace.getUtility(opponentMax);

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



