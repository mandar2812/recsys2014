package org.mandar.analysis.recsys2014;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mongodb.*;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;
import org.mandar.analysis.recsys2014.lib.DBSettings;
import org.mandar.analysis.recsys2014.lib.Utils;

import java.io.*;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Created by mandar on 22/4/14.
 */
public class recsysPrep {
    public static void main(String[] args) {
        String tweet_json = "";
        try {
            MongoClient mongoClient = new MongoClient(DBSettings.DBHOST);
            DB db = mongoClient.getDB(DBSettings.DATABASE);
            DBCollection collection = db.getCollection(DBSettings.TRAINING_COLLECTION);

            if(args[0].equalsIgnoreCase(DBSettings.TEST_COLLECTION)){
                collection = db.getCollection(DBSettings.TEST_COLLECTION);
            } else if(args[0].equalsIgnoreCase(DBSettings.TEST_COLLECTION_EMPTY)){
                collection = db.getCollection(DBSettings.TEST_COLLECTION_EMPTY);
            }
            File csvData = new File("data/"+args[0]+".dat");
            String line = "";
            String cvsSplitBy = ",";
            long lines = Utils.countLines("data/" + args[0] + ".dat");
            BufferedReader br = new BufferedReader(new FileReader(csvData));
            long progress = 0;
            br.readLine();
            double rt_count, fav_count;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] line_parts = line.split(cvsSplitBy);
                Gson gson = new Gson();
                tweet_json = Joiner.on(",").join(Arrays.copyOfRange(line_parts, 4, line_parts.length));
                JsonObject tweet_json_data = gson.fromJson(tweet_json, JsonObject.class);
                BasicDBObject dbObject = (BasicDBObject) JSON.parse(tweet_json);

                if(tweet_json_data.get("retweet_count").getAsString().isEmpty()) {
                    rt_count = 0;
                } else {
                    rt_count = Double.parseDouble(tweet_json_data.get("retweet_count")
                            .getAsString());
                }

                if(tweet_json_data.get("favorite_count").getAsString().isEmpty()) {
                    fav_count = 0;
                } else {
                    fav_count = Double.parseDouble(tweet_json_data.get("favorite_count")
                            .getAsString());
                }

                collection.insert(new BasicDBObject(DBSettings.FIELDS.user, Long.parseLong(line_parts[0]))
                        .append(DBSettings.FIELDS.rating, rt_count + fav_count)
                        .append(DBSettings.FIELDS.item, tweet_json_data.get("id").getAsLong())
                        .append(DBSettings.FIELDS.movie, Long.parseLong(line_parts[1]))
                        .append(DBSettings.FIELDS.movierating, Long.parseLong(line_parts[2]))
                        .append(DBSettings.FIELDS.timestamp, Long.parseLong(line_parts[3]))
                        .append(DBSettings.FIELDS.meta_data, dbObject)
                );
                progress++;
                if((progress*100/lines) % 10 >= 0 && (progress*100/lines) % 10 <=5) {
                    System.out.println("File Read Progress: " + (progress * 100 / lines) + " %");
                }
            }

            mongoClient.close();
            if(br != null)
                br.close();
        } catch(MongoException m) {
            m.printStackTrace();
        } catch(UnknownHostException u) {
            System.out.println(u.toString());
            System.exit(1);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch(JSONParseException j) {
            j.printStackTrace();
        } catch(JsonSyntaxException mal) {
            System.out.println(tweet_json);
        }
    }

}
