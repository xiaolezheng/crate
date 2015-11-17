/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.analyze;

import com.google.common.base.Preconditions;
import io.crate.Constants;
import io.crate.analyze.symbol.Reference;
import io.crate.exceptions.InvalidColumnNameException;
import io.crate.metadata.ColumnIdent;
import io.crate.metadata.ReferenceIdent;
import io.crate.metadata.ReferenceInfo;
import io.crate.sql.tree.DefaultTraversalVisitor;
import io.crate.sql.tree.Insert;
import io.crate.sql.tree.Node;

import java.util.ArrayList;

public abstract class AbstractInsertAnalyzer extends DefaultTraversalVisitor<AbstractInsertAnalyzedStatement, Analysis> {

    protected final AnalysisMetaData analysisMetaData;

    protected AbstractInsertAnalyzer(AnalysisMetaData analysisMetaData) {
        this.analysisMetaData = analysisMetaData;
    }



    /**
     * validates the column and sets primary key / partitioned by / routing information as well as a
     * column Reference to the context.
     *
     * the created column reference is returned
     */
    protected Reference addColumn(String column, AbstractInsertAnalyzedStatement context, int i) {
        assert context.tableInfo() != null;
        return addColumn(new ReferenceIdent(context.tableInfo().ident(), column), context, i);
    }

    /**
     * validates the column and sets primary key / partitioned by / routing information as well as a
     * column Reference to the context.
     *
     * the created column reference is returned
     */
    protected Reference addColumn(ReferenceIdent ident, AbstractInsertAnalyzedStatement context, int i) {
        final ColumnIdent columnIdent = ident.columnIdent();
        Preconditions.checkArgument(!columnIdent.name().startsWith("_"), "Inserting system columns is not allowed");
        if (Constants.INVALID_COLUMN_NAME_PREDICATE.apply(columnIdent.name())) {
            throw new InvalidColumnNameException(columnIdent.name());
        }

        // set primary key column if found
        for (ColumnIdent pkIdent : context.tableInfo().primaryKey()) {
            if (pkIdent.getRoot().equals(columnIdent)) {
                context.addPrimaryKeyColumnIdx(i);
            }
        }

        // set partitioned column if found
        for (ColumnIdent partitionIdent : context.tableInfo().partitionedBy()) {
            if (partitionIdent.getRoot().equals(columnIdent)) {
                context.addPartitionedByIndex(i);
            }
        }

        // set routing if found
        ColumnIdent routing = context.tableInfo().clusteredBy();
        if (routing != null && (routing.equals(columnIdent) || routing.isChildOf(columnIdent))) {
            context.routingColumnIndex(i);
        }

        // ensure that every column is only listed once
        Reference columnReference = context.allocateUniqueReference(ident);
        context.columns().add(columnReference);
        return columnReference;
    }

    public AnalyzedStatement analyze(Node node, Analysis analysis) {
        analysis.expectsAffectedRows(true);
        return super.process(node, analysis);
    }
}
