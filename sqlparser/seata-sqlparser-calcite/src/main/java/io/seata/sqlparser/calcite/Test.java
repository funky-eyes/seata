package io.seata.sqlparser.calcite;

import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.SqlUpdate;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;

public class Test {
    public static void main(String[] args) throws SqlParseException {
        String sql="update a set b=c where id>1 ";
        SqlParser.Config config=SqlParser.config().withLex(Lex.MYSQL);
        SqlParser parser=SqlParser.create(sql,config);
        SqlUpdate update = (SqlUpdate) parser.parseStmt();
    }
}
