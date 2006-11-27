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

package dirmi.core;

import dirmi.io.Broker;

/**
 * Supports accepting connections and making new connections.
 *
 * @author Brian S O'Neill
 */
public interface RemoteBroker extends Broker {
    /**
     * Returns a RemoteConnecter for creating new client-side connections.
     */
    RemoteConnecter connecter();

    /**
     * Returns an RemoteAccepter for accepting new server-side connections.
     */
    RemoteAccepter accepter();
}