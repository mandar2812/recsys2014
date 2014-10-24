package org.mandar.analysis.recsys2014.lib;

import com.google.common.collect.ImmutableList;
import org.grouplens.lenskit.cursors.Cursor;
import org.grouplens.lenskit.cursors.GroupingCursor;
import org.grouplens.lenskit.data.event.Event;
import org.grouplens.lenskit.data.history.History;
import org.grouplens.lenskit.data.history.ItemEventCollection;

import javax.annotation.Nonnull;
import javax.annotation.WillCloseWhenClosed;
import java.util.List;

/**
 * Created by mandar on 23/4/14.
 */
public class MongoItemCollectionCursor<E extends Event> extends GroupingCursor<ItemEventCollection<E>,E> {

    private ImmutableList.Builder<E> builder;
    private long itemId;

    public MongoItemCollectionCursor(@WillCloseWhenClosed Cursor<E> cur) {
        super(cur);
    }

    @Override
    protected void clearGroup() {
        builder = null;
    }

    @Override
    protected boolean handleItem(E event) {
        if (builder == null) {
            itemId = event.getItemId();
            builder = ImmutableList.builder();
        }

        if (itemId == event.getItemId()) {
            builder.add(event);
            return true;
        } else {
            return false;
        }
    }

    @Nonnull
    @Override
    protected ItemEventCollection<E> finishGroup() {
        List<E> events = builder.build();
        builder = null;
        return History.forItem(itemId, events);
    }
}
