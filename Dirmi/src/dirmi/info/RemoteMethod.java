/*
 *  Copyright 2006 Brian S O'Neill
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dirmi.info;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import dirmi.Asynchronous;

import dirmi.core.Identifier;

/**
 * 
 *
 * @author Brian S O'Neill
 */
public interface RemoteMethod extends Serializable {
    /**
     * Returns the name of this method.
     */
    String getName();

    /**
     * Returns a unique identifier for this method.
     */
    Identifier getMethodID();

    /**
     * Returns the return type of this method, which is null if void.
     */
    RemoteParameter<?> getReturnType();

    /**
     * Returns the method parameters in an unmodifiable list.
     */
    List<? extends RemoteParameter<?>> getParameterTypes();

    /**
     * Returns the method exception types in an unmodifiable set.
     */
    Set<? extends RemoteParameter<? extends Throwable>> getExceptionTypes();

    /**
     * Returns true if this method is asynchronous.
     *
     * @see Asynchronous
     */
    boolean isAsynchronous();

    RemoteParameter<? extends Throwable> getRemoteFailureException();

    boolean isRemoteFailureExceptionDeclared();
}