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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class IVPoolCacheDBHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "ivPoolCache.db";
	private static final int DATABASE_VERSION = 1;

	public static final String TABLE_IV_CACHE = "IVCache";
//	public static final String COLUMN_ID = "_id";
	
	public static final String COLUMN_SHARE = "share";
	public static final String COLUMN_HASH = "hash";
	public static final String COLUMN_IV = "iv";
	
	// Database creation sql statement
//	private static final String DATABASE_CREATE_IVCACHE = "create table " + TABLE_IV_CACHE +" (" +
//			""+COLUMN_HASH+" string primary key, "+COLUMN_IV+" blob);";
	
	private static final String DATABASE_CREATE_IVCACHE = "create table " + TABLE_IV_CACHE +" (" +
			COLUMN_SHARE + " string, " + COLUMN_HASH+" string, "+COLUMN_IV+" blob);";
	
	public IVPoolCacheDBHelper(Context context) {
//		super(context, name, factory, version);
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_CREATE_IVCACHE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(IVPoolCacheDBHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_IV_CACHE);
		onCreate(db);
	}

}
