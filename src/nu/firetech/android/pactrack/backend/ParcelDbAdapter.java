/*
 * Copyright (C) 2011 Joakim Andersson
 * 
 * This file is part of PactrackDroid, an Android application to keep
 * track of parcels sent with the Swedish mail service (Posten).
 * 
 * PactrackDroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * PactrackDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package nu.firetech.android.pactrack.backend;

import java.util.Calendar;

import nu.firetech.android.pactrack.common.Error;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ParcelDbAdapter {
	private static final String TAG = "<PactrackDroid> ParcelDbAdapter";
	
	public static final String KEY_ROWID = "_id";

	//Parcels table
	public static final String KEY_PARCEL = "parcelid";
	public static final String KEY_CUSTOMER = "customer";
	public static final String KEY_SENT = "sent";
	public static final String KEY_WEIGHT = "weight";
	public static final String KEY_POSTAL = "postal";
	public static final String KEY_SERVICE = "service";
	public static final String KEY_STATUS = "status";
	public static final String KEY_STATUSCODE = "status_code";
	public static final String KEY_UPDATE = "last_update";
	public static final String KEY_OK_UPDATE = "last_successful_update";
	public static final String KEY_ERROR = "error_code";
	public static final String KEY_AUTO = "auto_included";

	//Events table
	public static final String KEY_FOREIGN = "parcel_id";
	public static final String KEY_LOC = "location";
	public static final String KEY_DESC = "description";
	public static final String KEY_TIME = "time";
	public static final String KEY_ERREV = "error_event";

	//Special
	public static final String KEY_CUSTOM = "custom_field";

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private static final String DATABASE_NAME = "parcels.db";
	private static final String PARCEL_TABLE = "parcels";
	private static final String EVENT_TABLE= "events";
	private static final int DATABASE_VERSION = 5;


	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("create table "+PARCEL_TABLE
					+ " ("+KEY_ROWID+" integer primary key autoincrement, "
					+KEY_PARCEL+" varchar(20) not null, "        
					+KEY_CUSTOMER+" text, "
					+KEY_SENT+" varchar(10), "
					+KEY_WEIGHT+" float, "
					+KEY_POSTAL+" text, "
					+KEY_SERVICE+" text, "
					+KEY_STATUS+" text, "
					+KEY_STATUSCODE+" integer, "
					+KEY_UPDATE+" varchar(19), "
					+KEY_OK_UPDATE+" varchar(19), "
					+KEY_ERROR+" integer default "+Error.NONE+", "
					+KEY_AUTO+" integer(1) default 1);");
			db.execSQL("create table "+EVENT_TABLE
					+ " ("+KEY_ROWID+" integer primary key autoincrement, "
					+KEY_FOREIGN+" integer not null,"
					+KEY_LOC+" text not null,"
					+KEY_DESC+" text not null,"
					+KEY_TIME+" varchar(16) not null,"
					+KEY_ERREV+" integer(1) default 0);");

			// Create an index to efficiently access positions for a particular parcel.
			db.execSQL("CREATE INDEX idx_positions ON "+EVENT_TABLE+" ("+KEY_FOREIGN+");");
			
			// Create an index to prevent duplicate events.
			db.execSQL("CREATE UNIQUE INDEX idx_unique ON "+EVENT_TABLE+" ("+KEY_FOREIGN+", "+KEY_LOC+", "+KEY_DESC+", "+KEY_TIME+");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d(TAG, "Trying to upgrade database from version " + oldVersion + " to " + newVersion);
			if (oldVersion < 2) {
				db.execSQL("ALTER TABLE "+PARCEL_TABLE+" ADD "+KEY_UPDATE+" varchar(19)");
				db.execSQL("ALTER TABLE "+PARCEL_TABLE+" ADD "+KEY_OK_UPDATE+" varchar(19)");
				oldVersion = 2;
			}
			if (oldVersion < 3) {
				db.execSQL("ALTER TABLE "+PARCEL_TABLE+" ADD "+KEY_STATUSCODE+" integer");
				oldVersion = 3;
			}
			if (oldVersion < 4) {
				db.execSQL("ALTER TABLE "+PARCEL_TABLE+" ADD "+KEY_AUTO+" integer(1) default 1");
				oldVersion = 4;
			}
			if (oldVersion < 5) {
				db.execSQL("ALTER TABLE "+EVENT_TABLE+" ADD "+KEY_ERREV+" integer(1) default 0");
				oldVersion = 5;
			}
			
			if (oldVersion == newVersion) {
				Log.d(TAG, "Database upgrade successful");
			} else {
				Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + " unsuccessful, creating a new database and destroying all old data");
				db.execSQL("DROP TABLE IF EXISTS "+PARCEL_TABLE);
				db.execSQL("DROP TABLE IF EXISTS "+EVENT_TABLE);
				onCreate(db);
			}
		}
	}

	public ParcelDbAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	public ParcelDbAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
		mDbHelper = null;
	}
	
	public boolean isOpen() {
		return (mDbHelper != null);
	}
	
	public long getNumAutoParcels() {
		Cursor data = mDb.query(PARCEL_TABLE,
				new String[] { "COUNT("+KEY_ROWID+") AS "+KEY_CUSTOM },
				KEY_AUTO + "=1", null, null, null, null);
		data.moveToFirst();
		long count = data.getLong(data.getColumnIndexOrThrow(KEY_CUSTOM));
		data.close();
		
		return count;
	}

	public long addParcel(String parcelid) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_PARCEL, parcelid);
		
		long rowId = mDb.insert(PARCEL_TABLE, null, initialValues);

		// Only start the service if there was no previous parcel in the database
		if (rowId != -1 && getNumAutoParcels() < 2) {
			ServiceStarter.startService(mCtx, null);
		}

		return rowId;
	}

	public boolean deleteParcel(long rowId) {
		boolean deleted = mDb.delete(PARCEL_TABLE, KEY_ROWID + "=" + rowId, null) > 0;

		if (deleted) {
			mDb.delete(EVENT_TABLE, KEY_FOREIGN + "=" + rowId, null);
		
			// Stop the service if database is empty
			if (getNumAutoParcels() < 1) {
				// We can bypass the preference lookup since we want to stop the service here
				ServiceStarter.startService(mCtx, null, 0);
			}
		}

		return deleted;
	}

	public Cursor fetchAllParcels(boolean autoOnly) {
		return mDb.query(PARCEL_TABLE,
				new String[] {
					KEY_ROWID,
					KEY_PARCEL,
					KEY_CUSTOMER,
					KEY_SENT,
					KEY_WEIGHT,
					KEY_POSTAL,
					KEY_SERVICE,
					KEY_STATUS,
					KEY_STATUSCODE,
					KEY_UPDATE,
					KEY_OK_UPDATE,
					KEY_ERROR,
					KEY_AUTO
				}, (autoOnly ? KEY_AUTO + "=1" : null), null, null, null, KEY_PARCEL);
	}

	public Cursor fetchParcel(long rowId) throws SQLException {
		Cursor parcel =
			mDb.query(true, PARCEL_TABLE,
					new String[] {
						KEY_ROWID,
						KEY_PARCEL,
						KEY_CUSTOMER,
						KEY_SENT,
						KEY_WEIGHT,
						KEY_POSTAL,
						KEY_SERVICE,
						KEY_STATUS,
						KEY_STATUSCODE,
						KEY_UPDATE,
						KEY_OK_UPDATE,
						KEY_ERROR,
						KEY_AUTO
					}, KEY_ROWID + "=" + rowId, null, null, null, null, null);

		if (parcel != null) {
			parcel.moveToFirst();
		}
		return parcel;
	}

	public boolean changeParcelId(long rowId, String newValue) {
		ContentValues args = new ContentValues();
		args.put(KEY_PARCEL, newValue);

		boolean updated = mDb.update(PARCEL_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;

		if (updated) {
			mDb.delete(EVENT_TABLE, KEY_FOREIGN + " = " + rowId, null);
		}

		return updated;   
	}
	
	public boolean getAutoUpdate(long rowId) {
		Cursor parcel = mDb.query(PARCEL_TABLE, new String[] { KEY_AUTO }, KEY_ROWID + "=" + rowId, null, null, null, null);
		parcel.moveToFirst();
		boolean auto = parcel.getInt(parcel.getColumnIndexOrThrow(KEY_AUTO)) == 1;
		parcel.close();
		
		return auto;
	}
	
	public boolean setAutoUpdate(long rowId, boolean newValue) {
		ContentValues args = new ContentValues();
		args.put(KEY_AUTO, newValue);
		
		boolean updated = mDb.update(PARCEL_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
		
		if (updated) {
			long numAutoParcels = getNumAutoParcels();
			if (newValue && numAutoParcels < 2) { // Start the service if this is a "new" parcel
				ServiceStarter.startService(mCtx, null);
				
			} else if (!newValue && numAutoParcels < 1) { // Stop the service if database is "empty"
				// We can bypass the preference lookup since we want to stop the service here
				ServiceStarter.startService(mCtx, null, 0);
			}
		}
		
		return updated;
	}

	public Cursor fetchEvents(long parcelId) {
		return mDb.query(EVENT_TABLE,
				new String[] {
					KEY_ROWID,
					KEY_LOC,
					KEY_DESC,
					"("+KEY_TIME+" || ': ' || "+KEY_LOC+") AS "+KEY_CUSTOM,
					KEY_ERREV
				}, KEY_FOREIGN + "=" + parcelId, null, null, null, KEY_TIME+" DESC");
	}

	public boolean updateParcelData(long rowId, Parcel parcelData) {
		ContentValues args = new ContentValues();
		
		Calendar c = Calendar.getInstance();
		String now = String.format("%tF %tT", c, c);
		
		if (parcelData.getError() == Error.NONE) {
			if (parcelData.getParcel() != null) {
				args.put(KEY_PARCEL, parcelData.getParcel());
			}
			args.put(KEY_CUSTOMER, parcelData.getCustomer());
			args.put(KEY_SENT, parcelData.getSent());
			args.put(KEY_WEIGHT, parcelData.getWeight());
			args.put(KEY_POSTAL, parcelData.getPostal());
			args.put(KEY_SERVICE, parcelData.getService());
			args.put(KEY_STATUS, parcelData.getStatus());
			args.put(KEY_STATUSCODE, parcelData.getStatusCode());
			args.put(KEY_OK_UPDATE, now);
		}
		args.put(KEY_UPDATE, now);
		args.put(KEY_ERROR, parcelData.getError());

		return mDb.update(PARCEL_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean addEvent(long parcelId, ParcelEvent eventData) {
		// Check for existing event.
		// We have an index preventing insertion of such events, but relying on that only fills the logs with errors.
		Cursor existingCheck = mDb.query(EVENT_TABLE, new String[] {KEY_ROWID}, 
				KEY_FOREIGN+" = "+parcelId+" AND "+KEY_LOC+" = ? AND "+KEY_DESC+" = ? AND "+KEY_TIME+" = ? AND "+KEY_ERREV+" = ?",
				new String[] {eventData.getLocation(), eventData.getDescription(), eventData.getTime(), (eventData.isError() ? "1" : "0")},
				null, null, null, null);
		
		boolean existing = false;
		if (existingCheck != null) {
			existing = !existingCheck.isAfterLast();
			existingCheck.close();
		}
		
		if (!existing) {
			ContentValues args = new ContentValues();
			args.put(KEY_FOREIGN, parcelId);
			args.put(KEY_LOC, eventData.getLocation());
			args.put(KEY_DESC, eventData.getDescription());
			args.put(KEY_TIME, eventData.getTime());
			args.put(KEY_ERREV, (eventData.isError() ? 1 : 0));
	
			return mDb.insert(EVENT_TABLE, null, args) > 0;
		} else {
			return false;
		}
	}
}

