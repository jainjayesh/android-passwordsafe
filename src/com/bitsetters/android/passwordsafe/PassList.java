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

import android.app.ListActivity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Menu.Item;
import android.view.View.MeasureSpec;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

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
    private static final int ACTIVITY_CREATE=0;
    private static final int ACTIVITY_EDIT=1;

    // Menu Item order
    public static final int EDIT_PASSWORD_INDEX = Menu.FIRST;
    public static final int ADD_PASSWORD_INDEX = Menu.FIRST + 1;
    public static final int DEL_PASSWORD_INDEX = Menu.FIRST + 2;   

    public static final String KEY_ID = "id";  // Intent keys

    private CryptoHelper ch;
    private DBHelper dbHelper;

    private static boolean signedIn = false;  // Has user logged in
    private static String PBEKey;	      // Password Based Encryption Key			

    private List<PassEntry> rows;

    /** 
     * Called when the activity is first created. 
     */
    @Override
    public void onCreate(Bundle icicle) {
	super.onCreate(icicle);
	setContentView(R.layout.pass_list);
	dbHelper = new DBHelper(this);
	if(!signedIn) {
	    Intent i = new Intent(this, FrontDoor.class);
	    startSubActivity(i, ACTIVITY_CREATE);
	} 

	fillData();
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
	    String plainDesc = "";
	    try {
		plainDesc = ch.decrypt(cryptDesc);
		items.add(plainDesc);
	    } catch (CryptoHelperException e) {
		Log.e(TAG,e.toString());
	    }
	}
	ArrayAdapter<String> entries = 
	    new ArrayAdapter<String>(this, R.layout.pass_row, items);
	setListAdapter(entries);
	setupListStripes();
    }

    /**
     * Add stripes to the list view.
     * 
     * This will alternate row colors in the list view. 100% borrowed from
     * googles notepad application.  
     */
    private void setupListStripes() {
	// Get Drawables for alternating stripes
	Drawable[] lineBackgrounds = new Drawable[2];

	lineBackgrounds[0] = getResources().getDrawable(R.drawable.even_stripe);
	lineBackgrounds[1] = getResources().getDrawable(R.drawable.odd_stripe);

	// Make and measure a sample TextView of the sort our adapter will
	// return
	View view = getViewInflate().inflate(
		android.R.layout.simple_list_item_1, null, null);

	TextView v = (TextView) view.findViewById(android.R.id.text1);
	v.setText("X");
	// Make it 100 pixels wide, and let it choose its own height.
	v.measure(MeasureSpec.makeMeasureSpec(View.MeasureSpec.EXACTLY, 100),
		MeasureSpec.makeMeasureSpec(View.MeasureSpec.UNSPECIFIED, 0));
	int height = v.getMeasuredHeight();
	getListView().setStripes(lineBackgrounds, height);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	super.onCreateOptionsMenu(menu);

//	menu.add(0,EDIT_PASSWORD_INDEX, R.string.password_edit);
//	menu.addSeparator(EDIT_PASSWORD_INDEX, 0);
	menu.add(0,ADD_PASSWORD_INDEX, R.string.password_insert);
	//.setShortcut(arg0, arg1, arg2);
	menu.add(0, DEL_PASSWORD_INDEX, R.string.password_delete);  

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
	startSubActivity(i, ACTIVITY_CREATE);
    }

    private void delPassword(long Id) {
	dbHelper.deletePassword(Id);
	fillData();
    }

    public boolean onOptionsItemSelected(Item item) {
	switch(item.getId()) {
	case ADD_PASSWORD_INDEX:
	    addPassword();
	    break;
	case DEL_PASSWORD_INDEX:
	    try {
		delPassword(rows.get(getSelection()).id);
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
	startSubActivity(i, ACTIVITY_EDIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
	    String data, Bundle extras) {

	super.onActivityResult(requestCode, resultCode, data, extras);
	fillData();
    }


}