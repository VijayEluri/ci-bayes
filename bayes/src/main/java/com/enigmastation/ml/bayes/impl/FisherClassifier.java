package com.enigmastation.ml.bayes.impl;

import com.enigmastation.ml.bayes.ClassifierDataFactory;
import com.enigmastation.ml.bayes.Feature;
import com.enigmastation.ml.common.collections.MapBuilder;

import java.util.List;
import java.util.Map;

public class FisherClassifier extends SimpleClassifier {
    Map<Object, Double> minimums = new MapBuilder().defaultValue(0.0).build();

    public double getMinimum(Object category) {
        return minimums.get(category);
    }

    public void setMinimum(Object category, double v) {
        minimums.put(category, v);
    }

    public FisherClassifier() {
    }

    public FisherClassifier(Map<Object, Feature> features, Map<Object, Integer> categories) {
        super(features, categories);
    }

    public FisherClassifier(ClassifierDataFactory factory) {
        super(factory);
    }

    double featureProb(Object feature, Object category) {
        double clf = super.featureProb(feature, category);
        if (clf == 0.0) {
            return 0.0;
        }
        double frequencySum = 0.0;
        for (Object c : categories()) {
            frequencySum += super.featureProb(feature, c);
        }
        return clf / frequencySum;
    }

    double fisherProbability(Object source, Object category) {
        double p = 1.0;
        List<Object> features = getFeatures(source);
        for (Object f : features) {
            p *= weightedProb(f, category);
        }
        double fisherScore = -2.0 * Math.log(p);
        return invChi(fisherScore, features.size() * 2);
    }

    protected double invChi(double chi, double df) {
        double m = chi / 2.0;
        double sum = Math.exp(-m);
        double term = sum;
        for (int i = 1; i < df / 2; i++) {
            term *= (m / i);
            sum += term;
        }
        return Math.min(sum, 1.0);
    }

    @Override
    public Object classify(Object source, Object defaultClassification) {
        Object best = defaultClassification;
        double max = 0.0;
        for (Object c : categories()) {
            double p = fisherProbability(source, c);
            if (p > getMinimum(c) && p > max) {
                best = c;
                max = p;
            }
        }
        return best;
    }
}