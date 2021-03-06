/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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
package io.crate.operation.reference.sys.shard;

import io.crate.metadata.PartitionName;
import io.crate.metadata.Schemas;
import io.crate.metadata.SimpleObjectExpression;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.index.shard.ShardId;

import java.util.regex.Matcher;

public class ShardTableNameExpression extends SimpleObjectExpression<BytesRef> {

    private final BytesRef value;

    public ShardTableNameExpression(ShardId shardId) {
        String index = shardId.getIndex();
        if (PartitionName.isPartition(index)) {
            value = new BytesRef(PartitionName.fromIndexOrTemplate(index).tableIdent().name());
        } else {
            Matcher matcher = Schemas.SCHEMA_PATTERN.matcher(index);
            if (matcher.matches()) {
                value = new BytesRef(matcher.group(2));
            } else {
                value = new BytesRef(index);
            }
        }
    }

    @Override
    public BytesRef value() {
        return value;
    }

}
