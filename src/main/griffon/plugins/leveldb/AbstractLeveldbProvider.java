/*
 * Copyright 2012-2013 the original author or authors.
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

package griffon.plugins.leveldb;

import griffon.util.CallableWithArgs;
import groovy.lang.Closure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.iq80.leveldb.DB;

import static griffon.util.GriffonNameUtils.isBlank;

/**
 * @author Andres Almiray
 */
public abstract class AbstractLeveldbProvider implements LeveldbProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractLeveldbProvider.class);
    private static final String DEFAULT = "default";

    public <R> R withLeveldb(Closure<R> closure) {
        return withLeveldb(DEFAULT, closure);
    }

    public <R> R withLeveldb(String databaseName, Closure<R> closure) {
        if (isBlank(databaseName)) databaseName = DEFAULT;
        if (closure != null) {
            DB connection = getDatabase(databaseName);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing statement on databaseName '" + databaseName + "'");
            }
            return closure.call(databaseName, connection);
        }
        return null;
    }

    public <R> R withLeveldb(CallableWithArgs<R> callable) {
        return withLeveldb(DEFAULT, callable);
    }

    public <R> R withLeveldb(String databaseName, CallableWithArgs<R> callable) {
        if (isBlank(databaseName)) databaseName = DEFAULT;
        if (callable != null) {
            DB connection = getDatabase(databaseName);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing statement on databaseName '" + databaseName + "'");
            }
            callable.setArgs(new Object[]{databaseName, connection});
            return callable.call();
        }
        return null;
    }

    protected abstract DB getDatabase(String databaseName);
}