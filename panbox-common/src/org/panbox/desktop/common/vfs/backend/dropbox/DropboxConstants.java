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
package org.panbox.desktop.common.vfs.backend.dropbox;

/**
 * Created by tnolle on 01.08.14.
 */
public enum DropboxConstants {

	DB_FILESTATUS_UPTODATE, DB_FILESTATUS_UNSYNCABLE, DB_FILESTATUS_SYNCING, DB_FILESTATUS_UNWATCHED;

	public static DropboxConstants fromString(String status) {
		if (status.contains("up to date")) {
			return DB_FILESTATUS_UPTODATE;
		} else if (status.contains("unsyncable")) {
			return DB_FILESTATUS_UNSYNCABLE;
		} else if (status.contains("syncing")) {
			return DB_FILESTATUS_SYNCING;
		} else if (status.contains("unwatched")) {
			return DB_FILESTATUS_UNWATCHED;
		} else {
			return null;
		}
	}

	public static final String DB_MODE = "https";
	public static final String DB_URL = "dropbox.com";
	public static final String DB_CONTENT_URL = "https://dl.dropboxusercontent.com/";
	public static final String DB_PUBLIC_FOLDER = "/Public";
	public static final String DB_SEPARATOR = "/";

	public static final String DB_SHARE_PREFIX = "/home/";
	public static final String DB_PARAM_SHARE = "share=1";
	public static final String DB_PARAM_SHAREOPTIONS = "shareoptions=1&share_subfolder=0";

	public static final String DB_CMD = "dropbox";
	public static final String DB_CMD_PUBURL = "puburl";
	public static final String DB_CMD_FILESTATUS = "filestatus";

	public static final String APP_KEY = "0c4z87ogromgnt5";
	public static final String APP_SECRET = "bg768wuoswhk54n";

	public final static String DROPBOX_HOST_DB = "host.db";

	public final static String LOCK_SUFFIX = ".lock";
	public final static String TEMP_LOCK_SUFFIX = ".lock.temp";

	public final static int TEMP_LOCK_DURATION = 600; // 10 minutes
	public static final int MAX_TREE_SEARCH_DEPTH = 12;

	/**
	 * known paths of default dropbox deamon installations in Arch, Ubuntu,
	 * Fedora, Gentoo
	 */
	public static final String[] LINUX_DB_DEAMON_PATH = new String[] {
			"/usr/bin/dropboxd", "~/.dropbox-dist/dropboxd",
			"/var/lib/dropbox/.dropbox-dist/dropboxd", "/opt/dropbox/dropboxd" };

	/**
	 * Windows default relative installation location in %APPDATA%\Dropbox\bin
	 */
	public static final String WINDOWS_DB_BIN_PATH = "bin\\Dropbox.exe";

	public static final String LINUX_PID_FILE = "dropbox.pid";
}
