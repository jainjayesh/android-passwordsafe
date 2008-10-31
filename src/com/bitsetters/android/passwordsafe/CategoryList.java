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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
 * @author Steven Osborn - http://steven.bitsetters.com
 */
public class CategoryList extends ListActivity {

    private static final String TAG = "CategoryList";

    // Menu Item order
    public static final int LOCK_CATEGORY_INDEX = Menu.FIRST;
    public static final int EDIT_CATEGORY_INDEX = Menu.FIRST + 1;
    public static final int ADD_CATEGORY_INDEX = Menu.FIRST + 2;
    public static final int DEL_CATEGORY_INDEX = Menu.FIRST + 3;
    public static final int HELP_INDEX = Menu.FIRST + 4;
    public static final int EXPORT_INDEX = Menu.FIRST + 5;
    public static final int IMPORT_INDEX = Menu.FIRST + 6;
    
    public static final int REQUEST_ONCREATE = 0;
    public static final int REQUEST_EDIT_CATEGORY = 1;
    public static final int REQUEST_ADD_CATEGORY = 2;

    public static final int MAX_CATEGORIES = 256;

    private static final String EXPORT_FILENAME = "/sdcard/passwordsafe.csv";
    
    public static final String KEY_ID = "id";  // Intent keys

    private CryptoHelper ch=null;
    private DBHelper dbHelper=null;
	private boolean needPrePopulateCategories=false;
	
    private static String PBEKey;	      // Password Based Encryption Key			

    private List<CategoryEntry> rows;
    
    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Log.d(TAG,"caught ACTION_SCREEN_OFF");
                PBEKey=null;
            }
        }
    };

    /** 
     * Called when the activity is first created. 
     */
    @Override
    public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		setContentView(R.layout.cat_list);
		String title = getResources().getString(R.string.app_name) + " - " +
			getResources().getString(R.string.categories);
		setTitle(title);
		
		Log.d(TAG,"onCreate()");
		if (dbHelper==null) {
			dbHelper = new DBHelper(this);
			if (dbHelper.getPrePopulate()==true)
			{
				needPrePopulateCategories=true;
			}
		}
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mIntentReceiver, filter);
    }

    @Override
    protected void onResume() {
		super.onResume();

		Log.d(TAG,"onResume()");
		if (dbHelper == null) {
		    dbHelper = new DBHelper(this);
		}

		fillData();
    }
    
    @Override
    protected void onPause() {
		super.onPause();
		
		Log.d(TAG,"onPause()");
		dbHelper.close();
		dbHelper = null;
    }

    @Override
    public void onStop() {
		super.onStop();

		Log.d(TAG,"onStop()");
//		dbHelper.close();
    }
    
    @Override
    public void onDestroy() {
		super.onDestroy();

		Log.d(TAG,"onDestroy()");
    }

    /**
     * Returns the current status of signedIn. 
     * 
     * @return	True if signed in
     */
    public static boolean isSignedIn() {
    	if (PBEKey != null) {
    		return true;
    	}
    	return false;
    }
    /**
     * Sets signedIn status to false.
     * 
     * @see com.bitsetters.android.passwordsafe.CategoryList#isSignedIn
     */
    public static void setSignedOut() {
    	Log.d(TAG,"setSignedOut()");
    	PBEKey=null;
    }
    /**
     * Populates the category ListView
     */
    private void fillData() {
    	Log.d(TAG,"fillData()");
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
	
		menu.add(0,LOCK_CATEGORY_INDEX, 0, R.string.password_lock)
			.setIcon(android.R.drawable.ic_lock_lock)
			.setShortcut('0', 'l');
		menu.add(0,EDIT_CATEGORY_INDEX, 0, R.string.password_edit)
			.setIcon(android.R.drawable.ic_menu_edit)
			.setShortcut('1', 'e');
		menu.add(0,ADD_CATEGORY_INDEX, 0, R.string.password_add)
			.setIcon(android.R.drawable.ic_menu_add)
			.setShortcut('2', 'a');
		menu.add(0, DEL_CATEGORY_INDEX, 0, R.string.password_delete)  
			.setIcon(android.R.drawable.ic_menu_delete)
			.setShortcut('3', 'd');

		menu.add(0, HELP_INDEX, 0, R.string.help)
			.setIcon(android.R.drawable.ic_menu_help);

		menu.add(0, EXPORT_INDEX, 0, R.string.export_database)
			.setIcon(android.R.drawable.ic_menu_upload);
		menu.add(0, IMPORT_INDEX, 0, R.string.import_database)
			.setIcon(android.R.drawable.ic_input_get);

	
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
		case LOCK_CATEGORY_INDEX:
			PBEKey=null;
		    Intent frontdoor = new Intent(this, FrontDoor.class);
		    startActivityForResult(frontdoor,REQUEST_ONCREATE);
			break;
		case EDIT_CATEGORY_INDEX:
			Intent i = new Intent(this, CategoryEdit.class);
			int sel = getSelectedItemPosition();
			if (sel > -1) {
				i.putExtra(KEY_ID, rows.get(sel).id);
				startActivityForResult(i,REQUEST_EDIT_CATEGORY);
			}
		    break;
		case ADD_CATEGORY_INDEX:
		    addCategory();
		    break;
		case DEL_CATEGORY_INDEX:
		    try {
				int delPosition = getSelectedItemPosition();
				if (delPosition > -1) {
					delCategory(rows.get(delPosition).id);
				}
		    } catch (IndexOutOfBoundsException e) {
				// This should only happen when there are no
				// entries to delete.
				Log.w(TAG,e.toString());
		    }
		    break;
		case HELP_INDEX:
		    Intent help = new Intent(this, Help.class);
		    startActivity(help);
			break;
		case EXPORT_INDEX:
			exportDatabase();
			break;
		case IMPORT_INDEX:
			importDatabase();
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

    	if (requestCode==REQUEST_ONCREATE) {
    		if (needPrePopulateCategories==true)
	    	{
	    		needPrePopulateCategories=false;
	    		addCategory(getString(R.string.category_business));
	    		addCategory(getString(R.string.category_personal));
	    	}
    	}
    	
    	fillData();
    }

    private void addCategory(String name) {
    	Log.i(TAG,"addCategory("+name+")");
    	if ((name==null) || (name=="")) return;
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
			CSVWriter writer = new CSVWriter(new FileWriter(EXPORT_FILENAME), ',');

			String[] header = { getString(R.string.category),
					getString(R.string.description), 
					getString(R.string.website),
					getString(R.string.username),
					getString(R.string.password),
					getString(R.string.notes)
			};
			writer.writeNext(header);
			
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
			    String[] rowEntries = { categories.get(row.category),
			    		row.plainDescription,
			    		row.plainWebsite,
			    		row.plainUsername,
			    		row.plainPassword,
			    		row.plainNote
			    };
			    writer.writeNext(rowEntries);
			}
			writer.close();
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

	private void deleteDatabaseNow(){
		dbHelper.deleteDatabase();
	}

	public void deleteDatabase4Import(){
//		Log.i(TAG,"deleteDatabase4Import");
		Dialog about = new AlertDialog.Builder(this)
			.setIcon(R.drawable.passicon)
			.setTitle(R.string.dialog_delete_database_title)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					deleteDatabaseNow();
					importDatabaseStep2();
				}
			})
			.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			}) 
			.setMessage(R.string.dialog_delete_database_msg)
			.create();
		about.show();
	}
		
	public void importDatabase(){
		Dialog about = new AlertDialog.Builder(this)
			.setIcon(R.drawable.passicon)
			.setTitle(R.string.dialog_import_title)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					deleteDatabase4Import();
				}
			})
			.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					importDatabaseStep2();
				}
			}) 
			.setMessage(R.string.dialog_import_msg)
			.create();
		about.show();
	}
	
	private void importDatabaseStep2(){
		try {
			final int recordLength=6;
			CSVReader reader= new CSVReader(new FileReader(EXPORT_FILENAME));
		    String [] nextLine;
		    nextLine = reader.readNext();
		    if (nextLine==null) {
		        Toast.makeText(CategoryList.this, R.string.import_error_first_line,
		                Toast.LENGTH_SHORT).show();
		        return;
		    }
		    if (nextLine.length != recordLength){
		        Toast.makeText(CategoryList.this, R.string.import_error_first_line,
		                Toast.LENGTH_SHORT).show();
		        return;
		    }
		    if ((nextLine[0].compareToIgnoreCase(getString(R.string.category)) != 0) ||
			    (nextLine[1].compareToIgnoreCase(getString(R.string.description)) != 0) ||
			    (nextLine[2].compareToIgnoreCase(getString(R.string.website)) != 0) ||
			    (nextLine[3].compareToIgnoreCase(getString(R.string.username)) != 0) ||
			    (nextLine[4].compareToIgnoreCase(getString(R.string.password)) != 0) ||
			    (nextLine[5].compareToIgnoreCase(getString(R.string.notes)) != 0))
		    {
		        Toast.makeText(CategoryList.this, R.string.import_error_first_line,
		                Toast.LENGTH_SHORT).show();
		        return;
		    }
//		    Log.i(TAG,"first line is valid");
		    
		    HashMap<String, Long> categoryToId=getCategoryToId();
		    //
		    // take a pass through the CSV and collect any new Categories
		    //
			HashMap<String,Long> categoriesFound = new HashMap<String,Long>();
		    int categoryCount=0;
		    while ((nextLine = reader.readNext()) != null) {
		        // nextLine[] is an array of values from the line
		        if ((nextLine==null) || (nextLine[0]=="")){
		        	continue;	// skip blank categories
		        }
		        if (categoryToId.containsKey(nextLine[0])){
		        	continue;	// don't recreate existing categories
		        }
		        categoryCount++;
	        	Long passwordsInCategory= new Long(1);
		        if (categoriesFound.containsKey(nextLine[0])) {
		        	passwordsInCategory+=categoriesFound.get(nextLine[0]);
		        }
	        	categoriesFound.put(nextLine[0], passwordsInCategory);
		        if (categoryCount>MAX_CATEGORIES){
			        Toast.makeText(CategoryList.this, R.string.import_too_many_categories,
			                Toast.LENGTH_SHORT).show();
			        return;
		        }
		    }
		    if (categoryCount!=0)
		    {
			    Set<String> categorySet = categoriesFound.keySet();
			    Iterator<String> i=categorySet.iterator();
			    while (i.hasNext()){
		    		addCategory(i.next());
			    }
		    }
		    reader.close();

		    categoryToId=getCategoryToId();	// re-read the categories to get id's of new categories
		    //
		    // read the whole file again to import the actual fields
		    //
			reader = new CSVReader(new FileReader(EXPORT_FILENAME));
		    nextLine = reader.readNext();
		    int newEntries=0;
		    int lineNumber=0;
		    String lineErrors="";
		    int lineErrorsCount=0;
		    final int maxLineErrors=10;
		    while ((nextLine = reader.readNext()) != null) {
		    	lineNumber++;
//		    	Log.d(TAG,"lineNumber="+lineNumber);
		    	
		        // nextLine[] is an array of values from the line
			    if (nextLine.length < 2){
			    	if (lineErrorsCount < maxLineErrors) {
				    	lineErrors += "line "+lineNumber+": "+
				    		getString(R.string.import_not_enough_fields)+"\n";
			    		lineErrorsCount++;
			    	}
			    	continue;	// skip if not enough fields
			    }
			    if (nextLine.length < recordLength){
			    	// if the fields after category and description are missing, 
			    	// just fill them in
			    	String [] replacement=new String[recordLength];
			    	for (int i=0;i<nextLine.length; i++) {
			    		// copy over the fields we did get
			    		replacement[i]=nextLine[i];
			    	}
			    	for (int i=nextLine.length; i<recordLength; i++) {
			    		// flesh out the rest of the fields
			    		replacement[i]="";
			    	}
			    	nextLine=replacement;
			    }
		        if ((nextLine==null) || (nextLine[0]=="")){
			    	if (lineErrorsCount < maxLineErrors) {
				    	lineErrors += "line "+lineNumber+": "+
				    		getString(R.string.import_blank_category)+"\n";
			    		lineErrorsCount++;
			    	}
		        	continue;	// skip blank categories
		        }
		        String description=nextLine[1];
		        if ((description==null) || (description=="")){
			    	if (lineErrorsCount < maxLineErrors) {
				    	lineErrors += "line "+lineNumber+": "+
				    		getString(R.string.import_blank_description)+"\n";
			    		lineErrorsCount++;
			    	}
		        	continue;
		        }
		        
		        PassEntry entry=new PassEntry();
				try {
					entry.category = categoryToId.get(nextLine[0]);
				    entry.description = ch.encrypt(description);
				    entry.website = ch.encrypt(nextLine[2]);
				    entry.username = ch.encrypt(nextLine[3]);
				    entry.password = ch.encrypt(nextLine[4]);
				    entry.note = ch.encrypt(nextLine[5]);
				} catch(CryptoHelperException e) {
				    Log.e(TAG,e.toString());
				    continue;
				}
			    dbHelper.addPassword(entry);
		        newEntries++;
		    }
			reader.close();
			if (lineErrors != "") {
				Log.d(TAG,lineErrors);
			}

			if (newEntries==0)
		    {
		        Toast.makeText(CategoryList.this, R.string.import_no_entries,
		                Toast.LENGTH_SHORT).show();
		        return;
		    }else{
				Toast.makeText(this, getString(R.string.added)+ " "+ newEntries +
						" "+ getString(R.string.entries),
						Toast.LENGTH_SHORT).show();
				fillData();
		    }
		} catch (IOException e) {
			e.printStackTrace();
	        Toast.makeText(CategoryList.this, R.string.import_file_error,
	                Toast.LENGTH_LONG).show();
		}
	}

	public HashMap<String, Long> getCategoryToId()
	{
		if (ch == null){
			ch = new CryptoHelper();
		}
		if(PBEKey == null) {
		    PBEKey = "";
		}
		ch.setPassword(PBEKey);
	
		HashMap<String,Long> categories = new HashMap<String,Long>();
		rows = dbHelper.fetchAllCategoryRows();

		for (CategoryEntry row : rows) {
		    String cryptDesc = row.name;
		    row.plainName = "";
		    try {
				row.plainName = ch.decrypt(cryptDesc);
				categories.put(row.plainName, row.id);
		    } catch (CryptoHelperException e) {
				Log.e(TAG,e.toString());
		    }
		}
		return categories;
	}
}