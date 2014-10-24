package org.mandar.analysis.recsys2014.lib;

import com.google.common.collect.ImmutableList;
import org.grouplens.lenskit.cursors.Cursor;
import org.grouplens.lenskit.cursors.GroupingCursor;
import org.grouplens.lenskit.data.history.History;
import org.grouplens.lenskit.data.history.UserHistory;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Created by mandar on 22/4/14.
 */
public class MongoUserHistoryCursor extends GroupingCursor<UserHistory<Tweet>,Tweet> {
    private ImmutableList.Builder<Tweet> builder;
    private long userId;

    public MongoUserHistoryCursor(Cursor<Tweet> base) {
        super(base);
    }


    @Override
    protected void clearGroup() {
        builder = null;
    }

    @Override
    protected boolean handleItem(Tweet event) {
        if (builder == null) {
            userId = event.getUserId();
            builder = ImmutableList.builder();
        }

        if (userId == event.getUserId()) {
            builder.add(event);
            return true;
        } else {
            return false;
        }
    }

    @Nonnull
    @Override
    protected UserHistory<Tweet> finishGroup() {
        List<Tweet> events = builder.build();
        builder = null;
        return History.forUser(userId, events);
    }
}
