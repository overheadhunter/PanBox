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
package org.panbox.core.csp;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author palige
 *         <p/>
 *         Interface defines several methods for the integration of Panbox with
 *         CSP client applications.
 */
public interface ICSPClientIntegration extends ICSPIntegration {
	/**
	 * file contents are in sync with the CSP
	 */
	public final static int FILE_STATUS_SYNCHRONZED = 0;

	/**
	 * file contents are currently being synchronized with the CSP
	 */
	public final static int FILE_STATUS_SYNCHRONIZING = 1;

	/**
	 * file contents could not be synchronized with the CSP due to an error
	 */
	public final static int FILE_STATUS_SYNC_ERROR = 2;

	/**
	 * file is not meant to be synchronized with the CSP
	 */
	public final static int FILE_STATUS_NOSYNC = 3;

	/**
	 * the current status of the file is unknown
	 */
	public final static int FILE_STATUS_UNKNOWN = -1;

	/**
	 * Returns the current synchronization state of the given file.
	 *
	 * @param f
	 * @return see FILE_STATUS constants
	 */
	public int getFileStatus(File f) throws Exception;

	File getCurrentSyncDir() throws IOException;

	/**
	 * Returns a list of directories whose contents are under control of a CSP
	 * client application.
	 *
	 * @return a list of directories being watched by a CSP client application,
	 *         or <code>null</code>, if the list cannot be retrieved
	 *         automatically.
	 */
	public List<File> getClientSyncDirs() throws Exception;

	/**
	 * Returns a client applications local configuration directory
	 * 
	 * @return configuration directory of the client application
	 * @throws Exception
	 *             if the client application was installed but the configuration
	 *             dir could not be read
	 */
	public File getClientConfigDir() throws Exception;

	/**
	 * Method determines whether the specific CSP client application is
	 * installed on the host system
	 * 
	 * @return <code>true</code> if a client installation has been detected,
	 *         <code>false</code> otherwise
	 * @throws Exception
	 */
	public boolean isClientInstalled() throws Exception;

	/**
	 * Method determines whether the specific CSP client application is
	 * currently running
	 * 
	 * @return
	 * @throws Exception
	 */
	public boolean isClientRunning() throws Exception;

}
