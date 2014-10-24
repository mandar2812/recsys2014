package org.mandar.analysis.recsys2014;

import com.mongodb.MongoClient;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.IRStatistics;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderIRStatsEvaluator;
import org.apache.mahout.cf.taste.impl.model.mongodb.MongoDBDataModel;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.mandar.analysis.recsys2014.builders.UURecBuilder;
import org.mandar.analysis.recsys2014.eval.recsysIRStatsEvaluator;
import org.mandar.analysis.recsys2014.lib.DBSettings;
import org.mandar.analysis.recsys2014.models.NeoMongoDBDataModel;

import java.net.UnknownHostException;
import java.util.List;

/**
 * Created by mandar on 2/6/14.
 */
public class recsysEvalMahout {


    public static void main(String[] args) {
        try{
            DataModel trainmodel = new NeoMongoDBDataModel(
                    DBSettings.DBHOST, 27017, DBSettings.DATABASE,
                    DBSettings.TRAINING_COLLECTION, true, false, null,
                    DBSettings.FIELDS.user, DBSettings.FIELDS.movie,
                    DBSettings.FIELDS.rating,
                    DBSettings.MAHOUT_TRAINING_MAP
            );

            DataModel testmodel = new NeoMongoDBDataModel(
                    DBSettings.DBHOST, 27017, DBSettings.DATABASE,
                    DBSettings.TEST_COLLECTION, true, false, null,
                    DBSettings.FIELDS.user, DBSettings.FIELDS.movie,
                    DBSettings.FIELDS.rating,
                    DBSettings.MAHOUT_TEST_MAP
            );
            //MongoClient conn = new MongoClient(DBSettings.DBHOST);
            RecommenderIRStatsEvaluator evaluator = new recsysIRStatsEvaluator(testmodel);
            RecommenderBuilder builder_uu = new UURecBuilder();

            IRStatistics result_uu = evaluator.evaluate(
                    builder_uu, null, trainmodel,
                    null, 10, 0.0, 0.0
            );

            /*RecommenderBuilder builder_ii = new IIRecBuilder();
            IRStatistics result_ii = evaluator.evaluate(
                    builder_ii, null, model,
                    null, 10, 0.1, 0.8
            );*/

            /*RecommenderBuilder builder_svd = new SVDRecBuilder();
            IRStatistics result_svd = evaluator.evaluate(
                    builder_svd, null, trainmodel,
                    null, 10, 0.1, 0.8
            );*/

            System.out.println("Results for User User");
            System.out.println("Precision: "+result_uu.getPrecision());
            System.out.println("Recall: "+result_uu.getRecall());
            System.out.println("nDCG@10: "+result_uu.getNormalizedDiscountedCumulativeGain());

            /*System.out.println("Results for Item Item");
            System.out.println("Precision: "+result_ii.getPrecision());
            System.out.println("Recall: "+result_ii.getRecall());
            System.out.println("nDCG@10: "+result_ii.getNormalizedDiscountedCumulativeGain());*/

            /*System.out.println("Results for SVD");
            System.out.println("Precision: "+result_svd.getPrecision());
            System.out.println("Recall: "+result_svd.getRecall());
            System.out.println("nDCG@10: "+result_svd.getNormalizedDiscountedCumulativeGain());*/



        } catch(UnknownHostException u) {
            u.printStackTrace();
        } catch(TasteException t) {
            t.printStackTrace();
        }

    }
}
