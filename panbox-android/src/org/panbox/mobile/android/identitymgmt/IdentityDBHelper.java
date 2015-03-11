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
package org.panbox.mobile.android.identitymgmt;

import org.panbox.core.identitymgmt.AbstractIdentityManager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class IdentityDBHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "identity.db";
	private static final int DATABASE_VERSION = 1;

	// Database creation sql statement
	private static final String DATABASE_CREATE_IDENTITY = "create table "
			+ AbstractIdentityManager.TABLE_IDENTITY + "(" + AbstractIdentityManager.COLUMN_ID
			+ " integer primary key autoincrement, " + AbstractIdentityManager.COLUMN_Name
			+ " string, " + AbstractIdentityManager.COLUMN_FirstName + " string, " + AbstractIdentityManager.COLUMN_Email
			+ " string, " + AbstractIdentityManager.COLUMN_KeystorePath + " string);";
	
	private static final String DATABASE_CREATE_CLOUDPROVIDER = "create table "
			+ AbstractIdentityManager.TABLE_CLOUDPROVIDER + "(" + AbstractIdentityManager.COLUMN_ID
			+ " integer primary key autoincrement, " + AbstractIdentityManager.COLUMN_Name
			+ " string, " + AbstractIdentityManager.COLUMN_Username + " string, " + AbstractIdentityManager.COLUMN_Password
			+ " string);";
	
	private static final String DATABASE_CREATE_CLOUDPROVIDER_MAP = "create table "
			+ AbstractIdentityManager.TABLE_CLOUDPROVIDER_MAP + "(" + AbstractIdentityManager.COLUMN_IdentityId
			+ " integer, " + AbstractIdentityManager.COLUMN_CloudproviderId
			+ " integer);";

	public IdentityDBHelper(Context context) {
//		super(context, name, factory, version);
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_CREATE_IDENTITY);
		db.execSQL(DATABASE_CREATE_CLOUDPROVIDER);
		db.execSQL(DATABASE_CREATE_CLOUDPROVIDER_MAP);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(IdentityDBHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + AbstractIdentityManager.TABLE_IDENTITY);
		onCreate(db);

	}

}
