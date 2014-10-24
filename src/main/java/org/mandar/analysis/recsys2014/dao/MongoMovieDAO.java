package org.mandar.analysis.recsys2014.dao;

import com.google.common.collect.ImmutableSet;
import com.mongodb.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.grouplens.lenskit.util.DelimitedTextCursor;
import org.mandar.analysis.recsys2014.lib.Client;
import org.mandar.analysis.recsys2014.lib.CollectionName;
import org.mandar.analysis.recsys2014.lib.DBName;
import org.mandar.analysis.recsys2014.lib.DBSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by mandar on 3/6/14.
 */
public class MongoMovieDAO implements ItemTagDAO {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final MongoClient connection;
    protected boolean closeConnection;
    private final DB database;
    private final DBCollection collection;
    private transient volatile Long2ObjectMap<List<String>> tagCache;
    private transient volatile Set<String> vocabCache;

    @Inject
    public MongoMovieDAO(@Client MongoClient conn, @DBName String db, @CollectionName String coll) {
        this(conn, true, db, coll);
    }

    public MongoMovieDAO(MongoClient conn, boolean close, String db, String coll) {
        this.connection = conn;
        this.closeConnection = close;
        this.database = this.connection.getDB(db);
        this.collection = this.database.getCollection(coll);
    }

    private void ensureTagCache() {
        if (tagCache == null) {
            synchronized (this) {
                if (tagCache == null) {
                    tagCache = new Long2ObjectOpenHashMap<List<String>>();
                    ImmutableSet.Builder<String> vocabBuilder = ImmutableSet.builder();
                    DBCursor movies = this.collection.find(
                            new BasicDBObject("$fields",
                            new BasicDBObject("genre", 1)
                                    .append("_id", 0)
                                    .append("stars", 1)
                                    .append("director", 1)
                                    .append("movieID", 1)
                            )
                    );
                    logger.info("Building Tag Cache for movies");
                    while (movies.hasNext()) {
                        DBObject movie = movies.next();
                        long mid = Long.parseLong(movie.get("movieID").toString());
                        List<String> tags = tagCache.get(mid);
                        if (tags == null) {
                            tags = new ArrayList<String>();
                            tagCache.put(mid, tags);
                        }
                        // Add each of the tags in genre, stars, director
                        // into the tags object and the vocabBuilder object
                        List<String> genres, stars;
                        String director = movie.get("director").toString();
                        genres = (List<String>) movie.get("genre");
                        stars = (List<String>) movie.get("stars");
                        tags.addAll(genres);
                        tags.addAll(stars);
                        tags.add(director);
                        tagCache.put(mid, tags);
                        logger.debug("Tags for this movie are {}",tagCache.get(mid).toString());
                        vocabBuilder.addAll(genres).addAll(stars).add(director);
                    }
                    logger.info("Finished building Tag Cache");
                    vocabCache = vocabBuilder.build();
                    movies.close();
                }
            }
        }
    }

    @Override
    public List<String> getItemTags(long item) {
        ensureTagCache();
        List<String> tags = tagCache.get(item);
        if (tags != null) {
            return Collections.unmodifiableList(tags);
        } else {
            return Collections.emptyList();
        }

    }

    @Override
    public Set<String> getTagVocabulary() {
        ensureTagCache();
        return vocabCache;
    }

    @Override
    public String getItemTitle(long item) {
        DBObject movieDoc = collection.findOne(new BasicDBObject(DBSettings.FIELDS.movie, item));
        return movieDoc.get("title").toString();
    }

    @Override
    public LongSet getItemIds() {
        List movies = collection.distinct(DBSettings.FIELDS.movie);
        LongSet res = new LongArraySet(movies);
        return res;
    }

    @Override
    public String getItemDescription(long item) {
        DBObject movieDoc = collection.findOne(new BasicDBObject(DBSettings.FIELDS.movie, item));
        return movieDoc.get("description").toString();
    }

    @Override
    public String getItemStoryline(long item) {
        DBObject movieDoc = collection.findOne(new BasicDBObject(DBSettings.FIELDS.movie, item));
        return movieDoc.get("storyline").toString();
    }
}
