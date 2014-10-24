package org.mandar.analysis.recsys2014.eval;

import com.google.common.base.Preconditions;
import com.mongodb.*;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.*;
import org.apache.mahout.cf.taste.impl.common.*;
import org.apache.mahout.cf.taste.impl.eval.GenericRelevantItemsDataSplitter;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.recommender.IDRescorer;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.common.RandomUtils;
import org.mandar.analysis.recsys2014.lib.DBSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

/**
 * Created by mandar on 2/6/14.
 */
public class recsysIRStatsEvaluator implements RecommenderIRStatsEvaluator {

    private static final Logger log = LoggerFactory.getLogger(recsysIRStatsEvaluator.class);

    private static final double LOG2 = Math.log(2.0);

    /**
     * Pass as "relevanceThreshold" argument to
     * {@link #evaluate(RecommenderBuilder, DataModelBuilder, DataModel, IDRescorer, int, double, double)} to
     * have it attempt to compute a reasonable threshold. Note that this will impact performance.
     */
    private final Random random;
    private DataModel testDataModel;
    //private final RelevantItemsDataSplitter dataSplitter;
    public recsysIRStatsEvaluator(DataModel test){
        Preconditions.checkNotNull(test);
        random = RandomUtils.getRandom();
        this.testDataModel = test;
        //this.dataSplitter = new GenericRelevantItemsDataSplitter();
    }

    @Override
    public IRStatistics evaluate(RecommenderBuilder recommenderBuilder,
                                 DataModelBuilder dataModelBuilder, DataModel trainDataModel,
                                 IDRescorer rescorer, int at, double relevanceThreshold,
                                 double evaluationPercentage) throws TasteException, NullPointerException {

        Preconditions.checkArgument(recommenderBuilder != null, "recommenderBuilder is null");
        Preconditions.checkArgument(trainDataModel != null, "dataModel is null");
        Preconditions.checkArgument(at >= 1, "at must be at least 1");
        Preconditions.checkArgument(evaluationPercentage >= 0.0 && evaluationPercentage <= 1.0,
                "Invalid evaluationPercentage: " + evaluationPercentage + ". Must be: 0.0 < evaluationPercentage <= 1.0");

        int numItems = trainDataModel.getNumItems();
        RunningAverage precision = new FullRunningAverage(0, 0);
        RunningAverage recall = new FullRunningAverage(0, 0);
        RunningAverage fallOut = new FullRunningAverage(0, 0);
        RunningAverage nDCG = new FullRunningAverage(0, 0);
        int numUsersRecommendedFor = 0;
        int numUsersWithRecommendations = 0;
        Recommender recommender = recommenderBuilder.buildRecommender(trainDataModel);

        LongPrimitiveIterator it =  this.testDataModel.getUserIDs();

        while(it.hasNext()) {

            long userID = it.nextLong();

            /*if (random.nextDouble() >= evaluationPercentage) {
                // Skipped
                continue;
            }*/
            long start = System.currentTimeMillis();
            PreferenceArray prefs = trainDataModel.getPreferencesFromUser(userID);

            // List some most-preferred items that would count as (most) "relevant" results
            double theRelevanceThreshold = Double.isNaN(relevanceThreshold) ? computeThreshold(prefs) : relevanceThreshold;

            FastIDSet relevantItemIDs = this.getRelevantItemsIDs(userID, at,
                    theRelevanceThreshold);

            int numRelevantItems = relevantItemIDs.size();
            if (numRelevantItems <= 0) {
                continue;
            }

            int size = numRelevantItems + trainDataModel.getItemIDsFromUser(userID).size();
            /*if (size < 2 * at) {
                // Really not enough prefs to meaningfully evaluate this user
                continue;
            }*/


            int intersectionSize = 0;

            List<RecommendedItem> recommendedItems = recommender.recommend(userID, at, rescorer);
            for (RecommendedItem recommendedItem : recommendedItems) {
                if (relevantItemIDs.contains(recommendedItem.getItemID())) {
                    intersectionSize++;
                }
            }

            int numRecommendedItems = recommendedItems.size();

            // Precision
            if (numRecommendedItems > 0) {
                precision.addDatum((double) intersectionSize / (double) numRecommendedItems);
            }

            // Recall
            recall.addDatum((double) intersectionSize / (double) numRelevantItems);

            // Fall-out
            if (numRelevantItems < size) {
                fallOut.addDatum((double) (numRecommendedItems - intersectionSize)
                        / (double) (numItems - numRelevantItems));
            }

            // nDCG
            double cumulativeGain = 0.0;
            double idealizedGain = 0.0;

            /*for (int i = 0; i < numRecommendedItems; i++) {
                RecommendedItem item = recommendedItems.get(i);
                long itemID = item.getItemID();
                log.info("Getting pref for user {} item {}", userID, itemID);
                double test_data_rating = this.testDataModel.getPreferenceValue(userID, itemID);
                double discount = 1.0 / log2(i + 2.0); // Classical formulation says log(i+1), but i is 0-based here
                cumulativeGain += (discount*test_data_rating);

            }*/

            //Loop through the test items for a user and record the DCG
            FastIDSet testItemsforUser = this.testDataModel.getItemIDsFromUser(userID);
            PreferenceArray buffPrefArr = new GenericUserPreferenceArray(testItemsforUser.size());
            LongPrimitiveIterator itt = testItemsforUser.iterator();

            int count = 0;
            long tmpItemID;
            float tmpPref;
            while(itt.hasNext()){
                tmpItemID = itt.nextLong();
                tmpPref = recommender.estimatePreference(userID, tmpItemID);
                if(Float.isNaN(tmpPref))
                    tmpPref = 0;
                log.debug("User {} Item {} Pref {}", userID, tmpItemID, tmpPref);
                buffPrefArr.set(count, new GenericPreference(userID, tmpItemID, tmpPref));
                count++;
            }
            buffPrefArr.sortByValueReversed();
            for(int i = 0; i < buffPrefArr.length(); i++){
                long itemID = buffPrefArr.getItemID(i);
                log.info("Getting pref for user {} item {}", userID, itemID);
                double test_data_rating = this.testDataModel.getPreferenceValue(userID, itemID);
                double discount = 1.0 / log2(i + 2.0); // Classical formulation says log(i+1), but i is 0-based here
                cumulativeGain += (discount*test_data_rating);
            }

            log.info("User {}: Value of cumulativeGain = {}", userID, cumulativeGain);
            //PreferenceArray testuserPref = testDataModel.getPreferencesFromUser(userID);
            //testuserPref.sortByValueReversed();
            //Now calculate idealized DCG

            count = 0;
            LongPrimitiveIterator it2 = relevantItemIDs.iterator();
            while(it2.hasNext()) {
                long relevantitemID = it2.nextLong();

                double discount = 1.0 / log2(count + 2.0); // Classical formulation says log(i+1), but i is 0-based here

                double relevantitemEng = this.testDataModel.getPreferenceValue(userID, relevantitemID);
                idealizedGain += (discount*relevantitemEng);
                count++;
            }
            log.info("User {}: Value of idealizedGain = {}", userID, idealizedGain);

            if (idealizedGain > 0.0) {
                nDCG.addDatum(cumulativeGain / idealizedGain);
            }

            // Reach
            numUsersRecommendedFor++;
            if (numRecommendedItems > 0) {
                numUsersWithRecommendations++;
            }

            long end = System.currentTimeMillis();

            log.info("Evaluated with user {} in {}ms", userID, end - start);
            log.info("Precision/recall/fall-out/nDCG/reach: {} / {} / {} / {} / {}",
                    precision.getAverage(), recall.getAverage(), fallOut.getAverage(), nDCG.getAverage(),
                    (double) numUsersWithRecommendations / (double) numUsersRecommendedFor);

        }

        return new IRStatisticsInst(
                precision.getAverage(),
                recall.getAverage(),
                fallOut.getAverage(),
                nDCG.getAverage(),
                (double) numUsersWithRecommendations / (double) numUsersRecommendedFor);
    }

    private static double computeThreshold(PreferenceArray prefs) {
        if (prefs.length() < 2) {
            // Not enough data points -- return a threshold that allows everything
            return Double.NEGATIVE_INFINITY;
        }
        RunningAverageAndStdDev stdDev = new FullRunningAverageAndStdDev();
        int size = prefs.length();
        for (int i = 0; i < size; i++) {
            stdDev.addDatum(prefs.getValue(i));
        }
        return stdDev.getAverage() + stdDev.getStandardDeviation();
    }

    private static double log2(double value) {
        return Math.log(value) / LOG2;
    }

    private FastIDSet getRelevantItemsIDs(long userID,
                                          int at,
                                          double relevanceThreshold) throws TasteException {

        PreferenceArray prefs = this.testDataModel.getPreferencesFromUser(userID);
        FastIDSet relevantItemIDs = new FastIDSet(at);
        prefs.sortByValueReversed();
        for (int i = 0; i < prefs.length() && relevantItemIDs.size() < at; i++) {
            if (prefs.getValue(i) >= relevanceThreshold) {
                relevantItemIDs.add(prefs.getItemID(i));
            }
        }
        return relevantItemIDs;
    }
}
