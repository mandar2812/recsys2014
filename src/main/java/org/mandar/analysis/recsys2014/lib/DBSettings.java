package org.mandar.analysis.recsys2014.lib;

/**
 * Created by mandar on 22/4/14.
 */
public class DBSettings {
    public static String DATABASE = "recsys";
    public static String TRAINING_COLLECTION = "training";
    public static String TEST_COLLECTION = "test";
    public static String TEST_COLLECTION_EMPTY = "test_empty";
    public static String MOVIES_COLLECTION = "movies";
    public static String DBHOST = "localhost";
    public static String MAHOUT_TRAINING_MAP = "mongo_training_data_model_map";
    public static String MAHOUT_TEST_MAP = "mongo_test_data_model_map";
    public static String EVAL_COLLECTION = "eval_runs";

    public static class FIELDS {
        public static String user = "uID";
        public static String item = "tweetID";
        public static String rating = "engagement";
        public static String movierating = "rating";
        public static String timestamp = "timestamp";
        public static String movie = "movieID";
        public static String meta_data = "meta_data";
        public static String mahout_element = "element_id";
    };
}
