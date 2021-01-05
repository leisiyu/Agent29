package group29;

import agents.anac.y2016.caduceus.agents.Caduceus.Opponent;
import genius.core.Bid;
import genius.core.Domain;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;

import java.util.*;
import java.util.stream.Collectors;

public class JonnyBlack{
    private AdditiveUtilitySpace additiveUtilitySpace;
    private OpponentModel opponentModel;
    private double AV;
    private double reluctance;
    private double care;

    private int lastBidIndex;
    private int round;
    private ArrayList<Bid> rank;
    private ArrayList<Bid> allBid;
    private ArrayList<Bid> feasibleBid;
    private ArrayList<Integer> feasibleBidIndex;

    public JonnyBlack(AdditiveUtilitySpace userUtilitySpace){
        this.additiveUtilitySpace = userUtilitySpace;
        opponentModel = new OpponentModel(userUtilitySpace);
        AV = 0.85;
        reluctance = 1;
        care = 0.4;

        lastBidIndex = 0;
        round = 0;


        ArrayList<Bid> allBid = generateAllBid(additiveUtilitySpace);
        this.allBid = allBid;

        ArrayList<Integer> feasibleBidIndex = getFeasibleBidIndex(allBid);
        this.feasibleBidIndex = feasibleBidIndex;

        this.feasibleBid = getFeasibleBid(allBid);

        rank = ranking(feasibleBid);

    }

    public double getOpponentUtility(Bid bid){
        return opponentModel.getUtility(bid);
    }

    /**
     * return all the possible bid in the utility space
     * @param additiveUtilitySpace
     * @return
     */
    private ArrayList<Bid> generateAllBid(AdditiveUtilitySpace additiveUtilitySpace) {
        ArrayList<Bid> all_bids = new ArrayList<Bid>();
        ArrayList<ArrayList<ValueDiscrete>> option_lists = new ArrayList<ArrayList<ValueDiscrete>>();
        List<Issue> issues = additiveUtilitySpace.getDomain().getIssues();
        List<Integer> issue_nums = new ArrayList<Integer>();

        for (Issue issue : issues)
        {
            issue_nums.add(issue.getNumber());
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            ArrayList<ValueDiscrete> value_list = new ArrayList<ValueDiscrete>();
            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues())
            {
                value_list.add(valueDiscrete);
            }
            option_lists.add(value_list);
        }
        List<List<ValueDiscrete>> cartesian_combinations = cartesianCombinations(option_lists);

        Domain domain = additiveUtilitySpace.getDomain();
        for (List<ValueDiscrete> options : cartesian_combinations)
        {
            HashMap<Integer, Value> issue_num_option = new HashMap<Integer, Value>();
            int index = 1;
            for(ValueDiscrete p : options)
            {
                issue_num_option.put(index, p);
                index += 1;
            }
            Bid bid = new Bid(domain, issue_num_option);
            all_bids.add(bid);
        }
        return all_bids;
    }

    /**
     * get bid that not lower than our minimum target
     * @param bids
     * @return
     */
    private ArrayList<Bid> getFeasibleBid(ArrayList<Bid> bids) {

        ArrayList<Bid> feasible_bids = new ArrayList<Bid>();
        for(Bid bid : bids)
        {
            double utility = additiveUtilitySpace.getUtility(bid);
            if(utility >= AV)
            {
                feasible_bids.add(bid);
            }
        }
        return feasible_bids;
    }

    /**
     * Generating feasible bids set that is above the reserve value of my agent
     * @param bids all bids you want to find that is higher than the expectation
     * @return Feasible Bid
     */
    private ArrayList<Integer> getFeasibleBidIndex(ArrayList<Bid> bids)
    {
        ArrayList<Integer> indexes = new ArrayList<Integer>();
        ArrayList<Bid> feasible_bids = new ArrayList<Bid>();
        int index = 0;
        for(Bid bid : bids)
        {
            double utility = additiveUtilitySpace.getUtility(bid);
            if(utility >= AV)
            {
                feasible_bids.add(bid);
                indexes.add(index);
            }
            index += 1;
        }
        return indexes;
    }

    /**
     * select N best element in array according to the opponent model we have, return index of N best
     * @param bids
     * @param n
     */
    private ArrayList<Integer> getOpponentBestNBidIndex(ArrayList<Bid> bids, int n, OpponentModel opponentModel) {
        //create sort able array with index and value pair
        IndexValuePair[] pairs = new IndexValuePair[bids.size()];
        for (int i = 0; i < bids.size(); i++) {
            pairs[i] = new IndexValuePair(i, bids.get(i));
        }

        //sort
        Arrays.sort(pairs, new Comparator<IndexValuePair>() {
            public int compare(IndexValuePair o1, IndexValuePair o2) {
                return Float.compare(opponentModel.getUtility(o1.bid), opponentModel.getUtility(o2.bid));
            }
        });

        //extract the indices
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) {
            result.add(pairs[i].index);
        }
        return result;
    }

    /**
     * rank all the bid with respect to agent utility space
     * @param bids
     * @return
     */
    private ArrayList<Bid> ranking(ArrayList<Bid> bids) {
        //create sort able array with index and value pair
        IndexValuePair[] pairs = new IndexValuePair[bids.size()];
        for (int i = 0; i < bids.size(); i++) {
            pairs[i] = new IndexValuePair(i, bids.get(i));
        }

        //sort
        Arrays.sort(pairs, new Comparator<IndexValuePair>() {
            public int compare(IndexValuePair o1, IndexValuePair o2) {
                return Double.compare(additiveUtilitySpace.getUtility(o1.bid), additiveUtilitySpace.getUtility(o2.bid));
            }
        });

        //extract the indices
        ArrayList<Bid> result = new ArrayList<Bid>();
        for (int i = 0; i < bids.size(); i++) {
            result.add(feasibleBid.get(pairs[i].index));
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * return the bid index in the bid list which has the largest utility
     * @param bidsIndex
     * @return
     */
    private int getBestBidIndex(ArrayList<Integer> bidsIndex) {
        Iterator<Integer> it = bidsIndex.iterator();
        double maxUtility = 0.0;
        int maxBidIndex = bidsIndex.get(0);
        while(it.hasNext())
        {
            int bidIndex = it.next();
            double utility = additiveUtilitySpace.getUtility(allBid.get(bidIndex));
            if(utility > maxUtility)
            {
                maxBidIndex = bidIndex;
                maxUtility = utility;
            }
        }
        return maxBidIndex;
    }

    /**
     * Generate Cartesian product of List, for generating all the possible bids
     * @param <T>
     * @param lists
     * @return
     */
    public static <T> List<List<T>> cartesianCombinations(ArrayList<ArrayList<T>> lists) {
        List<List<T>> currentCombinations = Arrays.asList(Arrays.asList());
        for (ArrayList<T> list : lists) {
            currentCombinations = appendElements(currentCombinations, list);
        }
        return currentCombinations;
    }

    public static <T> List<List<T>> appendElements(List<List<T>> combinations, List<T> extraElements) {
        return combinations.stream().flatMap(oldCombination
                -> extraElements.stream().map(extra -> {
            List<T> combinationWithExtra = new ArrayList<>(oldCombination);
            combinationWithExtra.add(extra);
            return combinationWithExtra;
        }))
                .collect(Collectors.toList());
    }

    public static ArrayList<Integer> intersection(ArrayList<Integer> l1, ArrayList<Integer> l2) {
        HashSet<Integer> set = new HashSet<>();
        set.addAll(l1);
        set.retainAll(l2);

        ArrayList<Integer> result = new ArrayList<>(set);
        return result;
    }

    public class IndexValuePair {
        private int index;
        private Bid bid;

        public IndexValuePair(int index, Bid bid) {
            this.index = index;
            this.bid = bid;
        }
    }

    public Bid makeAnOffer(){
        /***************** update opponent model every 10 round *****************/
        if(round < 10)
        {
            Bid bid = rank.get(lastBidIndex);
            round += 1;
            lastBidIndex += 1;
            return bid;
        }

        //Every 10 round recalculate all properties
        else if(round%10 == 0)
        {
            //Update feasible list and rank(ordered feasible list), because we have calculated a new AV
            feasibleBidIndex = getFeasibleBidIndex(allBid);
            feasibleBid = getFeasibleBid(allBid);
            rank = ranking(feasibleBid);

            //Initialize the lists for store common bids and opponent best bids
            ArrayList<Integer> commonBidsIndex;

            //increase care for opponent and decrease the reluctance parameter for my agent
            care = care * 1.04;
            reluctance = reluctance * 0.985;

            //get opponent N best bids and store at a list
            opponentModel.update();
            ArrayList<Integer> OppoBestNBidsIndex = getOpponentBestNBidIndex(allBid, 200, opponentModel);
            System.out.println("opponent properties :" + "\n" + opponentModel.getIssueWeightStr());


            //intersection my feasible bids with all bid lists from opponents, get the common bid list
            commonBidsIndex = intersection(feasibleBidIndex , OppoBestNBidsIndex);

            System.out.println("round " + round + " common bids amount: " + commonBidsIndex.size());


            //Find the best bid in common bid list and re-calculate the AV
            int bestCommonBidIndex = getBestBidIndex(feasibleBidIndex); //best bid will be the overall best bid in all possible bid in default
            //if there is common bid, find best bid in common bid
            System.out.println("Common Bid Amount: " + commonBidsIndex.size());
            if(commonBidsIndex.size() != 0)
            {
                bestCommonBidIndex = getBestBidIndex(commonBidsIndex);
            }

            System.out.println("round " + round + " AV: " + AV);
            System.out.println("round " + round + " Care: " + care);

            AV = additiveUtilitySpace.getUtility(allBid.get(bestCommonBidIndex)) * reluctance;

        }
        round += 1;


        /***************** Making an offer to opponent *****************/

        // From high to low in rank, find the bid iterative that satisfy AV and Care
        // if the bid is not accepted, move to the next bid in the rank
        for(int i = lastBidIndex; i < rank.size()-1; i++)
        {
            if((additiveUtilitySpace.getUtility(rank.get(i)) >= AV) && (opponentModel.getUtility(rank.get(i)) >= care))
            {
//				System.out.println(opponentModelMap.get(agentToFavour).getUtility(rank.get(i)));
//				System.out.println(opponentModelMap.get(agentToFavour).getOptionValueStr());
                lastBidIndex = i+1;

                return rank.get(i);
            }
        }

        lastBidIndex = 0;

        return rank.get(0);
    }


    public void updateLastOffer(Bid lastOffer) {
        opponentModel.addBid(lastOffer);
    }

}
