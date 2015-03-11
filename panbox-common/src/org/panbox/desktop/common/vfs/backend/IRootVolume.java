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

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.crypto.SecretKey;

import org.panbox.core.crypto.Obfuscator;
import org.panbox.core.exception.ObfuscationException;
import org.panbox.core.keymgmt.ShareKey;
import org.panbox.core.vfs.backend.VirtualFile;
import org.panbox.desktop.common.vfs.backend.exceptions.SecretKeyNotFoundException;

public interface IRootVolume {

	public void registerShare(VFSShare share);

	public void registerShare(String userid, VFSShare share);

	public boolean removeShare(String shareName);

	public boolean removeShare(String userid, String shareName);

	public boolean existsAndChanged(VFSShare vfsShare);

	public boolean existsAndChanged(String userid, VFSShare vfsShare);

	/**
	 * Since we have more than one VirtualVolume we cannot say anything about
	 * the space! So we will turn of the information on these infos.
	 * 
	 * @return Always 0, since this disables the information on how much free
	 *         space is still left on Windows/Linux.
	 */
	public long getFreeSpace();

	/**
	 * Since we have more than one VirtualVolume we cannot say anything about
	 * the space! So we will turn of the information on these infos.
	 * 
	 * @return Always 0, since this disables the information on how much total
	 *         space exists on Windows/Linux.
	 */
	public long getTotalSpace();

	/**
	 * Since we have more than one VirtualVolume we cannot say anything about
	 * the space! So we will turn of the information on these infos.
	 * 
	 * @return Always 0, since this disables the information on how much usable
	 *         space exists on Windows/Linux.
	 */
	public long getUsableSpace();

	/**
	 * Gets the share key as SecretKey instance for the specified file. This
	 * will iterate over all shares and check which share contains this file. If
	 * the share notifies, that it contains the file, the share key of this
	 * share will be used as files' share key.
	 * 
	 * @param fileName
	 *            Relative path of the file.
	 * @return SecretKey instance of the share key of the share that contains
	 *         the specified file.
	 */
	public SecretKey getObfuscationKeyForFile(String fileName)
			throws SecretKeyNotFoundException;

	public Obfuscator getObfuscator(String fileName)
			throws SecretKeyNotFoundException;

	public SecretKey getShareKeyForFile(String fileName, int version)
			throws SecretKeyNotFoundException;

	public ShareKey getLatestShareKeyForFile(String fileName)
			throws SecretKeyNotFoundException;

	/**
	 * Gets a VirtualFile instance of the file that has been specified in the
	 * fileName parameter. This will iterate over all shares and check which
	 * share contains this file. If the share notifies, that it contains the
	 * file, a getFile-call will be placed on the share in order to forward to
	 * request to the share itself.
	 * 
	 * @param fileName
	 *            Relative path of the file.
	 * @return VirtualFile instance of the specified file.
	 * @throws IOException
	 */
	public VirtualFile getFile(String fileName) throws IOException;

	/**
	 * Resolves the filePath of a specified VirtualFile in order to get a
	 * relative path, which can be used in Panbox for Obfuscators, etc.
	 * (starting with /<ShareName>/...
	 * 
	 * @param file
	 *            VirtualFile instance of the file which relative filename
	 *            should be resolved.
	 * @return Relative path of the specified file.
	 */
	public String getRelativePathForFile(VirtualFile file)
			throws FileNotFoundException;

	public String deobfuscatePath(VirtualFile file)
			throws FileNotFoundException, ObfuscationException;

	public String obfuscatePath(String fileName, boolean createivs)
			throws FileNotFoundException, ObfuscationException;

}