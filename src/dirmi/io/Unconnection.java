/*
 *  Copyright 2007 Brian S O'Neill
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

package dirmi.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.concurrent.TimeUnit;

/**
 * 
 *
 * @author Brian S O'Neill
 */
public class Unconnection implements Connection {
    public static final Unconnection THE = new Unconnection();

    private Unconnection() {
    }

    public InputStream getInputStream() throws IOException {
        throw unconnected();
    }

    public long getReadTimeout() throws IOException {
        return 0;
    }

    public TimeUnit getReadTimeoutUnit() throws IOException {
        return TimeUnit.NANOSECONDS;
    }

    public void setReadTimeout(long time, TimeUnit unit) throws IOException {
    }

    public OutputStream getOutputStream() throws IOException {
        throw unconnected();
    }

    public long getWriteTimeout() throws IOException {
        return 0;
    }

    public TimeUnit getWriteTimeoutUnit() throws IOException {
        return TimeUnit.NANOSECONDS;
    }

    public void setWriteTimeout(long time, TimeUnit unit) throws IOException {
    }

    public String getLocalAddressString() {
        return null;
    }

    public String getRemoteAddressString() {
        return null;
    }

    public void close() throws IOException {
    }

    private IOException unconnected() {
        return new IOException("unconnected");
    }
}
