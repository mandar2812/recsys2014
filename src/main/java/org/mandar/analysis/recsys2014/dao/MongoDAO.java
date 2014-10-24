package org.mandar.analysis.recsys2014.dao;

import com.mongodb.*;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.grouplens.lenskit.cursors.Cursor;
import org.grouplens.lenskit.cursors.Cursors;
import org.grouplens.lenskit.data.dao.*;
import org.grouplens.lenskit.data.event.Event;
import org.grouplens.lenskit.data.event.Rating;
import org.grouplens.lenskit.data.sql.DatabaseAccessException;
import org.mandar.analysis.recsys2014.lib.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.WillCloseWhenClosed;
import javax.inject.Inject;
import java.util.List;

/**
 * Created by mandar on 21/4/14.
 */
public class MongoDAO implements EventDAO {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final MongoClient connection;
    protected boolean closeConnection;
    private final DB database;
    private final DBCollection collection;

    @Inject
    public MongoDAO(@Client MongoClient conn, @DBName String db, @CollectionName String coll) {
        this(conn, true, db, coll);
    }

    public MongoDAO(MongoClient conn,  boolean close, String db, String coll) {
        this.connection = conn;
        this.closeConnection = close;
        this.database = this.connection.getDB(db);
        this.collection = this.database.getCollection(coll);
    }

    public void close() {
        connection.close();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Cursor<Event> streamEvents() {
        return (Cursor) streamEvents(Rating.class, SortOrder.ANY);
    }

    @Override
    public <E extends Event> Cursor<E> streamEvents(Class<E> type) {
        return streamEvents(type, SortOrder.ANY);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E extends Event> Cursor<E> streamEvents(Class<E> type, SortOrder order) {
        if (!type.isAssignableFrom(Rating.class)) {
            return Cursors.empty();
        }

        try {
            DBCursor cur = collection.find();
            Cursor<E> cursor = (Cursor<E>) new MongoResultSetCursor(cur);
            cur.close();
            return cursor;
        } catch (MongoClientException e) {
            throw new DatabaseAccessException(e);
        }
    }

    public Tweet getTweetforUserMovieCombo(long userID, long movieID){
        return null;
    }

}
