/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.common.collect.ImmutableSet;
import io.crate.sql.tree.*;

import java.util.Locale;
import java.util.Set;


public class SubscriptVisitor extends AstVisitor<Void, SubscriptContext> {

    private static final Long MAX_VALUE = Integer.MAX_VALUE + 1L;
    private static final Set<Class<?>> SUBSCRIPT_NAME_CLASSES = ImmutableSet.<Class<?>>of(
            SubscriptExpression.class,
            QualifiedNameReference.class,
            FunctionCall.class
    );
    private static final Set<Class<?>> SUBSCRIPT_INDEX_CLASSES = ImmutableSet.<Class<?>>of(
            StringLiteral.class,
            LongLiteral.class
    );


    @Override
    protected Void visitQualifiedNameReference(QualifiedNameReference node, SubscriptContext context) {
        context.qName(node.getName());
        return null;
    }

    @Override
    protected Void visitStringLiteral(StringLiteral node, SubscriptContext context) {
        validateNestedArrayAccess(context);
        context.add(node.getValue());
        return null;
    }


    @Override
    protected Void visitLongLiteral(LongLiteral node, SubscriptContext context) {
        validateNestedArrayAccess(context);
        long value = node.getValue();

        if (value < 1 || value > MAX_VALUE) {
            throw new UnsupportedOperationException(
                    String.format(Locale.ENGLISH, "Array index must be in range 1 to %s",
                            MAX_VALUE));
        }
        context.index((int) value);
        return null;
    }

    @Override
    protected Void visitFunctionCall(FunctionCall node, SubscriptContext context) {
        context.expression(node);
        return null;
    }

    @Override
    protected Void visitExpression(Expression node, SubscriptContext context) {
        throw new UnsupportedOperationException(String.format(
                "Expression of type %s is not supported within a subscript expression",
                node.getClass()));
    }

    private void validateNestedArrayAccess(SubscriptContext context) {
        if (context.index() != null) {
            throw new UnsupportedOperationException("Nested array access is not supported");
        }
    }
}
