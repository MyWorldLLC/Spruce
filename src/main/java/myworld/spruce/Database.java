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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Database {
	
	protected final DataSource ds;
	
	protected final ConcurrentHashMap<Class<? extends DBModule>, DBModuleFactory<? extends DBModule>> factories;
	protected final List<DBModuleFactory<? extends DBModule>> initOrder;
	
	public Database(DataSource ds){
		this.ds = ds;
		factories = new ConcurrentHashMap<>();
		initOrder = new ArrayList<>();
	}

	public DataSource getDataSource(){
		return ds;
	}

	protected Connection getConnection() throws SQLException {
		return ds.getConnection();
	}

	public DBContext getContext() throws SQLException {
		return new DBContext(this, getConnection());
	}

	public <T extends DBModule> void register(Class<T> moduleType, DBModuleFactory<T> factory){
		factories.put(moduleType, factory);
		initOrder.add(factory);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends DBModule> DBModuleFactory<T> getFactory(Class<T> moduleType){
		DBModuleFactory<T> factory = (DBModuleFactory<T>) factories.get(moduleType);
		if(factory == null){
			throw new IllegalArgumentException("No factory registered for " + moduleType.getName());
		}
		return factory;
	}
	
	public void initFactories() throws SQLException {
		for(DBModuleFactory<?> factory : initOrder) {
			factory.init(this);
		}
	}
	
	public long execute(String query) throws SQLException {
		try(var ctx = getContext()) {
			return ctx.execute(query);
		}
	}
	
	public ResultSet query(String query, Object... values) throws SQLException {
		try(var ctx = getContext()){
			return ctx.query(query, values);
		}
	}
	
	public ResultSet query(DBStatement statement, Object... values) throws SQLException {
		try(var ctx = getContext()){
			return ctx.query(statement, values);
		}
	}
	
	public static Timestamp toTimestamp(Date date) {
		return new Timestamp(date.getTime());
	}
	
	public static Date fromTimestamp(Timestamp time) {
		return new Date(time.getTime());
	}
	
	public static Date fromTimestamp(Object time) {
		return fromTimestamp((Timestamp) time);
	}

	public static OffsetDateTime offsetDateTimeFromTimestamp(Object time){
		Timestamp timestamp = (Timestamp) time;
		return OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp.getTime()), ZoneId.systemDefault());
	}
	
}
