package io.seata.sqlparser.calcite.mysql;

import java.util.ArrayList;
import java.util.List;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLSelectRecognizer;
import io.seata.sqlparser.SQLType;
import org.apache.calcite.sql.SqlSelect;

public class MysqlSelectRecognizer implements SQLSelectRecognizer {
    SqlSelect sqlSelect;
    @Override public SQLType getSQLType() {
        return null;
    }

    @Override public String getTableAlias() {
        return null;
    }

    @Override public String getTableName() {
        return null;
    }

    @Override public String getOriginalSQL() {
        return null;
    }

    @Override public String getWhereCondition(ParametersHolder parametersHolder,
                                              ArrayList<List<Object>> paramAppenderList) {
        return null;
    }

    @Override public String getWhereCondition() {
        return null;
    }

    @Override public String getLimitCondition() {
        return null;
    }

    @Override public String getLimitCondition(ParametersHolder parametersHolder,
                                              ArrayList<List<Object>> paramAppenderList) {
        return null;
    }

    @Override public String getOrderByCondition() {
        return null;
    }

    @Override public String getOrderByCondition(ParametersHolder parametersHolder,
                                                ArrayList<List<Object>> paramAppenderList) {
        return null;
    }
}
