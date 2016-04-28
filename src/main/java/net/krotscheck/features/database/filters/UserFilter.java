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

package net.krotscheck.features.database.filters;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.DocIdSetBuilder;

import java.io.IOException;

/**
 * A lucene search filter that only permits entities that correspond to a
 * certain user ID.
 *
 * @author Michael Krotscheck
 */
public final class UserFilter extends Filter {

    /**
     * Set the search owner id.
     *
     * @param userId The userID.
     */
    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    /**
     * The searching owner id.
     */
    private Long userId;

    /**
     * Return a doc ID set of datasets that are published.
     *
     * @param context The index context.
     * @return A bitset of published results
     * @throws IOException Thrown when reading the index fails.
     */
    @Override
    public DocIdSet getDocIdSet(final LeafReaderContext context,
                                final Bits bits) throws IOException {
        IndexReader reader = context.reader();
        IndexSearcher indexSearcher = new IndexSearcher(reader);

        TermQuery query = new TermQuery(new Term("user.id", userId.toString()));

        TopDocs docs = indexSearcher.search(query, reader.maxDoc());
        DocIdSetBuilder b = new DocIdSetBuilder(reader.maxDoc());
        for (ScoreDoc doc : docs.scoreDocs) {
            b.add(doc.doc);
        }

        return b.build();
    }

    /**
     * ToString.
     *
     * @param field The field name.
     */
    @Override
    public String toString(final String field) {
        return "UserFilter[" + field + ": " + userId + "]";
    }
}
