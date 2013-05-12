/*
 * Copyright 2011-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package griffon.plugins.leveldb

import org.iq80.leveldb.DB

import griffon.core.GriffonApplication
import griffon.util.ApplicationHolder
import static griffon.util.GriffonNameUtils.isBlank

/**
 * @author Andres Almiray
 */
class DatabaseHolder {
    private static final String DEFAULT = 'default'
    private static final Object[] LOCK = new Object[0]
    private final Map<String, DB> databases = [:]

    private static final DatabaseHolder INSTANCE

    static {
        INSTANCE = new DatabaseHolder()
    }

    static DatabaseHolder getInstance() {
        INSTANCE
    }

    private DatabaseHolder() {}

    String[] getDatabaseNames() {
        List<String> databaseNames = new ArrayList().addAll(databases.keySet())
        databaseNames.toArray(new String[databaseNames.size()])
    }

    DB getDatabase(String databaseName = DEFAULT) {
        if (isBlank(databaseName)) databaseName = DEFAULT
        retrieveDatabase(databaseName)
    }

    void setDatabase(String databaseName = DEFAULT, DB database) {
        if (isBlank(databaseName)) databaseName = DEFAULT
        storeDatabase(databaseName, database)
    }

    boolean isDatabaseConnected(String databaseName) {
        if (isBlank(databaseName)) databaseName = DEFAULT
        retrieveDatabase(databaseName) != null
    }
    
    void disconnectDatabase(String databaseName) {
        if (isBlank(databaseName)) databaseName = DEFAULT
        storeDatabase(databaseName, null)
    }

    DB fetchDatabase(String databaseName) {
        if (isBlank(databaseName)) databaseName = DEFAULT
        DB database = retrieveDatabase(databaseName)
        if (database == null) {
            GriffonApplication app = ApplicationHolder.application
            ConfigObject config = LeveldbConnector.instance.createConfig(app)
            database = LeveldbConnector.instance.connect(app, config, databaseName)
        }

        if (database == null) {
            throw new IllegalArgumentException("No such leveldb database configuration for name $databaseName")
        }
        database
    }

    private DB retrieveDatabase(String databaseName) {
        synchronized(LOCK) {
            databases[databaseName]
        }
    }

    private void storeDatabase(String databaseName, DB database) {
        synchronized(LOCK) {
            databases[databaseName] = database
        }
    }
}
