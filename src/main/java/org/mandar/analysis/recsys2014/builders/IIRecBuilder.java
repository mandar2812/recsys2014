package org.mandar.analysis.recsys2014.builders;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.recommender.CachingRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.GenericItemSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;

import java.util.Collection;

/**
 * Created by mandar on 2/6/14.
 */
public class IIRecBuilder implements RecommenderBuilder {
    @Override
    public Recommender buildRecommender(DataModel dataModel) throws TasteException {

        ItemSimilarity similarity = new PearsonCorrelationSimilarity(dataModel);
        //Collection<GenericItemSimilarity.ItemItemSimilarity> correlations = dataModel;
        //ItemSimilarity similarity = new GenericItemSimilarity(correlations);
        Recommender recommender = new GenericItemBasedRecommender(dataModel, similarity);
        return new CachingRecommender(recommender);
    }
}
