package group29;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.uncertainty.UserModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


public class IaMap extends HashMap<Issue, List<ValueNew>> {

    public int countBidNumber = 0;    //  t: total number of prior bids
    HashMap<Issue, Double> weightList = new HashMap<>();   // store the weight of each issue

    public IaMap(UserModel userModel){
        super();   // inherit HashMap
        for(Issue issue:userModel.getDomain().getIssues()){
            IssueDiscrete values = (IssueDiscrete) issue; // turn issue to IssueDiscrete type
            List<ValueNew> list = new ArrayList<>();
            for(int i = 0; i < values.getNumberOfValues(); i ++){
                ValueNew temp = new ValueNew(values.getValue(i));
                list.add(temp);
            }
            this.put(issue, list); // put <issue, list>
        }
    }

    public void JonnyBlack(Bid lastOffer){
        this.countBidNumber += 1;  //  t: total number of prior bids

        for(Issue issue : lastOffer.getIssues()){
            int num = issue.getNumber();  // This is issue num

            for(ValueNew valueNew : this.get(issue)){
                if(valueNew.valueName.toString().equals(lastOffer.getValue(num).toString())){
                    // add up to the option for the lastOffer
                    valueNew.count += 1;
                }

                IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
                // 其实这个不需要每一次都赋值，第一次赋值完就不会改变了
                valueNew.totalOfOptions = issueDiscrete.getNumberOfValues();
                // renew countBidNumber in each valueNew
                valueNew.countBidNumber = this.countBidNumber;
            }

            // sort in a descending order
            Collections.sort(this.get(issue), this.get(issue).get(0));

            // renew rank in each valueNew
            for(ValueNew valueNew : this.get(issue)){
                // rank = index + 1
                valueNew.rank = this.get(issue).indexOf(valueNew) + 1;
            }
        }

        // calculate the weights for every issue below
        for(Issue issue : lastOffer.getIssues()){
            for(ValueNew valueNew : this.get(issue)){
                // calculate the unnormalized weights
                valueNew.compute();
            }
        }

        for(Issue issue : lastOffer.getIssues()){
            double totalWeight = 0.0f; // store the total unnormalized weight
            double maxWeight = 0.0f; // store the max weight in the issue
            for(ValueNew valueNew : this.get(issue)){
                totalWeight += valueNew.weightUnnormalized;
                if (valueNew.rank == 1) {
                    maxWeight = valueNew.weightUnnormalized;
                }
            }
            double issueWeight = maxWeight / totalWeight;
            this.weightList.put(issue, issueWeight);
        }

        // calculate the utility
        double temp = JBpredict(lastOffer);
    }

    public double JBpredict(Bid lastOffer){

        double utility = 0.0f;   //先进行初始化

        for(Issue issue : lastOffer.getIssues()){
            int num = issue.getNumber();
            for(ValueNew valueNew : this.get(issue)){
                if(valueNew.valueName.toString().equals(lastOffer.getValue(num).toString())) {
                    utility += weightList.get(issue) * valueNew.calculatedValue;
                    break;
                }
            }
        }

        // System.out.println(countBidNumber + "-> Group29: The utility of opponent is " + utility);

        return utility;
    }
}
