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

import griffon.core.GriffonApplication
import griffon.util.Environment
import griffon.util.Metadata
import griffon.util.ConfigUtils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.iq80.leveldb.*
import org.iq80.leveldb.impl.*

/**
 * @author Andres Almiray
 */
@Singleton
final class LeveldbConnector {
    private static final String DEFAULT = 'default'
    private static final Logger LOG = LoggerFactory.getLogger(LeveldbConnector)
    private bootstrap

    ConfigObject createConfig(GriffonApplication app) {
        if (!app.config.pluginConfig.leveldb) {
            app.config.pluginConfig.leveldb = ConfigUtils.loadConfigWithI18n('LeveldbConfig')
        }
        app.config.pluginConfig.leveldb
    }

    private ConfigObject narrowConfig(ConfigObject config, String databaseName) {
        if (config.containsKey('database') && databaseName == DEFAULT) {
            return config.database
        } else if (config.containsKey('databases')) {
            return config.databases[databaseName]
        }
        return config
    }

    DB connect(GriffonApplication app, ConfigObject config, String databaseName = DEFAULT) {
        if (DatabaseHolder.instance.isDatabaseConnected(databaseName)) {
            return DatabaseHolder.instance.getDatabase(databaseName)
        }

        config = narrowConfig(config, databaseName)
        app.event('LeveldbConnectStart', [config, databaseName])
        DB database = startLeveldb(config)
        DatabaseHolder.instance.setDatabase(databaseName, database)
        bootstrap = app.class.classLoader.loadClass('BootstrapLeveldb').newInstance()
        bootstrap.metaClass.app = app
        resolveLeveldbProvider(app).withLeveldb { dn, d -> bootstrap.init(dn, d) }
        app.event('LeveldbConnectEnd', [databaseName, database])
        database
    }

    void disconnect(GriffonApplication app, ConfigObject config, String databaseName = DEFAULT) {
        if (DatabaseHolder.instance.isDatabaseConnected(databaseName)) {
            config = narrowConfig(config, databaseName)
            DB database = DatabaseHolder.instance.getDatabase(databaseName)
            app.event('LeveldbDisconnectStart', [config, databaseName, database])
            resolveLeveldbProvider(app).withLeveldb { dn, d -> bootstrap.destroy(dn, d) }
            stopLeveldb(config)
            app.event('LeveldbDisconnectEnd', [config, databaseName])
            DatabaseHolder.instance.disconnectDatabase(databaseName)
        }
    }

    LeveldbProvider resolveLeveldbProvider(GriffonApplication app) {
        def leveldbProvider = app.config.leveldbProvider
        if (leveldbProvider instanceof Class) {
            leveldbProvider = leveldbProvider.newInstance()
            app.config.leveldbProvider = leveldbProvider
        } else if (!leveldbProvider) {
            leveldbProvider = DefaultLeveldbProvider.instance
            app.config.leveldbProvider = leveldbProvider
        }
        leveldbProvider
    }

    private DB startLeveldb(ConfigObject config) {
        def (path, options) = setupLeveldb(config)
        return Iq80DBFactory.factory.open(path, options)
    }

    private void stopLeveldb(ConfigObject config) {
        if (!config.delete) return
        def (path, options) = setupLeveldb(config)
        Iq80DBFactory.factory.destroy(path, options)
    }

    private List setupLeveldb(ConfigObject config) {
        Options options = new Options()
        config.options.each { key, value ->
            try {
                options."$key"(value)
            } catch(Exception e) {
                // ignore
            }
        }

        File path = new File(config.path)
        if (!path.absolute) {
            path = new File(Metadata.current.getGriffonStartDir(), config.path)
        }

        [path, options]
    }
}