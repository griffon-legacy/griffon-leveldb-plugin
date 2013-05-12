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

import java.util.Map;

/**
 * @author Andres Almiray
 */
public class LeveldbContributionAdapter implements LeveldbContributionHandler {
    private static final String DEFAULT = "default";

    private LeveldbProvider provider = DefaultLeveldbProvider.getInstance();

    public void setLeveldbProvider(LeveldbProvider provider) {
        this.provider = provider != null ? provider : DefaultLeveldbProvider.getInstance();
    }

    public LeveldbProvider getLeveldbProvider() {
        return provider;
    }

    public <R> R withLeveldb(Closure<R> closure) {
        return withLeveldb(DEFAULT, closure);
    }

    public <R> R withLeveldb(String databaseName, Closure<R> closure) {
        return provider.withLeveldb(databaseName, closure);
    }

    public <R> R withLeveldb(CallableWithArgs<R> callable) {
        return withLeveldb(DEFAULT, callable);
    }

    public <R> R withLeveldb(String databaseName, CallableWithArgs<R> callable) {
        return provider.withLeveldb(databaseName, callable);
    }
}