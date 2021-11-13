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
import java.sql.SQLException;

public abstract class DBModule implements AutoCloseable {

	protected final DBContext ctx;

	public DBModule(DBContext ctx){
		this.ctx = ctx;
	}

	public DBContext getContext(){
		return ctx;
	}

	public Connection getConnection(){
		return ctx.getConnection();
	}

	public void close() throws SQLException {}

}
