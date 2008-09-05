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
    private static final String DATABASE_TABLE = "passwords";
    private static final String DATABASE_VERIFY = "verify_crypto"; 
    private static String TAG = "DBHelper";
    Context myCtx;

    private static final String DATABASE_CREATE =
        "create table " + DATABASE_TABLE + " ("
    	    + "id integer primary key autoincrement, "
            + "password text not null, "
            + "description text not null,"
            + "username text,"
            + "website text,"
            + "note text);";
    
    private static final String VERIFY_CREATE = 
    	"create table " + DATABASE_VERIFY + " ("
    		+ "confirm text not null);";

    private SQLiteDatabase db;

    /**
     * 
     * @param ctx
     */
    public DBHelper(Context ctx) {
    	myCtx = ctx;
		try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);
			Cursor c =
				db.query("sqlite_master", new String[] { "name" },
						"type='table' and name='"+DATABASE_TABLE+"'", null, null, null, null);
			int numRows = c.getCount();
			if (numRows < 1) {
				db.execSQL(DATABASE_CREATE);
			}
			c.close();
		
			c = db.query("sqlite_master", new String[] { "name" },
				"type='table' and name='"+DATABASE_VERIFY+"'", null, null, null, null);
			numRows = c.getCount();
			if (numRows < 1) {
				db.execSQL(VERIFY_CREATE);
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
     * @param entry
     */
    public void addPassword(PassEntry entry) {
        ContentValues initialValues = new ContentValues();
    	initialValues.put("password", entry.password);
        initialValues.put("description", entry.description);
        initialValues.put("username",entry.username);
        initialValues.put("website", entry.website);
        initialValues.put("note", entry.note);

        try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);
	        db.insert(DATABASE_TABLE, null, initialValues);
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
			db.delete(DATABASE_TABLE, "id=" + Id, null);
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
    public List<PassEntry> fetchAllRows(){
        ArrayList<PassEntry> ret = new ArrayList<PassEntry>();
        try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);
	        Cursor c =
	            db.query(DATABASE_TABLE, new String[] {
	                "id", "password", "description", "username", "website", "note"},
	                null, null, null, null, null);
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
	            db.query(true, DATABASE_TABLE, new String[] {
	                "id", "password", "description", "username", "website",
	                "note"}, "id=" + Id, null, null, null, null, null);
	        if (c.getCount() > 0) {
	            c.moveToFirst();
	            row.id = c.getLong(0);
	
	            row.password = c.getString(1);
	            row.description = c.getString(2);
	            row.username = c.getString(3);
	            row.website = c.getString(4);
	            row.note = c.getString(5);
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
     * @return
     */
    public String fetchPBEKey() {
    	String key="";
        try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);
			Cursor c = db.query(true, DATABASE_VERIFY, new String[] {"confirm"},
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
			db.delete(DATABASE_VERIFY, "1=1", null);
			args.put("confirm", PBEKey);
			db.insert(DATABASE_VERIFY, null, args);
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
			db.update(DATABASE_TABLE, args, "id=" + Id, null);
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} finally 
		{
			db.close();
		}
    }
}


