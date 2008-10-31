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

    // Menu Item order
    public static final int EDIT_PASSWORD_INDEX = Menu.FIRST;
    public static final int ADD_PASSWORD_INDEX = Menu.FIRST + 1;
    public static final int DEL_PASSWORD_INDEX = Menu.FIRST + 2;   

    public static final int REQUEST_EDIT_PASSWORD = 1;
    public static final int REQUEST_ADD_PASSWORD = 2;

    public static final String KEY_ID = "id";  // Intent keys
    public static final String KEY_CATEGORY_ID = "categoryId";  // Intent keys

    private CryptoHelper ch;
    private DBHelper dbHelper=null;
    private static Long CategoryId=null;

    private static String PBEKey;	      // Password Based Encryption Key			

    private List<PassEntry> rows;
    
    /** 
     * Called when the activity is first created. 
     */
    @Override
    public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
//		Log.d(TAG,"onCreate()");
		setContentView(R.layout.pass_list);
		
		String title = getResources().getString(R.string.app_name) + " - " +
		getResources().getString(R.string.passwords);
		setTitle(title);

		
		if (dbHelper==null) {
			dbHelper = new DBHelper(this);
		}
		CategoryId = icicle != null ? icicle.getLong(CategoryList.KEY_ID) : null;
		if (CategoryId == null) {
		    Bundle extras = getIntent().getExtras();            
		    CategoryId = extras != null ? extras.getLong(CategoryList.KEY_ID) : null;
		}
		if (CategoryId<1) {
			finish();	// no valid category less than one
		}
		fillData();
    }
    
    @Override
    protected void onPause() {
		super.onPause();
		
//		Log.d(TAG,"onPause()");
		if (dbHelper != null) {
			dbHelper.close();
			dbHelper = null;
		}
    }

    @Override
    protected void onResume() {
		super.onResume();
		
//		Log.d(TAG,"onResume()");

		if (CategoryList.isSignedIn()==false) {
			finish();
		}
		if (dbHelper == null) {
		    dbHelper = new DBHelper(this);
		}
    }
    
    @Override
    public void onStop() {
		super.onStop();
		
//		Log.d(TAG,"onStop()");
		if (dbHelper != null) {
			dbHelper.close();
			dbHelper=null;
		}
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
		rows = dbHelper.fetchAllRows(CategoryId);

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
//		    new ArrayAdapter<String>(this, R.layout.pass_row, R.id.entry_desc, items);
		    new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
		setListAdapter(entries);
		
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0,ADD_PASSWORD_INDEX, 0, R.string.password_add)
			.setIcon(android.R.drawable.ic_menu_add)
			.setShortcut('2', 'a');
		menu.add(0, DEL_PASSWORD_INDEX, 0, R.string.password_delete)
			.setIcon(android.R.drawable.ic_menu_delete)
			.setShortcut('3', 'd');
	
		return super.onCreateOptionsMenu(menu);
    }

    static void setPBEKey(String key) {
		PBEKey = key;
    }

    static String getPBEKey() {
		return PBEKey;
    }

    static long getCategoryId() {
    	return CategoryId;
    }

    private void addPassword() {
		Intent i = new Intent(this, PassEdit.class);
	    startActivityForResult(i,REQUEST_ADD_PASSWORD);
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
		    	delPassword(rows.get(getSelectedItemPosition()).id);
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
		i.putExtra(KEY_CATEGORY_ID, CategoryId);
	    startActivityForResult(i,REQUEST_EDIT_PASSWORD);
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
