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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
    private static final int DATABASE_VERSION = 1;

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
        try {
            db = ctx.openDatabase(DATABASE_NAME, null);
        } catch (FileNotFoundException e) {
            try {
                db =
                    ctx.createDatabase(DATABASE_NAME, DATABASE_VERSION, 0,
                        null);
                db.execSQL(DATABASE_CREATE);
                db.execSQL(VERIFY_CREATE);
            } catch (FileNotFoundException e1) {
                db = null;
            }
        }
    }

    /**
     * Close database connection
     */
    public void close() {
        db.close();
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
	        db.insert(DATABASE_TABLE, null, initialValues);
    }

    /**
     * 
     * @param Id
     */
    public void deletePassword(long Id) {
        db.delete(DATABASE_TABLE, "id=" + Id, null);
    }

    /**
     * 
     * @return
     */
    public List<PassEntry> fetchAllRows(){
        ArrayList<PassEntry> ret = new ArrayList<PassEntry>();
        Cursor c =
            db.query(DATABASE_TABLE, new String[] {
                "id", "password", "description", "username", "website", "note"},
                null, null, null, null, null);
        int numRows = c.count();
        c.first();
        for (int i = 0; i < numRows; ++i) {
            PassEntry row = new PassEntry();
            row.id = c.getLong(0);
            
            row.password = c.getString(1);
            row.description = c.getString(2);
            row.username = c.getString(3);
            row.website = c.getString(4);
            row.note = c.getString(5);
            
            ret.add(row);
            c.next();
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
        Cursor c =
            db.query(true, DATABASE_TABLE, new String[] {
                "id", "password", "description", "username", "website",
                "note"}, "id=" + Id, null, null, null, null);
        if (c.count() > 0) {
            c.first();
            row.id = c.getLong(0);

            row.password = c.getString(1);
            row.description = c.getString(2);
            row.username = c.getString(3);
            row.website = c.getString(4);
            row.note = c.getString(5);
            
            return row;
        } else {
            row.id = -1;
            row.description = row.password = null;
        }
        return row;
    }

    /**
     * 
     * @return
     */
    public String fetchPBEKey() {
	Cursor c = db.query(true, DATABASE_VERIFY, new String[] {"confirm"},
		null, null, null, null, null);
	if(c.count() > 0) {
	    c.first();
	    return c.getString(0);
	} 
	return "";
    }
    
    /**
     * 
     * @param PBEKey
     */
    public void storePBEKey(String PBEKey) {
	 ContentValues args = new ContentValues();
	 db.delete(DATABASE_VERIFY, "1=1", null);
	 args.put("confirm", PBEKey);
         db.insert(DATABASE_VERIFY, null, args);
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
        
        db.update(DATABASE_TABLE, args, "id=" + Id, null);
    }
}


