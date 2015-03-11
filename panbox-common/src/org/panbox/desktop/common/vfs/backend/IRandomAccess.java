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
package org.panbox.desktop.common.vfs.backend;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.crypto.SecretKey;

import org.panbox.core.crypto.io.EncRandomAccessFile;
import org.panbox.core.vfs.backend.VirtualFile;
import org.panbox.desktop.common.ex.PanboxEncryptionException;

/**
 * @author palige
 * 
 *         basic VFS backend interface defining methods to be implemented by
 *         subclasses of {@link VirtualFile} that aim at supporting random
 *         access to encrypted files by utilizing subclasses of
 *         {@link EncRandomAccessFile}
 * 
 */
public interface IRandomAccess extends Flushable, Closeable {

	public void seek(long pos) throws IOException, PanboxEncryptionException;

	public long getFilePointer() throws IOException;

	public int skipBytes(int n) throws IOException, PanboxEncryptionException;

	public void write(byte[] b) throws IOException, PanboxEncryptionException;

	public int write(long seekpos, ByteBuffer b) throws IOException,
			PanboxEncryptionException;

	public void write(byte[] b, int off, int len) throws IOException,
			PanboxEncryptionException;

	public void write(int b) throws IOException, PanboxEncryptionException;

	public int read(byte[] b, int off, int len) throws IOException,
			PanboxEncryptionException;

	public int read(byte[] b) throws IOException, PanboxEncryptionException;

	public int read(long seekpos, ByteBuffer b)
			throws PanboxEncryptionException, IOException;

	public int read() throws IOException, PanboxEncryptionException;

	public int getShareKeyVersion() throws PanboxEncryptionException,
			IOException;

	public void initWithShareKey(SecretKey shareKey)
			throws PanboxEncryptionException, IOException;

	/**
	 * @param shareKeyVersion
	 * @param shareKey
	 * @throws IOException
	 * @throws PanboxEncryptionException
	 */
	public void create(int shareKeyVersion, SecretKey shareKey)
			throws IOException, PanboxEncryptionException;

	/**
	 * @throws IOException
	 * @throws PanboxEncryptionException
	 */
	public void open() throws IOException, PanboxEncryptionException;

	public boolean isOpened();
	
	public void setLength(long newLength) throws IOException,
			PanboxEncryptionException;

	int read(long seekpos, byte[] b) throws IOException,
			PanboxEncryptionException;

	int write(long seekpos, byte[] b) throws IOException,
			PanboxEncryptionException;

}
