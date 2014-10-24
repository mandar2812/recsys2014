package org.mandar.analysis.recsys2014.lib;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectBigList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import org.grouplens.lenskit.cursors.AbstractPollingCursor;
import org.grouplens.lenskit.cursors.Cursor;

/**
 * Created by mandar on 22/4/14.
 */
public class MongoResultSetCursor extends AbstractPollingCursor<Tweet> implements Cursor<Tweet> {
    private final ObjectListIterator<Tweet> cursor;

    public MongoResultSetCursor(DBCursor cur) {
        super(cur.size());
        ObjectArrayList<Tweet> buff = new ObjectArrayList<Tweet>();
        while(cur.hasNext()) {
            buff.add(new Tweet((BasicDBObject) cur.next()));
        }
        this.cursor = buff.iterator();
    }

    @Override
    protected Tweet poll() {
        return this.cursor.hasNext() ? cursor.next() : null;

    }

    @Override
    public int getRowCount() {
        return super.getRowCount();
    }

    @Override
    public boolean hasNext() {
        return cursor.hasNext();
    }



}
