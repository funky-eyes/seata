/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.sqlparser.druid;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLReplaceStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleMultiInsertStatement;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.SQLRecognizerFactory;
import org.apache.seata.sqlparser.druid.oracle.OracleOperateRecognizerHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * DruidSQLRecognizerFactoryImpl
 *
 */
class DruidSQLRecognizerFactoryImpl implements SQLRecognizerFactory {
    private static final int SQL_STATEMENT_CACHE_MAX_SIZE = 100;
    private static final Cache<SqlStatementCacheKey, ParsedSqlStatements> SQL_PARSE_RESULT_CACHE =
            CacheBuilder.newBuilder().maximumSize(SQL_STATEMENT_CACHE_MAX_SIZE).build();

    @Override
    public List<SQLRecognizer> create(String sql, String dbType) {
        return create(sql, dbType, false);
    }

    @Override
    public List<SQLRecognizer> create(String sql, String dbType, boolean sqlParserCacheable) {
        ParsedSqlStatements parsedSqlStatements;
        try {
            parsedSqlStatements = parseStatements(sql, dbType, sqlParserCacheable);
        } catch (RuntimeException e) {
            if (isParserException(e)) {
                throw new NotSupportYetException(
                        "not support the sql syntax: " + sql
                                + "\nplease see the doc about SQL restrictions https://seata.apache.org/zh-cn/docs/user/sqlreference/dml",
                        e);
            }
            throw e;
        }
        parsedSqlStatements.throwIfUnsupported();

        List<SQLRecognizer> recognizers = null;
        SQLRecognizer recognizer = null;
        for (SQLStatement sqlStatement : parsedSqlStatements.getSqlStatements()) {
            SQLOperateRecognizerHolder recognizerHolder =
                    SQLOperateRecognizerHolderFactory.getSQLRecognizerHolder(dbType.toLowerCase());
            if (sqlStatement instanceof SQLInsertStatement) {
                recognizer = recognizerHolder.getInsertRecognizer(sql, sqlStatement);
            } else if (sqlStatement instanceof SQLUpdateStatement) {
                recognizer = recognizerHolder.getUpdateRecognizer(sql, sqlStatement);
            } else if (sqlStatement instanceof SQLDeleteStatement) {
                recognizer = recognizerHolder.getDeleteRecognizer(sql, sqlStatement);
            } else if (sqlStatement instanceof SQLSelectStatement) {
                recognizer = recognizerHolder.getSelectForUpdateRecognizer(sql, sqlStatement);
            } else if (sqlStatement instanceof OracleMultiInsertStatement) {
                OracleMultiInsertStatement stmt = (OracleMultiInsertStatement) sqlStatement;
                if (stmt.getOption() == OracleMultiInsertStatement.Option.FIRST) {
                    throw new NotSupportYetException("INSERT FIRST not supported yet");
                }
                // Use specialized methods to handle Oracle bulk inserts
                recognizer =
                        ((OracleOperateRecognizerHolder) recognizerHolder).getMultiInsertRecognizer(sql, sqlStatement);
            }

            // When recognizer is null, it indicates that recognizerHolder cannot allocate unsupported syntax, like
            // merge and replace
            if (sqlStatement instanceof SQLReplaceStatement) {
                // just like:replace into t (id,dr) values (1,'2'), (2,'3')
                throw new NotSupportYetException(
                        "not support the sql syntax with ReplaceStatement:" + sqlStatement
                                + "\nplease see the doc about SQL restrictions https://seata.apache.org/zh-cn/docs/user/sqlreference/dml");
            }

            if (recognizer != null && recognizer.isSqlSyntaxSupports()) {
                if (recognizers == null) {
                    recognizers = new ArrayList<>();
                }
                recognizers.add(recognizer);
            }
        }
        return recognizers;
    }

    private ParsedSqlStatements parseStatements(String sql, String dbType, boolean sqlParserCacheable) {
        String adaptiveDbType = DruidDbTypeAdapter.getAdaptiveDbType(dbType);
        if (!sqlParserCacheable) {
            return parseSqlStatements(sql, adaptiveDbType);
        }

        SqlStatementCacheKey cacheKey = new SqlStatementCacheKey(sql, dbType, adaptiveDbType);
        try {
            return SQL_PARSE_RESULT_CACHE.get(cacheKey, () -> parseSqlStatements(sql, adaptiveDbType));
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        } catch (UncheckedExecutionException e) {
            throwUnchecked(e.getCause());
            throw e;
        } catch (ExecutionError e) {
            throw e;
        }
    }

    private ParsedSqlStatements parseSqlStatements(String sql, String adaptiveDbType) {
        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, adaptiveDbType);
        if (sqlStatements == null || sqlStatements.isEmpty()) {
            return ParsedSqlStatements.unsupported(Collections.emptyList(), "Unsupported SQL: " + sql);
        }
        List<SQLStatement> unmodifiableSqlStatements = Collections.unmodifiableList(sqlStatements);
        if (sqlStatements.size() > 1
                && !(sqlStatements.stream().allMatch(statement -> statement instanceof SQLUpdateStatement)
                        || sqlStatements.stream().allMatch(statement -> statement instanceof SQLDeleteStatement))) {
            return ParsedSqlStatements.unsupported(
                    unmodifiableSqlStatements, "ONLY SUPPORT SAME TYPE (UPDATE OR DELETE) MULTI SQL -" + sql);
        }
        return ParsedSqlStatements.supported(unmodifiableSqlStatements);
    }

    private void throwUnchecked(Throwable cause) {
        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }
        if (cause instanceof Error) {
            throw (Error) cause;
        }
        throw new IllegalStateException(cause);
    }

    /**
     * Check if the exception is a Druid ParserException
     * Use class name comparison to avoid directly referencing the ParserException class
     */
    private boolean isParserException(Throwable e) {
        if (e == null) {
            return false;
        }
        String className = e.getClass().getName();
        return "com.alibaba.druid.sql.parser.ParserException".equals(className);
    }

    private static final class ParsedSqlStatements {
        private final List<SQLStatement> sqlStatements;
        private final String unsupportedMessage;

        private ParsedSqlStatements(List<SQLStatement> sqlStatements, String unsupportedMessage) {
            this.sqlStatements = sqlStatements;
            this.unsupportedMessage = unsupportedMessage;
        }

        private static ParsedSqlStatements supported(List<SQLStatement> sqlStatements) {
            return new ParsedSqlStatements(sqlStatements, null);
        }

        private static ParsedSqlStatements unsupported(List<SQLStatement> sqlStatements, String unsupportedMessage) {
            return new ParsedSqlStatements(sqlStatements, unsupportedMessage);
        }

        private List<SQLStatement> getSqlStatements() {
            return sqlStatements;
        }

        private void throwIfUnsupported() {
            if (unsupportedMessage != null) {
                throw new UnsupportedOperationException(unsupportedMessage);
            }
        }
    }

    private static final class SqlStatementCacheKey {
        private final String sql;
        private final String dbType;
        private final String adaptiveDbType;

        private SqlStatementCacheKey(String sql, String dbType, String adaptiveDbType) {
            this.sql = sql;
            this.dbType = dbType;
            this.adaptiveDbType = adaptiveDbType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SqlStatementCacheKey)) {
                return false;
            }
            SqlStatementCacheKey that = (SqlStatementCacheKey) o;
            return Objects.equals(sql, that.sql)
                    && Objects.equals(dbType, that.dbType)
                    && Objects.equals(adaptiveDbType, that.adaptiveDbType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sql, dbType, adaptiveDbType);
        }
    }
}
