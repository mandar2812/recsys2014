package org.mandar.analysis.recsys2014;

/**
 * Created by mandar on 21/4/14.
 */
/*
 * Copyright 2011 University of Minnesota
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
import com.mongodb.*;

import org.apache.commons.net.ntp.TimeStamp;
import org.apache.mahout.cf.taste.impl.recommender.svd.RatingSGDFactorizer;
import org.grouplens.lenskit.ItemRecommender;
import org.grouplens.lenskit.ItemScorer;
import org.grouplens.lenskit.RatingPredictor;
import org.grouplens.lenskit.RecommenderBuildException;
import org.grouplens.lenskit.baseline.BaselineScorer;
import org.grouplens.lenskit.baseline.ItemMeanRatingItemScorer;
import org.grouplens.lenskit.baseline.UserMeanBaseline;
import org.grouplens.lenskit.baseline.UserMeanItemScorer;
import org.grouplens.lenskit.core.LenskitConfiguration;
import org.grouplens.lenskit.core.LenskitRecommender;
import org.grouplens.lenskit.core.LenskitRecommenderEngine;
import org.grouplens.lenskit.data.dao.EventDAO;
import org.grouplens.lenskit.data.dao.ItemDAO;
import org.grouplens.lenskit.iterative.*;
import org.grouplens.lenskit.knn.NeighborhoodSize;
import org.grouplens.lenskit.knn.item.ItemItemScorer;
import org.grouplens.lenskit.knn.item.ModelSize;
import org.grouplens.lenskit.knn.item.model.ItemItemModel;
import org.grouplens.lenskit.knn.user.NeighborFinder;
import org.grouplens.lenskit.knn.user.UserUserItemScorer;
import org.grouplens.lenskit.mf.funksvd.FeatureCount;
import org.grouplens.lenskit.mf.funksvd.FunkSVDItemScorer;
import org.grouplens.lenskit.scored.ScoredId;
import org.grouplens.lenskit.slopeone.DeviationDamping;
import org.grouplens.lenskit.slopeone.WeightedSlopeOneItemScorer;
import org.grouplens.lenskit.transform.normalize.*;
import org.grouplens.lenskit.transform.threshold.RealThreshold;
import org.grouplens.lenskit.transform.threshold.Threshold;
import org.grouplens.lenskit.transform.threshold.ThresholdValue;

import org.mandar.analysis.recsys2014.dao.ItemTagDAO;
import org.mandar.analysis.recsys2014.dao.MongoDAO;
import org.mandar.analysis.recsys2014.dao.MongoMovieDAO;
import org.mandar.analysis.recsys2014.lib.*;
import org.mandar.analysis.recsys2014.models.LuceneItemItemModel;
import org.mandar.analysis.recsys2014.scorers.TFIDFItemScorer;

import java.io.*;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Demonstration app for LensKit. This application builds an item-item CF model
 * from a CSV file, then generates recommendations for a user.
 *
 * Usage: java org.grouplens.lenskit.hello.recsysMain ratings.csv user
 */
public class recsysMain implements Runnable {

    private MongoClient connection;
    private String goal = "";
    private List<Long> users;
    private String algo = "uu";
    private int numNeighbours = 25;
    private int numFeatures = 25, numIterations = 120;
    private float damping = 0;
    private double regularizationParam = 0.015;
    private String stoppingCondition = "iteration";
    private double threshold = 0.01;
    private BasicDBObject algoConfig;
    private String similarityModel = "lucene";

    public static void main(String[] args) {
        recsysMain hello = new recsysMain(args);
        try {
            hello.run();
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    public void buildAlgoConfig() {
        algoConfig = new BasicDBObject();
        algoConfig.append("algorithm", algo);

        if(algo.equals("uu") || algo.equals("ii")){
            algoConfig.append("neighbours", numNeighbours);
            if(algo.equals("ii")){
                algoConfig.append("similarityModel", similarityModel);
            }
        } else if(algo.equals("svd")) {
            algoConfig.append("features", numFeatures);
            algoConfig.append("threshold", threshold);
            algoConfig.append("stoppingCondition", stoppingCondition);
            algoConfig.append("iterations", numIterations);
            algoConfig.append("regularization", regularizationParam);
        } else if (algo.equals("so")) {
            algoConfig.append("damping", damping);
        }
    }

    public void writeAlgoTestResults(double nDCG, DB db) {
        /*
        * Write the test configuration
        * parameters as well as test
        * results for nDCG @10
        */
        TimeStamp time1 = new TimeStamp(new Date());
        algoConfig.append("timestamp", time1.getTime());
        algoConfig.append("nDCG", nDCG);
        DBCollection eval_runs = db.getCollection(DBSettings.EVAL_COLLECTION);
        eval_runs.insert(algoConfig);

    }

    public recsysMain(String[] args) {
        int nextArg = 0;
        boolean done = false;
        while (!done && nextArg < args.length) {
            String arg = args[nextArg];
            algoConfig = new BasicDBObject();
            if (arg.equals("-run")) {
                goal = args[nextArg + 1];
                algo = args[nextArg + 2];
                nextArg += 3;
                if(goal.equals("training")){
                    done = true;
                }

            } else if(arg.equals("-nnbrs")) {
                numNeighbours = Integer.parseInt(args[nextArg + 1]);
                nextArg += 2;
            } else if (algo.equals("ii")) {
                if(arg.equals("-sim")){
                    similarityModel = args[nextArg + 1];
                }
                nextArg += 2;
            } else if(algo.equals("svd")) {
                if(arg.equals("-nfeatures")){
                    numFeatures = Integer.parseInt(args[nextArg + 1]);
                }
                if(arg.equals("-niterations")) {
                    numIterations = Integer.parseInt(args[nextArg + 1]);
                }
                if(arg.equals("-reg")) {
                    regularizationParam = Double.parseDouble(args[nextArg + 1]);
                }
                if(arg.equals("-stop")) {
                    stoppingCondition = args[nextArg + 1];
                }
                if(arg.equals("-threshold")) {
                    threshold = Double.parseDouble(args[nextArg + 1]);
                }
                nextArg += 2;
            } else if(algo.equals("so")) {
                if(arg.equals("-damping")){
                    damping = Float.parseFloat(args[nextArg + 1]);
                }
                nextArg += 2;
            } else if (arg.startsWith("-")) {
                throw new RuntimeException("unknown option: " + arg);
            }
        }
        this.buildAlgoConfig();
        users = new ArrayList<Long>(args.length - nextArg);
        for (; nextArg < args.length; nextArg++) {
            users.add(Long.parseLong(args[nextArg]));
        }

        try {
            this.connection = new MongoClient(DBSettings.DBHOST);
        } catch(UnknownHostException u) {
            u.printStackTrace();
            System.exit(1);
        }
    }

    @SuppressWarnings("unchecked")
    private LenskitConfiguration configureUURecommender(int neighbourhoodSize) {
        LenskitConfiguration config = new LenskitConfiguration();
        // ... and configure the item scorer.  The bind and set methods
        // are what you use to do that. Here, we want an item-item scorer.
        config.bind(ItemScorer.class)
                .to(UserUserItemScorer.class);
        // let's use personalized mean rating as the baseline/fallback predictor.
        // 2-step process:
        // First, use the user mean rating as the baseline scorer
        config.bind(BaselineScorer.class, ItemScorer.class)
                .to(UserMeanItemScorer.class);
        // Second, use the item mean rating as the base for user means
        config.bind(UserMeanBaseline.class, ItemScorer.class)
                .to(ItemMeanRatingItemScorer.class);
        // and normalize ratings by baseline prior to computing similarities
        config.within(UserVectorNormalizer.class)
                .bind(VectorNormalizer.class)
                .to(MeanCenteringVectorNormalizer.class);

        config.set(NeighborhoodSize.class).to(neighbourhoodSize);
        config.bind(Threshold.class).to(RealThreshold.class);
        config.set(ThresholdValue.class).to(0);
        return config;
    }

    @SuppressWarnings("unchecked")
    private LenskitConfiguration configureSORecommender(float devDamping) {
        LenskitConfiguration config = new LenskitConfiguration();
        // ... and configure the item scorer.  The bind and set methods
        // are what you use to do that. Here, we want an item-item scorer.
        config.bind(ItemScorer.class)
                .to(WeightedSlopeOneItemScorer.class);
        // let's use personalized mean rating as the baseline/fallback predictor.
        // 2-step process:
        // First, use the user mean rating as the baseline scorer
        config.bind(BaselineScorer.class, ItemScorer.class)
                .to(UserMeanItemScorer.class);
        // Second, use the item mean rating as the base for user means
        config.bind(UserMeanBaseline.class, ItemScorer.class)
                .to(ItemMeanRatingItemScorer.class);
        // and normalize ratings by baseline prior to computing similarities

        config.set(DeviationDamping.class).to(devDamping);

        return config;
    }

    @SuppressWarnings("unchecked")
    private LenskitConfiguration configureIIRecommender(int neighbourhoodSize, String simModel) {
        LenskitConfiguration config = new LenskitConfiguration();
        // ... and configure the item scorer.  The bind and set methods
        // are what you use to do that. Here, we want an item-item scorer.
        config.bind(ItemScorer.class)
                .to(ItemItemScorer.class);
        if(simModel.equals("lucene")) {
            config.bind(ItemItemModel.class).to(LuceneItemItemModel.class);
        } else if(simModel.equals("itemitem")) {
            config.bind(ItemItemModel.class).to(ItemItemModel.class);
        }

        // let's use personalized mean rating as the baseline/fallback predictor.
        // 2-step process:
        // First, use the user mean rating as the baseline scorer
        config.bind(BaselineScorer.class, ItemScorer.class)
                .to(UserMeanItemScorer.class);
        // Second, use the item mean rating as the base for user means
        config.bind(UserMeanBaseline.class, ItemScorer.class)
                .to(ItemMeanRatingItemScorer.class);
        // and normalize ratings by baseline prior to computing similarities
        config.bind(UserVectorNormalizer.class)
                .to(BaselineSubtractingUserVectorNormalizer.class);
        config.set(NeighborhoodSize.class).to(neighbourhoodSize);
        config.bind(Threshold.class).to(RealThreshold.class);
        config.set(ThresholdValue.class).to(0);
        config.set(ModelSize.class).to(neighbourhoodSize);
        return config;
    }

    @SuppressWarnings("unchecked")
    private LenskitConfiguration configureSVDRecommender(int num_features,
                                                         int num_iterations,
                                                         double reg,
                                                         String stop,
                                                         double thres) {
        LenskitConfiguration config = new LenskitConfiguration();
        config.bind(ItemScorer.class).to(FunkSVDItemScorer.class);

        config.bind(BaselineScorer.class, ItemScorer.class)
                .to(UserMeanItemScorer.class);
        config.bind(UserMeanBaseline.class, ItemScorer.class)
                .to(ItemMeanRatingItemScorer.class);
        config.set(FeatureCount.class).to(num_features);
        config.set(RegularizationTerm.class).to(reg);
        if(stop.equals("iteration")) {
            config.set(IterationCount.class).to(num_iterations);
        } else if(stop.equals("delta")) {
            config.bind(StoppingCondition.class).to(ThresholdStoppingCondition.class);
            config.set(MinimumIterations.class).to(num_iterations);
            config.set(StoppingThreshold.class).to(thres);
        } else if(stop.equals("error")) {
            config.bind(StoppingCondition.class).to(ErrorThresholdStoppingCondition.class);
            config.set(MinimumIterations.class).to(num_iterations);
            config.set(StoppingThreshold.class).to(thres);
        }


        return config;
    }

    @SuppressWarnings("unchecked")
    private LenskitConfiguration configureTFIDFRecommender() {
        LenskitConfiguration config = new LenskitConfiguration();
        // ... and configure the item scorer.  The bind and set methods
        // are what you use to do that. Here, we want an item-item scorer.
        config.bind(ItemScorer.class)
                .to(TFIDFItemScorer.class);

        return config;
    }

    @SuppressWarnings("unchecked")
    private LenskitConfiguration configureDAO(String coll) {
        // Reading directly from CSV files is slow, so we'll cache it in memory.
        // You can use SoftFactory here to allow ratings to be expunged and re-read
        // as memory limits demand. If you're using a database, just use it directly.


        LenskitConfiguration config = new LenskitConfiguration();
        EventDAO base = new MongoDAO(this.connection, true, DBSettings.DATABASE, coll);
        config.addComponent(base);
        if(this.algo.equals("tfidf") || this.algo.equals("ii")) {
            ItemDAO tagbase = new MongoMovieDAO(
                    this.connection, true,
                    DBSettings.DATABASE,
                    DBSettings.MOVIES_COLLECTION
            );

            config.addComponent(tagbase);
            config.addRoot(ItemTagDAO.class);
        }

        return config;
    }

    public void run() {
        // We first need to configure the data access.
        LenskitConfiguration dataConfig = this.configureDAO(DBSettings.TRAINING_COLLECTION);
        // Now we create the LensKit configuration...
        LenskitConfiguration config = new LenskitConfiguration();
        if (algo.equals("svd")){
            config = this.configureSVDRecommender(
                    numFeatures,
                    numIterations,
                    regularizationParam,
                    stoppingCondition,
                    threshold
            );
        } else if (algo.equals("ii")) {
            config = this.configureIIRecommender(numNeighbours, similarityModel);
        } else if(algo.equals("uu")) {
            config = this.configureUURecommender(numNeighbours);
        } else if(algo.equals("so")) {
            config = this.configureSORecommender(damping);
        } else if (algo.equals("tfidf")) {
            config = this.configureTFIDFRecommender();
        }

        // There are more parameters, roles, and components that can be set. See the
        // JavaDoc for each recommender algorithm for more information.
        // Now that we have a factory, build a recommender from the configuration
        // and data source. This will compute the similarity matrix and return a recommender
        // that uses it.
        LenskitRecommender rec = null;
        try {
            LenskitRecommenderEngine engine =
                    LenskitRecommenderEngine.newBuilder()
                            .addConfiguration(config)
                            .addConfiguration(dataConfig)
                            .build();
            rec = engine.createRecommender(dataConfig);

        } catch (RecommenderBuildException e) {
            e.printStackTrace();
            System.exit(1);
        }
        // we want to recommend items
        if("training".equals(this.goal)) {
            ItemRecommender irec = rec.getItemRecommender();
            assert irec != null; // not null because we configured one
            // for users
            try{
                MongoClient mongoClient = new MongoClient(DBSettings.DBHOST);
                DB db = mongoClient.getDB(DBSettings.DATABASE);
                DBCollection collection = db.getCollection(DBSettings.MOVIES_COLLECTION);
                for (long user: users) {
                    // get 10 recommendation for the user
                    List<ScoredId> recs = irec.recommend(user, 10);
                    System.out.format("Recommendations for %d:\n", user);
                    for (ScoredId item: recs) {
                        DBObject obj = collection.findOne(
                            new BasicDBObject(
                                DBSettings.FIELDS.movie, 
                                item.getId()
                                )
                            );
                        String recTitle = obj.get("title").toString();
                        String recDirector = obj.get("director").toString();
                        String recRel = obj.get("release_date").toString();
                        String recStars = obj.get("stars").toString();


                        System.out.format("\tID:%d, %s, %s Directed By: %s Starring: %s\n",
                                item.getId(), recTitle,
                                recRel, recDirector, recStars
                            );
                    }
                }
                mongoClient.close();
            } catch (UnknownHostException u){
                u.printStackTrace();
            }
        } else if("eval".equals(this.goal)) {
            //ItemScorer iscorer = rec.getItemScorer();
            RatingPredictor rat = rec.getRatingPredictor();
            File outFile = new File("data/participant_solution_"+algo+".dat");
            String line = "";
            //String cvsSplitBy = ",";
            long eng = 0;
            int count = 0;
            try{
                long lines = Utils.countLines("data/test_solution.dat");
                //outFile.delete();
                BufferedWriter brout = new BufferedWriter((new FileWriter(outFile, false)));
                //BufferedReader br = new BufferedReader(new FileReader(csvData));
                long progress = 0;
                //br.readLine();
                System.out.println("Reading from Test Set and writing result "+
                        "data/participant_solution_"+algo+".dat");

                MongoClient mongoClient = new MongoClient(DBSettings.DBHOST);
                DB db = mongoClient.getDB(DBSettings.DATABASE);
                DBCollection collection = db.getCollection(DBSettings.TEST_COLLECTION_EMPTY);
                DBCursor cur = collection.find();

                ArrayList<DBObject> arr = new ArrayList<DBObject>(cur.size());

                System.out.println("Making ObjectArrayList out of test collection result");
                while(cur.hasNext()) {
                    DBObject buff = cur.next();
                    eng = (long) Math.abs(rat.predict(Long.parseLong(buff.get("uID").toString()),
                            Long.parseLong(buff.get("movieID").toString())));
                    buff.put("engagement", eng);
                    arr.add(buff);
                    count++;
                }

                cur.close();
                //Now sort this by uID (desc), engagement (desc) and tweetID (desc)
                System.out.println("Sorting ObjectArrayList");
                Collections.sort(arr, new MongoComparator());
                for(int i = 0; i < arr.size(); i++){
                    brout.write(arr.get(i).get("uID")+","+
                            arr.get(i).get("tweetID")+","+
                            arr.get(i).get("engagement"));
                    brout.newLine();
                    progress++;
                    if((progress*100/lines) % 10 >= 0 &&
                            (progress*100/lines) % 10 <=1) {
                        System.out.println("File write Progress: " +
                                (progress * 100 / lines) + " %");
                    }
                }
                brout.close();

                ProcessBuilder pbr = new ProcessBuilder("java","-jar",
                        "rscevaluator-0.1-jar-with-dependencies.jar",
                        "data/test_solution.dat",
                        "data/participant_solution_"+algo+".dat");
                Process p = pbr.start();

                BufferedReader is = new BufferedReader(
                        new InputStreamReader(p.getInputStream())
                );
                double resultbuff = 0.0d;
                while ((line = is.readLine()) != null) {
                    if(line.contains("nDCG@10:")) {
                        resultbuff = Double.parseDouble(line.substring(9));
                    }
                    System.out.println(line);
                }
                System.out.println("Writing evaluation results to MongoDB");
                this.writeAlgoTestResults(resultbuff, db);
                mongoClient.close();
                p.waitFor();

            } catch(FileNotFoundException f){
                f.printStackTrace();
            } catch(IOException f) {
                f.printStackTrace();
            } catch(InterruptedException i) {
                i.printStackTrace();
            } catch(NullPointerException n) {
                n.printStackTrace();
            }
        }
    }
}
