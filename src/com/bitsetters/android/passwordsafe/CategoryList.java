/* $Id$
 * 
 * Copyright 2008 Randy McEoin
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

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import android.widget.Toast;

/**
 * CategoryList Activity
 * 
 * @author Randy McEoin
 */
public class CategoryList extends ListActivity {

    private static final String TAG = "CategoryList";

    // Menu Item order
    public static final int EDIT_CATEGORY_INDEX = Menu.FIRST;
    public static final int ADD_CATEGORY_INDEX = Menu.FIRST + 1;
    public static final int DEL_CATEGORY_INDEX = Menu.FIRST + 2;
    public static final int EXPORT_INDEX = Menu.FIRST + 3;

    public static final int REQUEST_ONCREATE = 0;
    public static final int REQUEST_EDIT_CATEGORY = 1;
    public static final int REQUEST_ADD_CATEGORY = 2;
    
    private static final String EXPORT_FILENAME = "/sdcard/passwordsafe.csv";
    
    public static final String KEY_ID = "id";  // Intent keys

    private CryptoHelper ch;
    private DBHelper dbHelper=null;
	private boolean needPrePopulateCategories=false;
	
    private static boolean signedIn = false;  // Has user logged in
    private static String PBEKey;	      // Password Based Encryption Key			

    private List<CategoryEntry> rows;
    
    /** 
     * Called when the activity is first created. 
     */
    @Override
    public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
//		setContentView(R.layout.cat_list);
		if (dbHelper==null) {
			dbHelper = new DBHelper(this);
			if (dbHelper.getPrePopulate()==true)
			{
				needPrePopulateCategories=true;
			}
		}
		if(!signedIn) {
		    Intent i = new Intent(this, FrontDoor.class);
		    startActivityForResult(i,REQUEST_ONCREATE);
		} 
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
		
//		dbHelper.close();
    }
    /**
     * Populates the category ListView
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
		rows = dbHelper.fetchAllCategoryRows();

		for (CategoryEntry row : rows) {
		    String cryptDesc = row.name;
		    row.plainName = "";
		    try {
				row.plainName = ch.decrypt(cryptDesc);
		    } catch (CryptoHelperException e) {
				Log.e(TAG,e.toString());
		    }
		}
		Collections.sort(rows, new Comparator<CategoryEntry>() {
		    public int compare(CategoryEntry o1, CategoryEntry o2) {
		        return o1.plainName.compareToIgnoreCase(o2.plainName);
		    }});
		for (CategoryEntry row : rows) {
			items.add(row.plainName);
		}

		ArrayAdapter<String> entries = 
		    new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
		setListAdapter(entries);
		
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
	
		menu.add(0,EDIT_CATEGORY_INDEX, 0, R.string.password_edit);
		//	menu.addSeparator(EDIT_CATEGORY_INDEX, 0);
		
		menu.add(0,ADD_CATEGORY_INDEX, 0, R.string.password_insert);
		//.setShortcut(arg0, arg1, arg2);
		menu.add(0, DEL_CATEGORY_INDEX, 0, R.string.password_delete);  
		menu.add(0, EXPORT_INDEX, 0, R.string.export_database);  
	
		return super.onCreateOptionsMenu(menu);
    }

    static void setPBEKey(String key) {
		PBEKey = key;
    }

    static String getPBEKey() {
		return PBEKey;
    }

    private void addCategory() {
		Intent i = new Intent(this, CategoryEdit.class);
		startActivityForResult(i,REQUEST_ADD_CATEGORY);
    }

    private void delCategory(long Id) {
    	if (dbHelper.countPasswords(Id)>0) {
            Toast.makeText(CategoryList.this, R.string.category_not_empty,
                    Toast.LENGTH_SHORT).show();
    		return;
    	}
		dbHelper.deleteCategory(Id);
		fillData();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case EDIT_CATEGORY_INDEX:
			Intent i = new Intent(this, CategoryEdit.class);
			i.putExtra(KEY_ID, rows.get(getSelectedItemPosition()).id);
		    startActivityForResult(i,REQUEST_EDIT_CATEGORY);
		    break;
		case ADD_CATEGORY_INDEX:
		    addCategory();
		    break;
		case DEL_CATEGORY_INDEX:
		    try {
		    	delCategory(rows.get(getSelectedItemPosition()).id);
		    } catch (IndexOutOfBoundsException e) {
				// This should only happen when there are no
				// entries to delete.
				Log.w(TAG,e.toString());
		    }
		    break;
		case EXPORT_INDEX:
			exportDatabase();
			break;
		}
		return super.onOptionsItemSelected(item);
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
	
		Intent i = new Intent(this, PassList.class);
		i.putExtra(KEY_ID, rows.get(position).id);
	    startActivityForResult(i,1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent i) {
    	super.onActivityResult(requestCode, resultCode, i);

    	if (dbHelper == null) {
		    dbHelper = new DBHelper(this);
		}

    	if ((requestCode==REQUEST_ONCREATE) && (needPrePopulateCategories==true))
    	{
    		needPrePopulateCategories=false;
    		addCategory(getString(R.string.category_business));
    		addCategory(getString(R.string.category_personal));
    	}
    	fillData();
    }

    private void addCategory(String name) {
		CategoryEntry entry =  new CategoryEntry();
	
		String namePlain = name;

		try {
			ch = new CryptoHelper();
			if(PBEKey == null) {
			    PBEKey = "";
			}
			ch.setPassword(PBEKey);

		    entry.name = ch.encrypt(namePlain);
		} catch(CryptoHelperException e) {
		    Log.e(TAG,e.toString());
		}
	    dbHelper.addCategory(entry);
    }
    
	public boolean exportDatabase(){
		try {
			FileWriter f = new FileWriter(EXPORT_FILENAME);

			f.write( CSVify(getString(R.string.category)) + "," +
					CSVify(getString(R.string.description)) + "," + 
					CSVify(getString(R.string.website)) + "," +
					CSVify(getString(R.string.username)) + "," +
					CSVify(getString(R.string.password)) + "," +
					CSVify(getString(R.string.notes))
		    		);
		    f.write("\n");

			
			ch = new CryptoHelper();
			if(PBEKey == null) {
			    PBEKey = "";
			}
			ch.setPassword(PBEKey);
		
			HashMap<Long, String> categories = new HashMap<Long, String>();
			
			List<CategoryEntry> crows;
			crows = dbHelper.fetchAllCategoryRows();
		
			for (CategoryEntry row : crows) {
			    String cryptDesc = row.name;
			    row.plainName = "";
			    try {
					row.plainName = ch.decrypt(cryptDesc);
					categories.put(row.id, row.plainName);
			    } catch (CryptoHelperException e) {
					Log.e(TAG,e.toString());
		            Toast.makeText(CategoryList.this, R.string.cannot_decrypt_category,
		                    Toast.LENGTH_SHORT).show();
		            return false;
			    }
			}
		
			List<PassEntry> rows;
			rows = dbHelper.fetchAllRows(new Long(0));
		
			for (PassEntry row : rows) {
			    String cryptDesc = row.description;
			    String cryptWebsite = row.website;
			    String cryptUsername = row.username;
			    String cryptPassword = row.password;
			    String cryptNote = row.note;
			    row.plainDescription = "";
			    row.plainWebsite = "";
			    row.plainUsername = "";
			    row.plainPassword = "";
			    row.plainNote = "";
			    try {
					row.plainDescription = ch.decrypt(cryptDesc);
					row.plainWebsite     = ch.decrypt(cryptWebsite);
					row.plainUsername    = ch.decrypt(cryptUsername);
					row.plainPassword    = ch.decrypt(cryptPassword);
					row.plainNote        = ch.decrypt(cryptNote);
			    } catch (CryptoHelperException e) {
					Log.e(TAG,e.toString());
		            Toast.makeText(CategoryList.this, R.string.cannot_decrypt_password,
		                    Toast.LENGTH_SHORT).show();
		            return false;
			    }
			    f.write(CSVify(categories.get(row.category)) + "," +
			    		CSVify(row.plainDescription) + "," + 
			    		CSVify(row.plainWebsite) + "," +
			    		CSVify(row.plainUsername) + "," +
			    		CSVify(row.plainPassword) + "," +
			    		CSVify(row.plainNote)
			    		);
			    f.write("\n");
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
	        Toast.makeText(CategoryList.this, R.string.export_file_error,
	                Toast.LENGTH_SHORT).show();
			return false;
		}
        Toast.makeText(CategoryList.this, R.string.export_success,
                Toast.LENGTH_LONG).show();
		return true;
	}

	public String CSVify(String in)
	{
		if (in == null)
			return "";
		
		String out=in;
		
		if (in.contains(",") || in.contains("\""))
		{
			in.replaceAll("\"", "\"\"");
			out = "\"" + in + "\"";
		}
		return out;
	}

}