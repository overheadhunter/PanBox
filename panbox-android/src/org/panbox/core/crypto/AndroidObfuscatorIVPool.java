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
package org.panbox.core.crypto;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.panbox.PanboxConstants;
import org.panbox.core.LimitedHashMap;
import org.panbox.mobile.android.dropbox.csp.DropboxConnector;
import org.panbox.mobile.android.dropbox.vfs.DropboxVirtualFile;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

public class AndroidObfuscatorIVPool extends AbstractObfuscatorIVPool {

	// private static AndroidObfuscatorIVPool instance = null;
	private DropboxConnector dbc = null;

	private static IVPoolCacheDBHelper dbHelper = null;
	private static SQLiteDatabase cacheDB = null;

	// public static AndroidObfuscatorIVPool getInstance(DropboxConnector dbc,
	// Context ctx) {
	// if (null == instance) {
	// //instance = new AndroidObfuscatorIVPool(dbc);
	// instance = new AndroidObfuscatorIVPool();
	// dbHelper = new IVPoolCacheDBHelper(ctx);
	// cacheDB = dbHelper.getWritableDatabase();
	// }
	// return instance;
	// }

	public AndroidObfuscatorIVPool(DropboxConnector dbc, Context ctx) {
		dbHelper = new IVPoolCacheDBHelper(ctx);
		cacheDB = dbHelper.getWritableDatabase();
		this.dbc = dbc;
	}

	@Override
	public byte[] getCachedIV(String lookupHash, String sharePath) {
		// 1. try to fetch it from cache in memory
		byte[] iv = super.getCachedIV(lookupHash, sharePath);

		if (null != iv) {
			// System.err.println("fetch memory cache: " + lookupHash);
			return iv;
		}

		// 2. create db connection to cache db and try to fetch the IV from
		// there
		
		Cursor c = cacheDB.query(IVPoolCacheDBHelper.TABLE_IV_CACHE,
				new String[] { IVPoolCacheDBHelper.COLUMN_IV },
				IVPoolCacheDBHelper.COLUMN_HASH + "=? and " + IVPoolCacheDBHelper.COLUMN_SHARE + "=?",
				new String[] { lookupHash, sharePath }, null, null, null);

		if (c != null) {
			if (c.moveToFirst()) {
				iv = c.getBlob(0);
				// System.err.println("fetch sqlite DB cache: " + lookupHash);
			}
			c.close();
			return iv;
		} else {
			System.err.println("Cannot query IV Pool cache.db");
		}

		// 3. (optional and we skip it for now): try single fetch (i.e. just
		// this one file) if possible

		return null;
	}

	@Override
	public void fetchIVPool(String absolutePath, String shareName) {

		long timeBefore = System.currentTimeMillis();
		String ivPath = absolutePath + File.separator + Obfuscator.IV_POOL_PATH;

		ArrayList<DropboxVirtualFile> files = dbc.listFiles(ivPath, null);

		LimitedHashMap<String, byte[]> ivs = new LimitedHashMap<String, byte[]>(
				PanboxConstants.OBFUSCATOR_IV_POOL_SIZE);

		cacheDB.beginTransactionNonExclusive();
		for (DropboxVirtualFile df : files) {
			if (!df.isDirectory()) {
				// ignore files, we only react on directories a-z and 0-9
				continue;
			}

			ArrayList<String> subDirFiles = dbc.list(ivPath + File.separator
					+ df.getFileName());

			String sql = "Insert or Replace into "
					+ IVPoolCacheDBHelper.TABLE_IV_CACHE + " ("
					+ IVPoolCacheDBHelper.COLUMN_SHARE + ", "
					+ IVPoolCacheDBHelper.COLUMN_HASH + ", "
					+ IVPoolCacheDBHelper.COLUMN_IV + ") values(?,?,?)";
			SQLiteStatement insert = cacheDB.compileStatement(sql);

			for (String fileName : subDirFiles) {
				Map.Entry<String, byte[]> e = splitFilename(fileName);
				ivs.put(e.getKey(), e.getValue());

				// create persistant cache
				insert.bindString(1, shareName);
				insert.bindString(2, e.getKey());
				insert.bindBlob(3, e.getValue());
				insert.execute();

				insert.clearBindings();

				// ContentValues values = new ContentValues();
				// values.put(IVPoolCacheDBHelper.COLUMN_HASH, e.getKey());
				// values.put(IVPoolCacheDBHelper.COLUMN_IV, e.getValue());
				//
				// String whereClause = IVPoolCacheDBHelper.COLUMN_HASH +"=?";
				// String[] whereArgs = new String[]{e.getKey()};
				// int updatedRows =
				// cacheDB.update(IVPoolCacheDBHelper.TABLE_IV_CACHE, values,
				// whereClause, whereArgs);
				//
				// if(updatedRows == 0)
				// {
				// cacheDB.insert(IVPoolCacheDBHelper.TABLE_IV_CACHE,
				// null, values);
				// }
			}
		}
		try {
			cacheDB.setTransactionSuccessful();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} finally {
			cacheDB.endTransaction();
		}

		// cacheDB.close();

		long timeAfter = System.currentTimeMillis();
		System.err.println("Time needed for IV Pool creation: "
				+ (timeAfter - timeBefore));

		// cache it in memory
		this.ivPool = ivs;
	}

}
