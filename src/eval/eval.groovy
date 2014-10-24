import com.mongodb.MongoClient
import org.grouplens.lenskit.ItemScorer
import org.grouplens.lenskit.eval.data.GenericDataSource
import org.grouplens.lenskit.eval.metrics.predict.CoveragePredictMetric
import org.grouplens.lenskit.eval.metrics.predict.NDCGPredictMetric
import org.grouplens.lenskit.eval.metrics.predict.RMSEPredictMetric
import org.grouplens.lenskit.eval.metrics.topn.ItemSelectors
import org.grouplens.lenskit.eval.metrics.topn.NDCGTopNMetric
import org.grouplens.lenskit.iterative.IterationCount
import org.grouplens.lenskit.iterative.RegularizationTerm
import org.grouplens.lenskit.knn.NeighborhoodSize
import org.grouplens.lenskit.knn.item.*
import org.grouplens.lenskit.knn.user.*
import org.grouplens.lenskit.baseline.*
import org.grouplens.lenskit.mf.funksvd.FeatureCount
import org.grouplens.lenskit.mf.funksvd.FunkSVDItemScorer
import org.grouplens.lenskit.transform.normalize.*
import org.mandar.analysis.recsys2014.dao.MongoDAO
import org.mandar.analysis.recsys2014.lib.DBSettings
import org.mandar.analysis.recsys2014.scorers.ExtendedItemUserMeanScorer

// Let's define some algorithms
/*def svd = algorithm("SVD"){
    bind ItemScorer to FunkSVDItemScorer
    within (ItemScorer) {
        bind BaselineScorer to UserMeanItemScorer
        bind UserMeanBaseline to ItemMeanRatingItemScorer

    }

    set FeatureCount to 10
    set IterationCount to 125
    set RegularizationTerm to 0.057
}*/

def conn = new MongoClient(DBSettings.DBHOST)
// Draw a picture of the custom algorithm
/*target('draw') {
    dumpGraph {
        output "${config.analysisDir}/extended.dot"
        algorithm extended
    }
}*/

target('evaluate') {
    // Create the MongoClient, DB and Collection objects
    // Use them to spwan the DAOs for the train and test data sets


    def TrainDataDAO = new MongoDAO(conn, true, DBSettings.DATABASE, DBSettings.TRAINING_COLLECTION)
    def TestDataDAO = new MongoDAO(conn, true, DBSettings.DATABASE, DBSettings.TEST_COLLECTION)

    trainTest {
        // and just use the target as the data set. The evaluator will do the right thing.
        dataset {
            train new GenericDataSource("train", TrainDataDAO)
            test new GenericDataSource("test", TestDataDAO)
        }

        // Three different types of output for analysis.
        output "${config.analysisDir}/eval-results.csv"
        predictOutput "${config.analysisDir}/eval-preds.csv"
        userOutput "${config.analysisDir}/eval-user.csv"

        metric CoveragePredictMetric
        metric RMSEPredictMetric
        //metric new NDCGTopNMetric('nDCG10', 10, ItemSelectors.allItems(), ItemSelectors.trainingItems())
        metric NDCGPredictMetric

        algorithm("ItemItem") {
            // use the item-item rating predictor with a baseline and normalizer
            bind ItemScorer to ItemItemScorer
            bind (BaselineScorer, ItemScorer) to UserMeanItemScorer
            bind (UserMeanBaseline, ItemScorer) to ItemMeanRatingItemScorer
            bind UserVectorNormalizer to BaselineSubtractingUserVectorNormalizer
            /*bind EventDAO to MongoDAO
            set DBName to DBSettings.DATABASE
            set CollectionName to DBSettings.TRAINING_COLLECTION
            set Client to conn*/
            // retain 500 neighbors in the model, use 30 for prediction
            set ModelSize to 500
            set NeighborhoodSize to 30

            // apply some Bayesian smoothing to the mean values
            within(BaselineScorer, ItemScorer) {
                set MeanDamping to 25.0d
            }
        }

        algorithm("UserUser") {
            // use the user-user rating predictor
            bind ItemScorer to UserUserItemScorer
            bind (BaselineScorer, ItemScorer) to UserMeanItemScorer
            bind (UserMeanBaseline, ItemScorer) to ItemMeanRatingItemScorer
            bind VectorNormalizer to MeanVarianceNormalizer
            /*bind EventDAO to MongoDAO
            set DBName to DBSettings.DATABASE
            set CollectionName to DBSettings.TRAINING_COLLECTION
            set Client to conn*/
            // use 30 neighbors for predictions
            set NeighborhoodSize to 30

            // override normalizer within the neighborhood finder
            // this makes it use a different normalizer (subtract user mean) for computing
            // user similarities
            within(NeighborFinder) {
                bind UserVectorNormalizer to BaselineSubtractingUserVectorNormalizer
                // override baseline to use user mean
                bind (UserMeanBaseline, ItemScorer) to UserMeanItemScorer
            }

            // and apply some Bayesian damping to the baseline
            within(BaselineScorer, ItemScorer) {
                set MeanDamping to 25.0d
            }
        }

        algorithm("ExtendedItemUserMean") {
            bind ItemScorer to ExtendedItemUserMeanScorer
            /*bind EventDAO to MongoDAO
            set DBName to DBSettings.DATABASE
            set CollectionName to DBSettings.TRAINING_COLLECTION
            set Client to conn*/
        }




    }
}

// After running the evaluation, let's analyze the results
target('analyze') {
    requires 'evaluate'
    // Run R. Note that the script is run in the analysis directory; you might want to
    // copy all R scripts there instead of running them from the source dir.
    ant.exec(executable: 'python', dir: config.analysisDir) {
        arg value: "${config.scriptDir}/chart.py"
    }
}

// By default, run the analyze target
defaultTarget 'analyze'
