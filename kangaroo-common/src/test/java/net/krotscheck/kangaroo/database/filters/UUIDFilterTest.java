/*
 * Copyright (c) 2016 Michael Krotscheck
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.krotscheck.kangaroo.database.filters;

import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

/**
 * Unit test for the UUID filter.
 *
 * @author Michael Krotscheck
 */
public final class UUIDFilterTest extends LuceneTestCase {

    /**
     * Name of the filter's search field.
     */
    private static final String FIELD = "user.id";

    /**
     * Test the user filter.
     *
     * @throws IOException Shouldn't be thrown.
     */
    @Test
    public void testFilteredSearch() throws IOException {
        UUID random = UUID.randomUUID();
        // Create the filter
        UUIDFilter filter = new UUIDFilter();
        filter.setIndexPath(FIELD);
        filter.setUuid(random);

        Directory directory = newDirectory();
        IndexWriter writer = new IndexWriter(directory,
                newIndexWriterConfig(new MockAnalyzer(random()))
                        .setMergePolicy(newLogMergePolicy()));

        // Build a document set.
        Document doc = new Document();
        doc.add(newStringField(FIELD, random.toString(), Field.Store.YES));
        writer.addDocument(doc);
        for (int i = 0; i < 60; i++) {
            doc = new Document();
            doc.add(newStringField(FIELD, UUID.randomUUID().toString(),
                    Field.Store.YES));
            writer.addDocument(doc);
        }
        writer.close();

        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
        booleanQuery.add(new BooleanClause(filter, Occur.FILTER));

        IndexReader reader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = newSearcher(reader);
        ScoreDoc[] hits = indexSearcher
                .search(booleanQuery.build(), 1000).scoreDocs;
        assertEquals("Number of matched documents", 1, hits.length);
        reader.close();
        directory.close();
    }
}
