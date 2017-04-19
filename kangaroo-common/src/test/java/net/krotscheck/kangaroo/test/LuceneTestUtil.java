/*
 * Copyright (c) 2017 Michael Krotscheck
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
 *
 */

package net.krotscheck.kangaroo.test;

import net.krotscheck.kangaroo.test.rule.hibernate.TestDirectoryProvider;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.hibernate.search.spi.BuildContext;
import org.mockito.Mockito;

/**
 * Lucene index introspection helper.
 *
 * @author Michael Krotscheck
 */
public final class LuceneTestUtil {

    /**
     * Utility class, private constructor.
     */
    private LuceneTestUtil() {
    }

    /**
     * Output the lucene index to the logger.
     *
     * @param indexName The name of the index to dump.
     */
    public static void dumpLuceneIndex(final String indexName) {
        // Check the lucene index
        BuildContext contextMock = Mockito.mock(BuildContext.class);
        TestDirectoryProvider p = new TestDirectoryProvider();
        p.initialize(indexName, null, contextMock);

        Directory directory = p.getDirectory();

        try {
            DirectoryReader reader = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(reader);

            Query query = new MatchAllDocsQuery();
            TopDocs tops = searcher.search(query, 1000);
            ScoreDoc[] scoreDoc = tops.scoreDocs;

            for (ScoreDoc score : scoreDoc) {
                Document d = reader.document(score.doc);
                System.out.println(String.format("Document %s: %s",
                        score.doc, d.get("id")));
                for (IndexableField field : d.getFields()) {
                    System.out.println(String.format("   %s: %s",
                            field.name(), field.stringValue()));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
