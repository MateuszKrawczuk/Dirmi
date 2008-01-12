/*
 *  Copyright 2008 Brian S O'Neill
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 *
 * @author Brian S O'Neill
 */
public abstract class AbstractBufferedInputStream extends InputStream {
    private final byte[] mBuffer;

    private int mStart;
    private int mEnd;

    public AbstractBufferedInputStream() {
        this(8192);
    }

    public AbstractBufferedInputStream(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        mBuffer = new byte[size];
    }

    public synchronized int read() throws IOException {
        int start = mStart;
        if (mEnd - start > 0) {
            mStart = start + 1;
            return mBuffer[start] & 0xff;
        } else {
            byte[] buffer = mBuffer;
            int amt = doRead(buffer, 0, buffer.length);
            if (amt <= 0) {
                return -1;
            }
            mStart = 1;
            mEnd = amt;
            return buffer[0] & 0xff;
        }
    }

    public synchronized int read(byte[] b, int off, int len) throws IOException {
        int start = mStart;
        int avail = mEnd - start;
        if (avail >= len) {
            System.arraycopy(mBuffer, start, b, off, len);
            mStart = start + len;
            return len;
        } else {
            System.arraycopy(mBuffer, start, b, off, avail);
            mStart = 0;
            mEnd = 0;
            off += avail;
            len -= avail;
            if (len >= mBuffer.length) {
                int amt = doRead(b, off, len);
                amt = (amt <= 0 ? 0 : amt) + avail;
                return amt <= 0 ? -1 : amt;
            } else {
                byte[] buffer = mBuffer;
                int amt = doRead(buffer, 0, buffer.length);
                if (amt <= 0) {
                    return avail <= 0 ? -1 : avail;
                } else {
                    int fill = Math.min(amt, len);
                    System.arraycopy(buffer, 0, b, off, fill);
                    mStart = fill;
                    mEnd = amt;
                    return fill + avail;
                }
            }
        }
    }

    public synchronized long skip(long n) throws IOException {
        if (n <= 0) {
            return doSkip(n);
        } else {
            int avail = mEnd - mStart;
            if (avail <= 0) {
                return doSkip(n);
            } else {
                long skipped = (avail < n) ? avail : n;
                mStart += skipped;
                return skipped;
            }
        }
    }

    public synchronized int available() throws IOException {
        return mEnd - mStart;
    }

    /**
     * @return actual amount read; is negative if EOF reached.
     */
    public int readFully(byte[] b) throws IOException {
        return readFully(b, 0, b.length);
    }

    /**
     * @return actual amount read; is negative if EOF reached.
     */
    public synchronized int readFully(byte[] b, int off, int len) throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }
        int total = 0;
        while (len > 0) {
            int amt = read(b, off, len);
            if (amt <= 0) {
                return ~total;
            } else {
                off += amt;
                len -= amt;
                total += amt;
            }
        }
        return total;
    }

    public void mark(int readlimit) {
    }

    public void reset() throws IOException {
        throw new IOException("unsupported");
    }

    public boolean markSupported() {
        return false;
    }

    protected abstract int doRead(byte[] buffer, int offset, int length) throws IOException;

    protected abstract long doSkip(long n) throws IOException;
}
