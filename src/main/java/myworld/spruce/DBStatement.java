/*
 * Copyright 2021 MyWorld, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package myworld.spruce;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class DBStatement implements AutoCloseable {

    protected final DBContext ctx;
    protected final PreparedStatement statement;
    protected volatile boolean batch;

    protected DBStatement(DBContext ctx, PreparedStatement statement){
        this.ctx = ctx;
        this.statement = statement;
        batch = false;
    }

    public DBContext getContext(){
        return ctx;
    }

    public PreparedStatement getStatement(){
        return statement;
    }

    public DBStatement beginBatch(){
        batch = true;
        return this;
    }

    public DBStatement clearBatch() throws SQLException {
        batch = false;
        statement.clearBatch();
        return this;
    }

    public DBStatement bind(Object... values) throws SQLException {
        for(int i = 0; i < values.length; i++) {
            // NOTE: JDBC parameters are 1-indexed, not 0-indexed
            statement.setObject(i + 1, values[i]);
        }
        if(batch){
            statement.addBatch();
        }
        return this;
    }

    public long[] executeBatch() throws SQLException {
        return statement.executeLargeBatch();
    }

    public long execute(Object... values) throws SQLException {
        if(batch){
            throw new IllegalStateException("Cannot execute() a batched statement. Use executeBatch() instead.");
        }
        bind(values);
        return statement.executeLargeUpdate();
    }

    public ResultSet query(Object... values) throws SQLException {
        bind(values);
        return ctx.trackCloseable(statement.executeQuery());
    }

    public <T> T querySingle(String columnLabel, Class<T> resultType) throws SQLException {
        return querySingle(r -> r.getObject(columnLabel, resultType));
    }

    public <T> T querySingle(int columnIndex, Class<T> resultType) throws SQLException {
        return querySingle(r -> r.getObject(columnIndex, resultType));
    }

    public <T> T querySingle(Class<T> resultType) throws SQLException {
        return querySingle(1, resultType);
    }

    public <T> T querySingle(ResultUnpacker<T> unpacker) throws SQLException {
        return ctx.unpackSingle(query(), unpacker);
    }

    public <T> List<T> queryList(ResultUnpacker<T> unpacker) throws SQLException {
        return ctx.unpackList(query(), unpacker);
    }

    public <K, V> Map<K, V> queryMap(ResultUnpacker<K> keyUnpacker, ResultUnpacker<V> valueUnpacker) throws SQLException {
        return ctx.unpackMap(query(), keyUnpacker, valueUnpacker);
    }

    public long count(Object... values) throws SQLException {
        var results = query(values);
        long count = 0;
        while(results.next()){
            count++;
        }
        return count;
    }

    @Override
    public void close() throws SQLException {
        statement.close();
    }
}
