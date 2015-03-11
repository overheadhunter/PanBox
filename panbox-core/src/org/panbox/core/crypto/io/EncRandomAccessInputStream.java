/*
 * 
 *               Panbox - encryption for cloud storage 
 *      Copyright (C) 2014-2015 by Fraunhofer SIT and Sirrix AG 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additonally, third party code may be provided with notices and open source
 * licenses from communities and third parties that govern the use of those
 * portions, and any licenses granted hereunder do not alter any rights and
 * obligations you may have under such open source licenses, however, the
 * disclaimer of warranty and limitation of liability provisions of the GPLv3 
 * will apply to all the product.
 * 
 */
package org.panbox.core.crypto.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.panbox.core.exception.FileEncryptionException;

/**
 * @author palige
 * 
 *         Class implements an {@link InputStream} over
 *         {@link EncRandomAccessFile}-instances, transparently decrypting all
 *         data being read from the stream. As the {@link EncRandomAccessFile}
 *         -implementations read and write chunks of a fixed size
 *         {@link EncRandomAccessFile#CHUNK_DATA_SIZE}, one may further improve
 *         performance by encapsulating instances of this stream implementation
 *         in a {@link BufferedInputStream} with a corresponding buffer size.
 * 
 */
public class EncRandomAccessInputStream extends InputStream {

	public EncRandomAccessInputStream(EncRandomAccessFile encRandomAccessFile)
			throws IOException, FileEncryptionException {
		super();
		this.encRandomAccessFile = encRandomAccessFile;
		this.encRandomAccessFile.seek(0);
	}

	private EncRandomAccessFile encRandomAccessFile;

	@Override
	public int read() throws IOException {
		try {
			return encRandomAccessFile.read();
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}
	}

	@Override
	public int read(byte[] b) throws IOException {
		try {
			return encRandomAccessFile.read(b);
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		try {
			return encRandomAccessFile.read(b, off, len);
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}
	}

	@Override
	public long skip(long n) throws IOException {
		try {
			return encRandomAccessFile.skipBytes(0);
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}
	}

	@Override
	public int available() throws IOException {
		long len, pos;
		try {
			len = encRandomAccessFile.length();
			pos = encRandomAccessFile.getFilePointer();
			return (int) (len - pos);
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}
	}

	@Override
	public void close() throws IOException {
		try {
			encRandomAccessFile.close();
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}
	}

	@Override
	public synchronized void reset() throws IOException {
		throw new IOException("function not implemented!");
	}

	@Override
	public boolean markSupported() {
		return false;
	}

}
