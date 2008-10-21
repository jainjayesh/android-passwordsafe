/* $Id$
 * 
 * Copyright 2007 Steven Osborn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bitsetters.android.passwordsafe;

import java.util.ArrayList;
import java.util.List;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * DBHelper class.  
 * 
 * The overall theme of this class was borrowed from the Notepad
 * example Open Handset Alliance website.  It's essentially a very
 * primitive database layer.
 * 
 * @author Steven Osborn - http://steven.bitsetters.com
 */
public class DBHelper {

    private static final String DATABASE_NAME = "passwordsafe";
    private static final String TABLE_DBVERSION = "dbversion";
    private static final String TABLE_PASSWORDS = "passwords";
    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_VERIFY = "verify_crypto";
    private static final int DATABASE_VERSION = 2;
    private static String TAG = "DBHelper";
    Context myCtx;

    private static final String DBVERSION_CREATE = 
    	"create table " + TABLE_DBVERSION + " ("
    		+ "version integer not null);";

    private static final String PASSWORDS_CREATE =
        "create table " + TABLE_PASSWORDS + " ("
    	    + "id integer primary key autoincrement, "
    	    + "category integer not null, "
            + "password text not null, "
            + "description text not null, "
            + "username text, "
            + "website text, "
            + "note text, "
            + "lastdatetimeedit text);";

    private static final String PASSWORDS_DROP =
    	"drop table " + TABLE_PASSWORDS + ";";

    private static final String CATEGORIES_CREATE =
        "create table " + TABLE_CATEGORIES + " ("
    	    + "id integer primary key autoincrement, "
            + "name text not null, "
            + "lastdatetimeedit text);";

    private static final String CATEGORIES_DROP =
    	"drop table " + TABLE_CATEGORIES + ";";

    private static final String VERIFY_CREATE = 
    	"create table " + TABLE_VERIFY + " ("
    		+ "confirm text not null);";

    private SQLiteDatabase db;
    private boolean needsPrePopulation=false;

    /**
     * 
     * @param ctx
     */
    public DBHelper(Context ctx) {
    	myCtx = ctx;
		try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);

			// Check for the existence of the DBVERSION table
			// If it doesn't exist than create the overall data,
			// otherwise double check the version
			Cursor c =
				db.query("sqlite_master", new String[] { "name" },
						"type='table' and name='"+TABLE_DBVERSION+"'", null, null, null, null);
			int numRows = c.getCount();
			if (numRows < 1) {
				CreateDatabase(db);
			} else {
				int version=0;
				Cursor vc = db.query(true, TABLE_DBVERSION, new String[] {"version"},
						null, null, null, null, null,null);
				if(vc.getCount() > 0) {
				    vc.moveToFirst();
				    version=vc.getInt(0);
				}
				vc.close();
				if (version!=DATABASE_VERSION) {
					Log.e(TAG,"database version mismatch");
				}
			}
			c.close();
			

		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} finally 
		{
			db.close();
		}
    }

    private void CreateDatabase(SQLiteDatabase db)
    {
		try {
			db.execSQL(DBVERSION_CREATE);
			ContentValues args = new ContentValues();
			args.put("version", DATABASE_VERSION);
			db.insert(TABLE_DBVERSION, null, args);

			db.execSQL(CATEGORIES_CREATE);
			needsPrePopulation=true;
			
			db.execSQL(PASSWORDS_CREATE);
			db.execSQL(VERIFY_CREATE);
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} 
    }
    
    public void deleteDatabase()
    {
        try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);
			db.execSQL(PASSWORDS_DROP);
			db.execSQL(PASSWORDS_CREATE);

			db.execSQL(CATEGORIES_DROP);
			db.execSQL(CATEGORIES_CREATE);
        } catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} finally 
		{
			db.close();
		}    	
    }
    
    public boolean getPrePopulate()
    {
    	return needsPrePopulation;
    }
    /**
     * Close database connection
     */
    public void close() {
    /*
    	try {
    		db.close();
	    } catch (SQLException e)
	    {
	    	Log.d(TAG,"close exception: " + e.getLocalizedMessage());
	    }
	    */
    }

    /**
     * 
     * @return
     */
    public String fetchPBEKey() {
    	String key="";
        try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);
			Cursor c = db.query(true, TABLE_VERIFY, new String[] {"confirm"},
				null, null, null, null, null,null);
			if(c.getCount() > 0) {
			    c.moveToFirst();
			    key=c.getString(0);
			}
			c.close();
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} finally 
		{
			db.close();
		}
		return key;
    }
    
    /**
     * 
     * @param PBEKey
     */
    public void storePBEKey(String PBEKey) {
		ContentValues args = new ContentValues();
        try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);
			db.delete(TABLE_VERIFY, "1=1", null);
			args.put("confirm", PBEKey);
			db.insert(TABLE_VERIFY, null, args);
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} finally 
		{
			db.close();
		}
    }

//////////Category Functions ////////////////

    /**
     * 
     * @param entry
     */
    public void addCategory(CategoryEntry entry) {
        ContentValues initialValues = new ContentValues();
    	initialValues.put("name", entry.name);

        try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);
	        db.insert(TABLE_CATEGORIES, null, initialValues);
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} finally 
		{
			db.close();
		}
    }

    /**
     * 
     * @param Id
     */
    public void deleteCategory(long Id) {
        try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);
			db.delete(TABLE_CATEGORIES, "id=" + Id, null);
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} finally 
		{
			db.close();
		}
    }

    /**
     * 
     * @return
     */
    public List<CategoryEntry> fetchAllCategoryRows(){
        ArrayList<CategoryEntry> ret = new ArrayList<CategoryEntry>();
        try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);
	        Cursor c =
	            db.query(TABLE_CATEGORIES, new String[] {
	                "id", "name"},
	                null, null, null, null, null);
	        int numRows = c.getCount();
	        c.moveToFirst();
	        for (int i = 0; i < numRows; ++i) {
	            CategoryEntry row = new CategoryEntry();
	            row.id = c.getLong(0);
	            row.name = c.getString(1);
	            ret.add(row);
	            c.moveToNext();
	        }
	        c.close();
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} finally 
		{
			db.close();
		}
        return ret;
    }

    /**
     * 
     * @param Id
     * @return
     */
    public CategoryEntry fetchCategory(long Id) {
        CategoryEntry row = new CategoryEntry();
        try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);
	        Cursor c =
	            db.query(true, TABLE_CATEGORIES, new String[] {
	                "id", "name"}, "id=" + Id, null, null, null, null, null);
	        if (c.getCount() > 0) {
	            c.moveToFirst();
	            row.id = c.getLong(0);
	
	            row.name = c.getString(1);
	        } else {
	            row.id = -1;
	            row.name = null;
	        }
	        c.close();
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} finally 
		{
			db.close();
		}
        return row;
    }

    /**
     * 
     * @param Id
     * @param entry
     */
    public void updateCategory(long Id, CategoryEntry entry) {
        ContentValues args = new ContentValues();
        args.put("name", entry.name);
        
        try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);
			db.update(TABLE_CATEGORIES, args, "id=" + Id, null);
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} finally 
		{
			db.close();
		}
    }


////////// Password Functions ////////////////
	
	
	/**
	 * 
	 * @param entry
	 */
	public void addPassword(PassEntry entry) {
	    ContentValues initialValues = new ContentValues();
		initialValues.put("category", entry.category);
		initialValues.put("password", entry.password);
	    initialValues.put("description", entry.description);
	    initialValues.put("username",entry.username);
	    initialValues.put("website", entry.website);
	    initialValues.put("note", entry.note);
	
	    try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);
	        db.insert(TABLE_PASSWORDS, null, initialValues);
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} finally 
		{
			db.close();
		}
	}
	
	/**
	 * 
	 * @param Id
	 */
	public void deletePassword(long Id) {
	    try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);
			db.delete(TABLE_PASSWORDS, "id=" + Id, null);
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} finally 
		{
			db.close();
		}
	}

	/**
	 * 
	 * @param categoryId
	 */
	public int countPasswords(long categoryId) {
		int count=0;
	    try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);
	        Cursor c = db.query(TABLE_PASSWORDS, new String[] {
	                "count(*)"},
	                "category="+categoryId, null, null, null, null);
	        c.moveToFirst();
	        count=c.getInt(0);
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} finally 
		{
			db.close();
		}
		Log.i(TAG,"count="+count);
		return count;
	}

	/**
	 * 
	 * @return
	 */
	public List<PassEntry> fetchAllRows(Long CategoryId){
	    ArrayList<PassEntry> ret = new ArrayList<PassEntry>();
	    try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);
	        Cursor c;
	        if (CategoryId==0)
	        {
		        c = db.query(TABLE_PASSWORDS, new String[] {
	                "id", "password", "description", "username", "website", "note", "category"},
	                null, null, null, null, null);
	        } else {
		        c = db.query(TABLE_PASSWORDS, new String[] {
		                "id", "password", "description", "username", "website", "note", "category"},
		                "category="+CategoryId, null, null, null, null);
	        }
	        int numRows = c.getCount();
	        c.moveToFirst();
	        for (int i = 0; i < numRows; ++i) {
	            PassEntry row = new PassEntry();
	            row.id = c.getLong(0);
	            
	            row.password = c.getString(1);
	            row.description = c.getString(2);
	            row.username = c.getString(3);
	            row.website = c.getString(4);
	            row.note = c.getString(5);
	            
	            row.category = c.getLong(6);
	            
	            ret.add(row);
	            c.moveToNext();
	        }
	        c.close();
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} finally 
		{
			db.close();
		}
	    return ret;
	}
	
	/**
	 * 
	 * @param Id
	 * @return
	 */
	public PassEntry fetchPassword(long Id) {
	    PassEntry row = new PassEntry();
	    try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);
	        Cursor c =
	            db.query(true, TABLE_PASSWORDS, new String[] {
	                "id", "password", "description", "username", "website",
	                "note", "category"}, "id=" + Id, null, null, null, null, null);
	        if (c.getCount() > 0) {
	            c.moveToFirst();
	            row.id = c.getLong(0);
	
	            row.password = c.getString(1);
	            row.description = c.getString(2);
	            row.username = c.getString(3);
	            row.website = c.getString(4);
	            row.note = c.getString(5);
	            
	            row.category = c.getLong(6);
	        } else {
	            row.id = -1;
	            row.description = row.password = null;
	        }
	        c.close();
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} finally 
		{
			db.close();
		}
	    return row;
	}
	
	/**
	 * 
	 * @param Id
	 * @param entry
	 */
	public void updatePassword(long Id, PassEntry entry) {
	    ContentValues args = new ContentValues();
	    args.put("description", entry.description);
	    args.put("username", entry.username);
	    args.put("password", entry.password);
	    args.put("website", entry.website);
	    args.put("note", entry.note);
	    
	    try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);
			db.update(TABLE_PASSWORDS, args, "id=" + Id, null);
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} finally 
		{
			db.close();
		}
	}

}

