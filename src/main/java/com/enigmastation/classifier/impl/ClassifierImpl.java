package com.enigmastation.classifier.impl;

import com.enigmastation.classifier.*;
import javolution.util.FastSet;

import java.util.Collections;
import java.util.Set;

/**
 * This is a simple Bayesian calculation class. It was ported from Python contained in the book
 * "<a href="http://www.oreilly.com/catalog/9780596529321/index.html">Programming Collective Intelligence</a>,"
 * by Toby Segaran.
 *
 * @author <a href="mailto:joeo@enigmastation.com">Joseph B. Ottinger</a>
 * @version $Revision: 36 $
 */
public class ClassifierImpl implements Classifier {
    /**
     * In Segaran's book, this is referred to as "fc"
     */
    private FeatureMap categoryFeatureMap = new FeatureMap();
    /**
     * In Segaran's book, this is referred to as "cc"
     */
    private ClassifierMap categoryDocCount = new ClassifierMap();
    protected WordLister extractor = null;
    Set<ClassifierListener> listeners = new FastSet<ClassifierListener>();
    Set<String> categories = new FastSet<String>();

    public void addListener(ClassifierListener listener) {
        listeners.add(listener);
    }

    public ClassifierImpl(WordLister w) {
        extractor = w;
    }

    public ClassifierImpl() {
        this(new StemmingWordLister());
    }

    /**
     * Increase the count of a feature/category pair.
     * <p/>
     * Direct port from Segaran's book, including method name
     *
     * @param feature  the feature (the 'word')
     * @param category the category
     */
    void incf(String feature, String category) {
        ClassifierMap fm = getCategoryFeatureMap().getFeature(feature);
        fm.incrementCategory(category);

        FeatureIncrement fi = null;
        for (ClassifierListener l : listeners) {
            if (fi == null) {
                fi = new FeatureIncrement(feature, category, fm.get(category));
            }
            l.handleFeatureUpdate(fi);
        }

    }

    /**
     * Increase the count of a category.
     * Direct port from Segaran's book, including method name
     *
     * @param category the category to increment
     */
    void incc(String category) {
        getCategoryDocCount().incrementCategory(category);

        CategoryIncrement ci = null;
        for (ClassifierListener l : listeners) {
            if (ci == null) {
                ci = new CategoryIncrement(category, getCategoryDocCount().get(category));
            }
            l.handleCategoryUpdate(ci);
        }

    }

    /**
     * Direct port from Segaran's book, including method name
     *
     * @param feature  the feature
     * @param category the category to query
     * @return the number of times a feature has appeared in a category
     */
    double fcount(String feature, String category) {
        if (getCategoryFeatureMap().containsKey(feature) && getCategoryFeatureMap().get(feature).containsKey(category)) {
            return getCategoryFeatureMap().get(feature).get(category);
        }
        return 0.0;
    }

    /**
     * Direct port from Segaran's book, including method name
     *
     * @param category the category to count items for
     * @return the number of items in a category
     */
    double catcount(String category) {
        //if (getCategoryDocCount().containsKey(category)) {
        return getCategoryDocCount().get(category);
        //}
        //return 0.0;
    }

    /**
     * Direct port from Segaran's book, including method name
     *
     * @return the total number of items
     */
    double totalcount() {
        return getCategoryDocCount().getTotalCount();
    }

    /**
     * Direct port from Segaran's book, including method name.
     *
     * @return the list of all getCategories
     */
    public final Set<String> getCategories() {
        return Collections.unmodifiableSet(categories);
    }

    public void train(Object item, String category) {
        Set<String> features = extractor.getUniqueWords(item);

        for (String f : features) {
            incf(f, category);
        }
        incc(category);
        categories.add(category);
    }

    /**
     * Convenience method for descendant classes - aids in porting from Segaran's book.
     * <p/>
     * I want to change this method to use the arithmetic exception *only* if it's rare. It's possible
     * that determining rarity might be even more expensive, though.
     *
     * @param feature  the feature to consider
     * @param category the category
     * @return the feature probability for the class
     */
    protected double fprob(String feature, String category) {
        try {
            return fcount(feature, category) / catcount(category);
        } catch (ArithmeticException ae) {
            return 0;
        }
    }

    /**
     * @param feature  the feature to consider
     * @param category the category
     * @return the feature probability for the class
     */
    public final double getFeatureProbability(String feature, String category) {
        return fprob(feature, category);
    }


    private double WEIGHT = 1.0;
    private double ASSUMED_PROBABILITY = 0.5;

    protected double weightedprob(String feature, String category) {
        return getWeightedProbability(feature, category);
    }

    /**
     * @param feature  The feature to consider
     * @param category the category to consider weight for
     * @return the weighted probability
     */
    public double getWeightedProbability(String feature, String category) {
        double basicprob = getFeatureProbability(feature, category);
        long totals = 0;
        for (String cat : getCategories()) {
            totals += fcount(feature, cat);
        }
        return ((WEIGHT * ASSUMED_PROBABILITY) + (totals * basicprob)) / (WEIGHT + totals);
    }

    public final FeatureMap getCategoryFeatureMap() {
        return categoryFeatureMap;
    }

    public final ClassifierMap getCategoryDocCount() {
        return categoryDocCount;
    }
}
