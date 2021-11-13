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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBContext implements AutoCloseable {

    protected final Database db;
    protected final Connection conn;
    protected final Map<Class<? extends DBModule>, DBModule> modules;
    protected final List<AutoCloseable> closeables;
    private volatile boolean inTransaction;

    protected DBContext(Database db, Connection conn){
        this.db = db;
        this.conn = conn;

        modules = new HashMap<>();
        closeables = new ArrayList<>();
    }

    public Connection getConnection(){
        return conn;
    }

    public Database getDatabase(){
        return db;
    }

    @SuppressWarnings("unchecked")
    public <T extends DBModule> T getModule(Class<T> moduleType){
        T module = (T) modules.get(moduleType);
        if(module == null){
            DBModuleFactory<T> factory = db.getFactory(moduleType);
            module = factory.create(this);
            modules.put(moduleType, module);
        }

        return module;
    }

    public void beginTransaction() throws SQLException {
        conn.setAutoCommit(false);
        inTransaction = true;
    }

    public void abortTransaction() throws SQLException {
        conn.rollback();
        conn.setAutoCommit(true);
        inTransaction = false;
    }

    public void commitTransaction() throws SQLException {
        conn.commit();
        conn.setAutoCommit(true);
        inTransaction = false;
    }

    public long execute(String query, Object... values) throws SQLException {
        return execute(prepare(query), values);
    }

    public ResultSet query(String query, Object... values) throws SQLException {
        var statement = prepare(query);
        return query(statement, values);
    }

    public ResultSet query(DBStatement statement, Object... values) throws SQLException {
        return trackCloseable(statement.query(values));
    }

    public long execute(DBStatement statement, Object... values) throws SQLException {
        return statement.execute(values);
    }

    public DBStatement prepare(String sql) throws SQLException {
        return trackCloseable(new DBStatement(this, conn.prepareStatement(sql)));
    }

    public <T> T unpackSingle(ResultSet results, ResultUnpacker<T> unpacker) throws SQLException {
        if(!results.next()){
            return null;
        }
        return unpacker.unpack(results);
    }

    public <T> List<T> unpackList(ResultSet results, ResultUnpacker<T> unpacker) throws SQLException {
        List<T> resultList = new ArrayList<>();

        while(results.next()){
            resultList.add(unpacker.unpack(results));
        }

        return resultList;
    }

    public <K, V> Map<K, V> unpackMap(ResultSet results, ResultUnpacker<K> keyUnpacker, ResultUnpacker<V> valueUnpacker) throws SQLException {
        Map<K, V> resultMap = new HashMap<>();

        while(results.next()){
            resultMap.put(keyUnpacker.unpack(results), valueUnpacker.unpack(results));
        }

        return resultMap;
    }

    protected <T extends AutoCloseable> T trackCloseable(T closeable){
        closeables.add(closeable);
        return closeable;
    }

    public void throwAsSQLException(Exception e) throws SQLException {
        if(e instanceof SQLException se){
            throw se;
        }else{
            throw new SQLException(e);
        }
    }

    @Override
    public void close() throws SQLException {
        for(var closeable : closeables){
            try{
                closeable.close();
            }catch (Exception e) {
                throw new SQLException("Context could not close DB object", e);
            }
        }
        if(inTransaction){
            throw new SQLException("Context cannot be closed with an active transaction. Abort or commit before closing.");
        }
        conn.close();
    }
}
