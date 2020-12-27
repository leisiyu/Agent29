package group29;

import java.util.*;

import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.Bid;
import genius.core.utility.EvaluatorDiscrete;

import java.util.ArrayList;
import java.util.Random;

public class GeneticAlgorithm {

    private UserModel userModel;
    private Random rand = new Random();

    private List<AbstractUtilitySpace> population = new ArrayList<AbstractUtilitySpace>();
    private int populationSize = 500;
    private int maxIterNum = 200;
    private double virationRate = 0.05;

    public GeneticAlgorithm(UserModel userModel) {
        this.userModel = userModel;
    }

    public AbstractUtilitySpace genUtilitySpace() {
        //initiate
        for (int i = 0; i < populationSize; i++) {
            population.add(getRandomUnit());
        }

        for (int i = 0; i < maxIterNum; i++) {

            List<Double> fitnessList = new ArrayList<>();
            for (int j = 0; j < population.size(); j++) {
                fitnessList.add(getFitness(population.get(j)));
            }

            population = select(population, fitnessList, populationSize);

            for (int j = 0; j < populationSize * 0.1; j++) {
                AdditiveUtilitySpace father = (AdditiveUtilitySpace) population.get(rand.nextInt(populationSize));
                AdditiveUtilitySpace mother = (AdditiveUtilitySpace) population.get(rand.nextInt(populationSize));
                AbstractUtilitySpace child = mate(father, mother);
                population.add(child);
            }
        }

        int bestIdx = 0;
        double bestFitness = 0;
        for (int i = 0; i < population.size(); i++) {
            double fitness = getFitness(population.get(i));
            if (bestFitness < fitness) {
                bestFitness = fitness;
                bestIdx = i;
            }
        }
        System.out.println("resultsssss" + bestIdx + bestFitness);
        System.out.println("222222" + population.get(bestIdx));
        return population.get(bestIdx);
    }

    private AbstractUtilitySpace getRandomUnit() {
        AdditiveUtilitySpaceFactory factory = new AdditiveUtilitySpaceFactory(this.userModel.getDomain());

        List<IssueDiscrete> issues = factory.getIssues();
        for (IssueDiscrete issue: issues) {
            factory.setWeight(issue, rand.nextDouble());
            IssueDiscrete values=(IssueDiscrete) issue;
            for (Value value:values.getValues()){
                factory.setUtility(issue, (ValueDiscrete)value, rand.nextDouble());   //因为现在是累加效用空间，随便设置一个权重之后，可以对当前这个value设置一个效用，效用随机。
            }
        }
        factory.normalizeWeights();
        return factory.getUtilitySpace();
    }

    private AbstractUtilitySpace mate(AdditiveUtilitySpace father, AdditiveUtilitySpace mother) {
        double fatherWeight;
        double motherWeight;
        double uniteWeight;

        AdditiveUtilitySpaceFactory factory = new AdditiveUtilitySpaceFactory(this.userModel.getDomain());
        List<IssueDiscrete> issues = factory.getIssues();
        for (IssueDiscrete issue: issues) {
            fatherWeight = father.getWeight(issue);
            motherWeight = mother.getWeight(issue);

            uniteWeight = (fatherWeight + motherWeight) / 2;
            double childWeight = 0;
            double sign = Math.random() > 0.5 ? 1: -1;
            childWeight = uniteWeight + sign * virationRate * Math.abs(fatherWeight - motherWeight);

            if (childWeight < 0.01) childWeight = 0.01;
            factory.setWeight(issue , childWeight);

            if (rand.nextDouble() < virationRate) {
                factory.setWeight(issue, rand.nextDouble());
            }

            for (ValueDiscrete value: issue.getValues()) {
                fatherWeight = ((EvaluatorDiscrete)father.getEvaluator(issue)).getDoubleValue(value);
                motherWeight = ((EvaluatorDiscrete)mother.getEvaluator(issue)).getDoubleValue(value);

                uniteWeight = (fatherWeight + motherWeight) / 2;

                double sign1 = Math.random() > 0.5 ? 1: -1;
                childWeight = uniteWeight + sign1 * virationRate * Math.abs(fatherWeight - motherWeight);
                if (childWeight < 0.01) childWeight = 0.01;
                factory.setUtility(issue, value, childWeight);
                if (rand.nextDouble() < virationRate) {
                    factory.setUtility(issue, value, rand.nextDouble());
                }
            }
        }
        factory.normalizeWeights();
        return factory.getUtilitySpace();
    }

    private double getFitness(AbstractUtilitySpace unit) {
        BidRanking bidRanking = userModel.getBidRanking();

        List<Bid> bidOrder = bidRanking.getBidOrder();

        List<Bid> selectBidOrder = new ArrayList<>();
        int interval = bidOrder.size() % 400 + 1;
        for (int i = 0; i < bidOrder.size(); i = i + interval) {
            selectBidOrder.add(bidOrder.get(i));
        }

        TreeMap<Integer, Double> utilityRank = new TreeMap<>();
        for (int i = 0; i < selectBidOrder.size(); i++) {
            utilityRank.put(i, unit.getUtility(selectBidOrder.get(i)));
        }

        Comparator<Map.Entry<Integer, Double>> comparator = Comparator.comparingDouble(Map.Entry::getValue);
        List<Map.Entry<Integer, Double>> rank = new ArrayList<>(utilityRank.entrySet());
        Collections.sort(rank, comparator);

        int totalError = 0;
        for (int i = 0; i < rank.size(); i++) {
            int error = rank.get(i).getKey() - i;
            totalError = totalError + (error * error);
        }


        double x = totalError / Math.pow(rank.size(), 3);
        double score = -15 * Math.log(x + 0.00001f);

        return score;
    }

    private List<AbstractUtilitySpace> select(List<AbstractUtilitySpace> populationList, List<Double> fitnessList, int popSize) {
        List<AbstractUtilitySpace> nextPopulation = new ArrayList<>();

        List<Double> copyFitness = new ArrayList<>();
        copyFitness.addAll(fitnessList);


        int pickNumber = 2;
        for (int i = 0; i < pickNumber; i++) {
            double max = Collections.max(copyFitness);
            int idx = copyFitness.indexOf(max);
            nextPopulation.add(populationList.get(idx));

            copyFitness.set(idx, (double) -10000);
        }

        double sumFitness = fitnessList.stream().reduce(Double::sum).orElse((double) 0);

        for (int i = 0; i < popSize - pickNumber; i++) {
            double randomNum = rand.nextDouble() * sumFitness;
            double sum = 0;
            for (int j = 0; j < populationList.size(); j++) {
                sum = sum + fitnessList.get(i);
                if (sum > randomNum) {
                    nextPopulation.add(populationList.get(j));
                    break;
                }
            }
        }

        return nextPopulation;
    }
}
