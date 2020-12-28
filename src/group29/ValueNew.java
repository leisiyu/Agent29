package group29;

import java.util.Comparator;

import genius.core.issue.Value;

public class ValueNew implements Comparator<ValueNew> {
    public Value valueName;
    public int count = 0;    // fo: count the frequency of option o
    public int rank = 0;     // no: rank of the option o
    public int totalOfOptions = 0;  // k: the number of possible options for the issue
    public int countBidNumber = 0;  //  t: total number of prior bids
    public double calculatedValue = 0.0f; // Vo: the value predicted for option o
    public double weightUnnormalized = 0.0f; // wiUnnormalized: the unnormalized weight of issue i

    public ValueNew(Value valueName) {
        this.valueName = valueName;
    }

    // compare the count of each option
    @Override
    public int compare(ValueNew o1, ValueNew o2) {
        if(o1.count < o2.count){
            return 1;
        }else if (o1.count == o2.count){
            return 0;
        }else{
            return -1;
        }
    }

    public void compute(){
        // Vo = (k - no + 1) / k
        this.calculatedValue = (((double) this.totalOfOptions - (double) this.rank + 1) / (double) this.totalOfOptions);
        // wiUnnormalized = (fo / t)^2
        double temp = ((double) this.count / (double) this.countBidNumber);
        this.weightUnnormalized = Math.pow(temp,2);
    }
}