package group29;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

import java.util.*;

public class UserPrefElicit {

    private UserModel userModel;  // User Uncertainty
    private Random randomSeed = new Random(); // generate random seed
    private List<AbstractUtilitySpace> population = new ArrayList<>();  // initial population in GA
    private int initPopSize = 10000;          // size of population
    private int maxIterNum = 50;       // iteration number of GA
    private double crossoverRate = 0.1; // crossover rate
    private double mutationRate = 0.04; // mutation rate
    private double M = 50.0f;
    private double r = 0.9;

    public UserPrefElicit(UserModel userModel) {
        this.userModel = userModel;
    }

    public AbstractUtilitySpace geneticAlgorithm() {

        // 1. initial population
        for (int i = 0; i < initPopSize; i ++) {
            population.add(getRandomChromosome());
        }
        double worstFitness = 0.0f;

        for (int i = 0; i < maxIterNum; i ++) {

            // 2. get fitness of each chromosome
            List <Double> adaptiveValueList = new ArrayList<>();

            for (int j = 0; j < population.size(); j ++) {
                adaptiveValueList.add(getAdaptiveValue(population.get(j), i));
            }

            // System.out.println("Average fitness: " + getAverage(adaptiveValueList));

            // 3. selection
            population = selection(population, adaptiveValueList);
            int popSize = population.size();

            // 4. crossover and mutation
            for (int j = 0; j < popSize*crossoverRate; j ++) {
                AdditiveUtilitySpace father = (AdditiveUtilitySpace) population.get(randomSeed.nextInt(popSize));
                AdditiveUtilitySpace mother = (AdditiveUtilitySpace) population.get(randomSeed.nextInt(popSize));
                AbstractUtilitySpace child = crossover(father, mother);
                population.add(child);
            }
        }

        // select the best one in the final population to be our userPref
        List<Double> adaptiveValueList = new ArrayList<> ();
        for (AbstractUtilitySpace pop: population) {
            adaptiveValueList.add(getAdaptiveValue(pop, maxIterNum));
        }
        double bestFitness = Collections.max(adaptiveValueList);
        int index = adaptiveValueList.indexOf(bestFitness);
        return population.get(index);
    }

    // generate random AbstractUtilitySpace
    // this part is the same from
    private AbstractUtilitySpace getRandomChromosome() {
        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory =
                new AdditiveUtilitySpaceFactory(userModel.getDomain());
        List<Issue> issues = additiveUtilitySpaceFactory.getDomain().getIssues();
        for (Issue issue: issues) {
            additiveUtilitySpaceFactory.setWeight(issue, randomSeed.nextDouble());
            IssueDiscrete values = (IssueDiscrete) issue;
            for (Value value:values.getValues()) {
                additiveUtilitySpaceFactory.setUtility(issue, (ValueDiscrete) value, randomSeed.nextDouble());
            }
        }
        additiveUtilitySpaceFactory.normalizeWeights();
        return additiveUtilitySpaceFactory.getUtilitySpace();
    }

    private double getAdaptiveValue(AbstractUtilitySpace abstractUtilitySpace, int iterNum) {

        // 1. generate validationBidList
        int validationBidListSize = 1000; // size of validationBidList
        double M = 3;
        double r = 0.9;

        List<Bid> validationBidListAll = userModel.getBidRanking().getBidOrder();
        List<Bid> validationBidList = new ArrayList<>();

        int validationBidListAllSize = validationBidListAll.size();

        // we need to sample the list to make the size smaller to calculate fast
        int sampleGap = 1 + validationBidListAllSize / validationBidListSize;
        for (int i = 0; i < validationBidListAllSize; i += sampleGap) {
            validationBidList.add(validationBidListAll.get(i));
        }

        validationBidListSize = validationBidList.size();

        // 2. get the training utility of all the bids in validationBidList (this is now in the order from low to high)
        // and save the value to trainUtilityOrder(TreeMap) to order the training utility
        TreeMap<Integer, Double> trainUtilityMap = new TreeMap(); // Key: original index, Value: utility value

        for (int i = 0; i < validationBidListSize; i ++) {
            trainUtilityMap.put(i, abstractUtilitySpace.getUtility(validationBidList.get(i)));
        }

        // 3. use TreeMap to order our trainUtilityList
        // order by value and get new indexes
        List<Map.Entry<Integer, Double>> compareRankList = new ArrayList<>(trainUtilityMap.entrySet());
        compareRankList.sort(new TreeMapComp());

        // 4. calculate the objective value
        // obj = sum(abs(new index - original index))
        double obj = 0;
        for (int i = 0; i < validationBidListSize; i ++) {
            int gap = Math.abs(compareRankList.get(i).getKey()-i);
            obj += gap * gap;
        }
        obj = obj / validationBidListSize;

        // 5. get the fitness of the input abstractUtilitySpace
        double fitness = -20 * Math.log(obj) + 300;
        fitness = fitness + M * Math.pow(r, iterNum);
        return fitness;
    }

    // selection
    private List<AbstractUtilitySpace> selection(List<AbstractUtilitySpace> population, List<Double> fitnessList) {
        int eliteSize = 2; // size of selected elite
        int eliteGroupSize = 10; // size of selected elite group
        int[] eliteGroupIndexList = new int[eliteGroupSize];
        int populationSize = fitnessList.size();
        List<AbstractUtilitySpace> nextPopulation = new ArrayList<>();

        // Deep copy to find top eliteGroupSize chromosome
        List<Double> copyFitnessList = new ArrayList<>();
        for (Double aDouble : fitnessList) {
            copyFitnessList.add(aDouble);
        }

        for (int i = 0; i < eliteGroupSize; i ++) {
            double maxFitness = Collections.max(copyFitnessList);
            double minFitness = Collections.min(copyFitnessList);
            int index = copyFitnessList.indexOf(maxFitness);
            eliteGroupIndexList[i] = index;
            // set the current max fitness to the min one
            copyFitnessList.set(index, minFitness);
        }

        int num = 0;
        while (num < eliteSize) {
            int tmp = randomSeed.nextInt(10);
            if (eliteGroupIndexList[tmp] != -1) {
                nextPopulation.add(population.get(eliteGroupIndexList[tmp]));
                eliteGroupIndexList[tmp] = -1;
                num ++;
            }
        }

        // Linear Ranking Selection
        // 1. Sort the fitness
        TreeMap<Integer, Double> fitnessMap = new TreeMap(); // Key: original index, Value: utility value

        for (int i = 0; i < populationSize; i ++) {
            fitnessMap.put(i, fitnessList.get(i));
        }

        List<Map.Entry<Integer, Double>> compareRankList = new ArrayList<>(fitnessMap.entrySet());
        compareRankList.sort(new TreeMapComp());

        // 2. Based on Pmin+(Pmax-Pmin)*(i-1)/(N-1)
        double Pmax = 0.95;
        double Pmin = 0.05;
        for (int i = 0; i < populationSize; i ++) {
            double Pi = Pmin + (Pmax - Pmin) * (i-1) / (populationSize-1);
            double selectP = randomSeed.nextDouble();
            if (selectP <= Pi) {
                nextPopulation.add(population.get(compareRankList.get(i).getKey()));
            }
        }
        return nextPopulation;
    }

    private AbstractUtilitySpace crossover(AdditiveUtilitySpace father, AdditiveUtilitySpace mother) {
        double fatherGene;
        double motherGene;
        double childGene;
        double shiftStep = 0.35; // shift step to mom or dad

        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory = new AdditiveUtilitySpaceFactory(userModel.getDomain());
        List<IssueDiscrete> issueList = additiveUtilitySpaceFactory.getIssues();
        for (IssueDiscrete issue : issueList) {
            fatherGene = father.getWeight(issue);
            motherGene = mother.getWeight(issue);
            childGene = getCrossoverGene(shiftStep, fatherGene, motherGene);
            additiveUtilitySpaceFactory.setWeight(issue, childGene);

            for (ValueDiscrete value : issue.getValues()) {
                fatherGene = ((EvaluatorDiscrete) father.getEvaluator(issue)).getDoubleValue(value);
                motherGene = ((EvaluatorDiscrete) mother.getEvaluator(issue)).getDoubleValue(value);
                childGene = getCrossoverGene(shiftStep, fatherGene, motherGene);
                additiveUtilitySpaceFactory.setUtility(issue, value, childGene);
            }
        }
        additiveUtilitySpaceFactory.normalizeWeights();
        return additiveUtilitySpaceFactory.getUtilitySpace();
    }

    private double getAverage(List<Double> list) {
        double sum = 0;
        for (Double aDouble : list) {
            sum += aDouble;
        }
        return sum / list.size();
    }

    private double getCrossoverGene(double shiftStep, double fatherGene, double motherGene) {
        double unionGene = fatherGene + motherGene / 2;
        double childGene;
        double ShiftGene = shiftStep * Math.abs(fatherGene-motherGene);
        
        if (randomSeed.nextDouble() > 0.5) {
            childGene = unionGene + ShiftGene;
        } else {
            childGene = unionGene - ShiftGene;
            if (childGene < 0.01) childGene = 0.01;
        }

        // mutation
        if (randomSeed.nextDouble() < mutationRate) {
            childGene = randomSeed.nextDouble();
        }

        return childGene;
    }
}

class TreeMapComp implements Comparator<Map.Entry<Integer, Double>>
{
    @Override
    public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
        int flag = o1.getValue().compareTo(o2.getValue());
        if (flag == 0)
            return o1.getKey().compareTo(o2.getKey());
        return flag;
    }
}