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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * PassList Activity
 * 
 * This is the main activity for PasswordSafe all other activities are 
 * spawned as sub-activities of this one.  The basic application 
 * skeleton was based on google's notepad example.
 * 
 * @author Steven Osborn - http://steven.bitsetters.com
 */
public class PassList extends ListActivity {

    private static final String TAG = "PassList";
//    private static final int ACTIVITY_CREATE=0;
//    private static final int ACTIVITY_EDIT=1;

    // Menu Item order
    public static final int EDIT_PASSWORD_INDEX = Menu.FIRST;
    public static final int ADD_PASSWORD_INDEX = Menu.FIRST + 1;
    public static final int DEL_PASSWORD_INDEX = Menu.FIRST + 2;   

    public static final String KEY_ID = "id";  // Intent keys

    private CryptoHelper ch;
    private DBHelper dbHelper=null;

    private static boolean signedIn = false;  // Has user logged in
    private static String PBEKey;	      // Password Based Encryption Key			

    private List<PassEntry> rows;
    
    /** 
     * Called when the activity is first created. 
     */
    @Override
    public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		Log.i(TAG,"onCreate()");
		setContentView(R.layout.pass_list);
		if (dbHelper==null) {
			dbHelper = new DBHelper(this);
		}
		if(!signedIn) {
		    Intent i = new Intent(this, FrontDoor.class);
		    startActivityForResult(i,0);
		} 
		fillData();
    }
    
    @Override
    protected void onPause() {
		super.onPause();
		dbHelper.close();
		dbHelper = null;
    }

    @Override
    protected void onResume() {
		super.onResume();
		if (dbHelper == null) {
		    dbHelper = new DBHelper(this);
		}
    }
    
    @Override
    public void onStop() {
		super.onStop();
		
		Log.i(TAG,"onStop()");
//		dbHelper.close();
    }
    /**
     * Populates the password ListView
     */
    private void fillData() {
		// initialize crypto so that we can display readable descriptions in
		// the list view
		ch = new CryptoHelper();
		if(PBEKey == null) {
		    PBEKey = "";
		}
		ch.setPassword(PBEKey);
	
		List<String> items = new ArrayList<String>();
		rows = dbHelper.fetchAllRows();

		for (PassEntry row : rows) {
		    String cryptDesc = row.description;
		    row.plainDescription = "";
		    try {
				row.plainDescription = ch.decrypt(cryptDesc);
		    } catch (CryptoHelperException e) {
				Log.e(TAG,e.toString());
		    }
		}
		Collections.sort(rows, new Comparator<PassEntry>() {
		    public int compare(PassEntry o1, PassEntry o2) {
		        return o1.plainDescription.compareToIgnoreCase(o2.plainDescription);
		    }});
		for (PassEntry row : rows) {
			items.add(row.plainDescription);
		}

		ArrayAdapter<String> entries = 
		    new ArrayAdapter<String>(this, R.layout.pass_row, items);
		setListAdapter(entries);
		
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
	
		//	menu.add(0,EDIT_PASSWORD_INDEX, R.string.password_edit);
		//	menu.addSeparator(EDIT_PASSWORD_INDEX, 0);
		
		menu.add(0,ADD_PASSWORD_INDEX, 0, R.string.password_insert);
		//.setShortcut(arg0, arg1, arg2);
		menu.add(0, DEL_PASSWORD_INDEX, 0, R.string.password_delete);  
	
		return super.onCreateOptionsMenu(menu);
    }

    static void setPBEKey(String key) {
		PBEKey = key;
    }

    static String getPBEKey() {
		return PBEKey;
    }

    private void addPassword() {
		Intent i = new Intent(this, PassEdit.class);
		Log.i(TAG,"before startActivity() [PassEdit]");
		startActivity(i);
		Log.i(TAG,"after startActivity() [PassEdit]");
    }

    private void delPassword(long Id) {
		dbHelper.deletePassword(Id);
		fillData();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case ADD_PASSWORD_INDEX:
		    addPassword();
		    break;
		case DEL_PASSWORD_INDEX:
		    try {
		    	//TODO: need to fix
		    	/*
			delPassword(rows.get(getSelection()).id);
			*/
		    } catch (IndexOutOfBoundsException e) {
				// This should only happen when there are no
				// entries to delete.
				Log.w(TAG,e.toString());
		    }
		    break;
		}
		return super.onOptionsItemSelected(item);
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
	
		Intent i = new Intent(this, PassEdit.class);
		i.putExtra(KEY_ID, rows.get(position).id);
	    startActivityForResult(i,1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent i) {
    	super.onActivityResult(requestCode, resultCode, i);

    	if (dbHelper == null) {
		    dbHelper = new DBHelper(this);
		}

    	fillData();
    }


}