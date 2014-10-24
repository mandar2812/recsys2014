package org.mandar.analysis.recsys2014.models;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.grouplens.lenskit.knn.item.ModelSize;
import org.mandar.analysis.recsys2014.dao.ItemTagDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class LuceneModelBuilder implements Provider<LuceneItemItemModel> {
    private static final Logger logger = LoggerFactory.getLogger(LuceneModelBuilder.class);
    private final ItemTagDAO dao;
    private final int modelNeighborCount;

    @Inject
    public LuceneModelBuilder(ItemTagDAO dao, @ModelSize int nnbrs) {
        this.dao = dao;
        this.modelNeighborCount = nnbrs;
    }

    @Override
    public LuceneItemItemModel get() {
        Directory dir = new RAMDirectory();

        try {
            writeMovies(dir);
        } catch (IOException e) {
            throw new RuntimeException("I/O error writing movie model", e);
        }
        return new LuceneItemItemModel(dir, dao, modelNeighborCount);
    }

    private void writeMovies(Directory dir) throws IOException {
        Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_48);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_48, analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        IndexWriter writer = new IndexWriter(dir, config);
        try {
            logger.info("Building Lucene movie model");
            for (long movie: dao.getItemIds()) {
                logger.debug("building model for {}", movie);
                Document doc = makeMovieDocument(movie);
                writer.addDocument(doc);
            }
        } finally {
            writer.close();
        }
    }

    private Document makeMovieDocument(long movieId) {
        Document doc = new Document();
        Field movie, title, tags, description, storyline;

        movie = new StringField("movie",
                Long.toString(movieId),
                Field.Store.YES);

        title = new StringField("title",
                dao.getItemTitle(movieId),
                Field.Store.YES);

        tags = new TextField("tags",
                StringUtils.join(dao.getItemTags(movieId), "\n"),
                Field.Store.YES);
        tags.setBoost(2.0f);

        description = new TextField("description",
                dao.getItemDescription(movieId),
                Field.Store.YES);
        description.setBoost(1.0f);

        storyline = new TextField("storyline",
                dao.getItemStoryline(movieId),
                Field.Store.YES);
        storyline.setBoost(1.5f);

        doc.add(movie);
        doc.add(title);
        doc.add(tags);
        doc.add(description);
        doc.add(storyline);

        return doc;
    }
}
