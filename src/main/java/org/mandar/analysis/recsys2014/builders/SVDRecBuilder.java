package org.mandar.analysis.recsys2014.builders;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.recommender.svd.ALSWRFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDPlusPlusFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.Recommender;

/**
 * Created by mandar on 2/6/14.
 */

public class SVDRecBuilder implements RecommenderBuilder {
    @Override
    public Recommender buildRecommender(DataModel dataModel) throws TasteException {

        Factorizer factorizer = new SVDPlusPlusFactorizer(dataModel, 10, 0.001d, 0.02d, 0.05d, 50, 0.002d);
        Recommender recommender = new SVDRecommender(dataModel, factorizer);
        return recommender;
    }
}

