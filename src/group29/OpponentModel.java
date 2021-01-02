package group29;

import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader;
import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;

import java.util.*;
import java.util.stream.Collectors;

public class OpponentModel {

    private AdditiveUtilitySpace utilitySpace;
    private BidCounter bidCounter;
    private HashMap<Issue, HashMap<ValueDiscrete, Float>> value_table;
    private HashMap<Issue, Float> issue_weight;

    public OpponentModel(AdditiveUtilitySpace utilitySpace)
    {
        this.utilitySpace = utilitySpace;
        bidCounter = new BidCounter(utilitySpace);
        bidCounter.init();
    }

    public void addBid(Bid bid)
    {
        bidCounter.addBid(bid);
    }

    private HashMap<ValueDiscrete, Float> optionValue(Issue issue)
    {
        HashMap<ValueDiscrete, Integer> option_count = bidCounter.getOptionCount(issue);
        HashMap<ValueDiscrete, Integer> option_rank = (HashMap<ValueDiscrete, Integer>) bidCounter.getOptionCount(issue).clone();
        HashMap<ValueDiscrete, Float> option_value = new HashMap<ValueDiscrete, Float>();

        Iterator it = option_count.entrySet().iterator();
        while(it.hasNext())
        {
            int rank = 1;
            Map.Entry<ValueDiscrete, Integer> option_count_map = (Map.Entry<ValueDiscrete, Integer>) it.next();
            ValueDiscrete option_key = option_count_map.getKey();
            int count1 = option_count_map.getValue();
            option_value.put(option_key, 0.0f);

            for(int count2 : option_count.values())
            {
                if(count2 > count1)
                {
                    rank += 1;
                }
            }
            option_rank.replace(option_key, rank);
        }


        int k = option_value.size();
        Set<ValueDiscrete> keys = option_value.keySet();
        for(ValueDiscrete key : keys)
        {
            int n = option_rank.get(key);
            float rankValue = (float)(k - n + 1)/k;
            option_value.replace(key, rankValue);
        }

        return option_value;
    }

    private HashMap<Issue, HashMap<ValueDiscrete, Float>> updateAllValue()
    {
        HashMap<Issue, HashMap<ValueDiscrete, Float>> newValueTable = new HashMap<Issue, HashMap<ValueDiscrete, Float>>();
        List<Issue> issues = utilitySpace.getDomain().getIssues();
        for(Issue issue: issues)
        {
            HashMap<ValueDiscrete, Float> option_value = optionValue(issue);
            newValueTable.put(issue, option_value);
        }
        this.value_table = newValueTable;
        return newValueTable;
    }

    private HashMap<Issue, Float> updateIssueWeight()
    {
        HashMap<Issue, Float> issue_weight = new HashMap<Issue, Float>();
        List<Issue> issues = utilitySpace.getDomain().getIssues();

        float total_weight = 0;
        for(Issue issue: issues)
        {
            HashMap<ValueDiscrete, Integer> option_count = bidCounter.getOptionCount(issue);

            float total_f = 0;
            ArrayList<Integer> f_list = new ArrayList<>();
            for(ValueDiscrete key: option_count.keySet())
            {
                int f = option_count.get(key);
                total_f += f;
                f_list.add(f);
            }

            float weight = 0;
            for(int f: f_list)
            {
                weight += (f/total_f)*(f/total_f);
            }

            issue_weight.put(issue, weight);
            total_weight += weight;
        }

        HashMap<Issue, Float> normal_issue_weight = new HashMap<Issue, Float>();
        for(Issue key: issue_weight.keySet())
        {
            float weight = issue_weight.get(key);
            normal_issue_weight.put(key, (weight/total_weight));
        }

        return normal_issue_weight;
    }

    public void update()
    {
        this.issue_weight = updateIssueWeight();
        updateAllValue();
    }

    public float getUtility(Bid bid)
    {
        float utility = 0;
        List<Issue> issues = bid.getIssues();
        for (Issue issue : issues) {
            ValueDiscrete value = (ValueDiscrete) bid.getValue(issue);
            float normal_value = value_table.get(issue).get(value);
            float normal_weight = issue_weight.get(issue);
            utility += normal_value*normal_weight;
        }
        return utility;
    }


    public String getIssueWeightStr()
    {
        String s = "************ Updated Issue Weights ************\n";
        Iterator it = issue_weight.entrySet().iterator();
        while(it.hasNext())
        {
            Map.Entry<Issue, Float> map = (Map.Entry<Issue, Float>) it.next();
            String issueName = map.getKey().getName();
            Float weight = map.getValue();
            s += "Issue Name: " + issueName + " Weight: " + weight + "\n";
        }
        s += "\n";
        return s;
    }

    public String getBidCountStr()
    {
        return bidCounter.getBidCountStr();
    }

    public String getOptionValueStr()
    {
        String s = "************ Option Value table ************\n";
        Iterator it = value_table.entrySet().iterator();
        while(it.hasNext())
        {
            Map.Entry<Issue, HashMap<ValueDiscrete, Float>> map = (Map.Entry<Issue, HashMap<ValueDiscrete, Float>>) it.next();
            String issueName = map.getKey().getName();
            s += "Issue Name: " + issueName + "\n";
            HashMap<ValueDiscrete, Float> issue_value_table = map.getValue();
            Iterator it2 = issue_value_table.entrySet().iterator();
            while(it2.hasNext())
            {
                Map.Entry<ValueDiscrete, Float> map2 = (Map.Entry<ValueDiscrete, Float>) it2.next();
                String valueName = map2.getKey().getValue();
                Float value = map2.getValue();
                s += "Option: " + valueName + " value: " + value + "\n";
            }

        }
        s += "\n";
        return s;
    }


}

