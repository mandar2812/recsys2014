package org.mandar.analysis.recsys2014.lib;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.data.pref.Preference;

import javax.annotation.Nullable;

/**
 * Created by mandar on 22/4/14.
 */
public class Tweet implements Rating, Preference {
    private final double engagement;
    private final long user, movie, timestamp;
    private final long tweet;
    private final int movieRating;

    public Tweet(BasicDBObject rating) {
        engagement = rating.getLong(DBSettings.FIELDS.rating);
        user = rating.getLong(DBSettings.FIELDS.user);
        movie = rating.getLong(DBSettings.FIELDS.movie);
        timestamp = rating.getLong(DBSettings.FIELDS.timestamp);
        tweet = rating.getLong(DBSettings.FIELDS.item);
        movieRating = rating.getInt(DBSettings.FIELDS.movierating);
    }

    @Nullable
    @Override
    public Preference getPreference() {
        return this;
    }

    @Override
    public boolean hasValue() {
        return engagement > 0;
    }

    @Override
    public double getValue() throws IllegalStateException {
        return engagement;
    }

    @Override
    public long getUserId() {
        return  user;
    }

    @Override
    public long getItemId() {
        return movie;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public long getTweetID() { return tweet; }
}
