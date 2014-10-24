package org.mandar.analysis.recsys2014.lib;

/**
 * Created by mandar on 31/5/14.
 */
import com.mongodb.DBObject;

import java.util.Comparator;

public class MongoComparator implements Comparator<DBObject>{
    @Override
    public int compare(DBObject o1, DBObject o2) {
        if(Long.parseLong(o1.get("uID").toString()) < Long.parseLong(o2.get("uID").toString())) {
            return 1;
        } else if (Long.parseLong(o1.get("uID").toString()) == Long.parseLong(o2.get("uID").toString())){
            if(Long.parseLong(o1.get("engagement").toString()) < Long.parseLong(o2.get("engagement").toString())) {
                return 1;
            } else if(Long.parseLong(o1.get("engagement").toString()) == Long.parseLong(o2.get("engagement").toString())) {
                if(Long.parseLong(o1.get("tweetID").toString()) < Long.parseLong(o2.get("tweetID").toString())) {
                    return 1;
                }
            }
        }

        return -1;
    }

}
