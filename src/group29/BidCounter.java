package group29;

import genius.core.Bid;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.issue.Issue;

import java.util.*;

public class BidCounter {

    private HashMap<Issue, HashMap<ValueDiscrete, Integer>> issue_table;

    private AdditiveUtilitySpace additiveUtilitySpace;

    public BidCounter(AdditiveUtilitySpace additiveUtilitySpace)
    {
        this.additiveUtilitySpace = additiveUtilitySpace;
    }

    public void init()
    {
        List<Issue> issues = additiveUtilitySpace.getDomain().getIssues();
        HashMap<Issue, HashMap<ValueDiscrete, Integer>> issue_table = new HashMap<Issue, HashMap<ValueDiscrete, Integer>>();
        for (Issue issue : issues) {
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            HashMap<ValueDiscrete, Integer> value_table = new HashMap<ValueDiscrete, Integer>();
            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
                value_table.put(valueDiscrete, 0);
            }
            issue_table.put(issue, value_table);
        }
        this.issue_table = issue_table;
    }

    public void addBid(Bid bid)
    {
        List<Issue> issues = bid.getIssues();
        for (Issue issue : issues) {
            ValueDiscrete value = (ValueDiscrete) bid.getValue(issue);
            HashMap<ValueDiscrete, Integer> value_table = issue_table.get(issue);
            value_table.put(value, value_table.get(value) + 1);
        }
    }

    public HashMap<ValueDiscrete, Integer> getOptionCount(Issue issue)
    {
        return issue_table.get(issue);
    }

    public String getBidCountStr()
    {
        String s = "";
        Set issue_entrySet = issue_table.entrySet();
        Iterator issue_it = issue_entrySet.iterator();
        while(issue_it.hasNext())
        {
            Map.Entry<Issue, HashMap<ValueDiscrete, Integer>> issue_map = (Map.Entry<Issue, HashMap<ValueDiscrete, Integer>>)issue_it.next();
            String issue_name = issue_map.getKey().getName();
            s = s + "issue name: " + issue_name + "\n";
            Set value_entrySet = issue_map.getValue().entrySet();
            Iterator value_it = value_entrySet.iterator();
            while(value_it.hasNext())
            {
                Map.Entry<ValueDiscrete, Integer> value_map = (Map.Entry<ValueDiscrete, Integer>) value_it.next();
                String value = value_map.getKey().getValue();
                int count = value_map.getValue();
                s = s + "option name: " + value + " count: " + count + "\n";
            }
            s = s + "\n";
        }
        return s;
    }

}
