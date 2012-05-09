/*
 * Copyright 2012 the original author or authors.
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

import org.iq80.leveldb.*
import org.iq80.leveldb.impl.*

import griffon.core.GriffonApplication
import griffon.util.Environment
import griffon.util.Metadata
import griffon.util.CallableWithArgs

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Andres Almiray
 */
@Singleton
final class LeveldbConnector implements LeveldbProvider {
    private bootstrap

    private static final Logger LOG = LoggerFactory.getLogger(LeveldbConnector)

    Object withLeveldb(String databaseName = 'default', Closure closure) {
        LeveldbDatabaseHolder.instance.withLeveldb(databaseName, closure)
    }

    public <T> T withLeveldb(String databaseName = 'default', CallableWithArgs<T> callable) {
        return LeveldbDatabaseHolder.instance.withLeveldb(databaseName, callable)
    }

    // ======================================================

    ConfigObject createConfig(GriffonApplication app) {
        def databaseClass = app.class.classLoader.loadClass('LeveldbConfig')
        new ConfigSlurper(Environment.current.name).parse(databaseClass)
    }

    private ConfigObject narrowConfig(ConfigObject config, String databaseName) {
        return databaseName == 'default' ? config.database : config.databases[databaseName]
    }

    DB connect(GriffonApplication app, ConfigObject config, String databaseName = 'default') {
        if (LeveldbDatabaseHolder.instance.isDatabaseConnected(databaseName)) {
            return LeveldbDatabaseHolder.instance.getDatabase(databaseName)
        }

        config = narrowConfig(config, databaseName)
        app.event('LeveldbConnectStart', [config, databaseName])
        DB database = startLeveldb(config)
        LeveldbDatabaseHolder.instance.setDatabase(databaseName, database)
        bootstrap = app.class.classLoader.loadClass('BootstrapLeveldb').newInstance()
        bootstrap.metaClass.app = app
        bootstrap.init(databaseName, database)
        app.event('LeveldbConnectEnd', [databaseName, database])
        database
    }

    void disconnect(GriffonApplication app, ConfigObject config, String databaseName = 'default') {
        if (LeveldbDatabaseHolder.instance.isDatabaseConnected(databaseName)) {
            config = narrowConfig(config, databaseName)
            DB database = LeveldbDatabaseHolder.instance.getDatabase(databaseName)
            app.event('LeveldbDisconnectStart', [config, databaseName, database])
            bootstrap.destroy(databaseName, database)
            stopLeveldb(config, database)
            app.event('LeveldbDisconnectEnd', [config, databaseName])
            LeveldbDatabaseHolder.instance.disconnectDatabase(databaseName)
        }
    }

    private DB startLeveldb(ConfigObject config) {
        Options options = new Options()
        config.options.each { key, value ->
            options."$key"(value)
        }
        
        File path = new File(config.path)
        if (!path.absolute) {
            path = new File(Metadata.current.getGriffonStartDir(), config.path)
        }
        
        return Iq80DBFactory.factory.open(path, options)
    }

    private void stopLeveldb(ConfigObject config, DB database) {
        if (!config.delete) return
	
        Options options = new Options()
        config.options.each { key, value ->
            options."$key"(value)
        }
        
        File path = new File(config.path)
        if (!path.absolute) {
            path = new File(Metadata.current.getGriffonStartDir(), config.path)
        }

        Iq80DBFactory.factory.destroy(path, options)
    }
}
