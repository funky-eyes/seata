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
package org.apache.seata.rm.datasource.sql;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.SQLRecognizerFactory;
import org.apache.seata.sqlparser.SqlParserType;

import java.util.List;

import static org.apache.seata.common.DefaultValues.DEFAULT_CLIENT_SQL_PARSER_CACHE_ENABLE;
import static org.apache.seata.common.DefaultValues.DEFAULT_CLIENT_SQL_PARSER_CACHE_MAX_SIZE;

public class SQLVisitorFactory {
    /**
     * SQLRecognizerFactory.
     */
    private static final SQLRecognizerFactory SQL_RECOGNIZER_FACTORY;

    private static final boolean SQL_PARSER_CACHE_ENABLE;
    private static final int SQL_PARSER_CACHE_MAX_SIZE;

    static {
        String sqlParserType = ConfigurationFactory.getInstance()
                .getConfig(ConfigurationKeys.SQL_PARSER_TYPE, SqlParserType.SQL_PARSER_TYPE_DRUID);
        SQL_PARSER_CACHE_ENABLE = ConfigurationFactory.getInstance()
                .getBoolean(ConfigurationKeys.SQL_PARSER_CACHE_ENABLE, DEFAULT_CLIENT_SQL_PARSER_CACHE_ENABLE);
        SQL_PARSER_CACHE_MAX_SIZE = ConfigurationFactory.getInstance()
                .getInt(ConfigurationKeys.SQL_PARSER_CACHE_MAX_SIZE, DEFAULT_CLIENT_SQL_PARSER_CACHE_MAX_SIZE);
        SQL_RECOGNIZER_FACTORY = EnhancedServiceLoader.load(SQLRecognizerFactory.class, sqlParserType);
    }

    /**
     * Get sql recognizer.
     *
     * @param sql    the sql
     * @param dbType the db type
     * @return the sql recognizer
     */
    public static List<SQLRecognizer> get(String sql, String dbType) {
        return SQL_RECOGNIZER_FACTORY.create(sql, dbType);
    }

    /**
     * Get sql recognizer.
     *
     * @param sql                the sql
     * @param dbType             the db type
     * @param sqlParserCacheable whether the sql is from a cacheable caller path
     * @return the sql recognizer
     */
    public static List<SQLRecognizer> get(String sql, String dbType, boolean sqlParserCacheable) {
        return SQL_RECOGNIZER_FACTORY.create(
                sql, dbType, SQL_PARSER_CACHE_ENABLE && sqlParserCacheable, SQL_PARSER_CACHE_MAX_SIZE);
    }
}
