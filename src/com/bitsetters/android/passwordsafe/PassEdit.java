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

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * PassEdit Activity
 * 
 * @author Steven Osborn - http://steven.bitsetters.com
 * @author Randy McEoin
 */
public class PassEdit extends Activity {

    public static final int SAVE_PASSWORD_INDEX = Menu.FIRST;   
    public static final int DEL_PASSWORD_INDEX = Menu.FIRST + 1;   

    private EditText descriptionText;
    private EditText passwordText;
    private EditText usernameText;
    private EditText websiteText;
    private EditText noteText;
    private Long RowId;
    private DBHelper dbHelper=null;
    private CryptoHelper ch;

    private static String TAG = "PassEdit";

    public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
	
		String title = getResources().getString(R.string.app_name) + " - " +
		getResources().getString(R.string.edit_entry);
		setTitle(title);
		
		ch = new CryptoHelper();
		ch.setPassword(PassList.getPBEKey());

		if (dbHelper == null){
			dbHelper = new DBHelper(this);
		}
		
		setContentView(R.layout.pass_edit);
	
		descriptionText = (EditText) findViewById(R.id.description);
		passwordText = (EditText) findViewById(R.id.password);
		usernameText = (EditText) findViewById(R.id.username);
		noteText = (EditText) findViewById(R.id.note);
		websiteText = (EditText) findViewById(R.id.website);
	
		Button confirmButton = (Button) findViewById(R.id.save);
	
		RowId = icicle != null ? icicle.getLong(PassList.KEY_ID) : null;
		if (RowId == null) {
		    Bundle extras = getIntent().getExtras();            
		    RowId = extras != null ? extras.getLong(PassList.KEY_ID) : null;
		}
	
		populateFields();
	
		confirmButton.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View arg0) {
				// Don't allow the user to enter a blank description, we need
				// something useful to show in the list
				if(descriptionText.getText().toString().trim().length() == 0) {
		            Toast.makeText(PassEdit.this, R.string.notify_blank_desc,
		                    Toast.LENGTH_SHORT).show();
				    return;
				}
				savePassword();
		    }
		});
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	if (RowId != null) {
    		outState.putLong(PassList.KEY_ID, RowId);
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
		if (CategoryList.isSignedIn()==false) {
			saveState();
			finish();
		}
		populateFields();
    }

    private void saveState() {
		PassEntry entry =  new PassEntry();
	
		String passwordPlain = passwordText.getText().toString();
		String notePlain = noteText.getText().toString();
		String usernamePlain = usernameText.getText().toString();
		String websitePlain = websiteText.getText().toString();
		String descPlain = descriptionText.getText().toString();

		try {
			entry.category = PassList.getCategoryId();
		    entry.description = ch.encrypt(descPlain);
		    entry.username = ch.encrypt(usernamePlain);
		    entry.password = ch.encrypt(passwordPlain);
		    entry.note = ch.encrypt(notePlain);
		    entry.website = ch.encrypt(websitePlain);
		} catch(CryptoHelperException e) {
		    Log.e(TAG,e.toString());
		}
	
	
		if(RowId == null) {
		    dbHelper.addPassword(entry);
		} else {
		    dbHelper.updatePassword(RowId, entry);
		}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, SAVE_PASSWORD_INDEX, 0, R.string.save)
		.setIcon(android.R.drawable.ic_menu_save);
		menu.add(0, DEL_PASSWORD_INDEX, 0, R.string.password_delete)
			.setIcon(android.R.drawable.ic_menu_delete);
	
		return super.onCreateOptionsMenu(menu);
    }

    /**
     * Save the password entry and finish the activity.
     */
    private void savePassword() {
		saveState();
		setResult(RESULT_OK);
		finish();
    }
    /**
     * Delete the password entry from the database given the row id within the database.
     * 
     * @param Id
     */
    private void delPassword(long Id) {
		dbHelper.deletePassword(Id);
		setResult(RESULT_OK);
		finish();
    }

    /**
     * Handler for when a MenuItem is selected from the Activity.
     */
    public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case SAVE_PASSWORD_INDEX:
			savePassword();
			break;
		case DEL_PASSWORD_INDEX:
		    try {
		    	if (RowId>0) {
		    		delPassword(RowId);
		    	}
		    } catch (IndexOutOfBoundsException e) {
				// This should only happen when there are no
				// entries to delete.
				Log.w(TAG,e.toString());
		    }
		    break;
		}
		return super.onOptionsItemSelected(item);
    }

    private void populateFields() { 
		if (RowId != null) {
		    PassEntry row = dbHelper.fetchPassword(RowId);
		    if (row.id > -1) {
				String cryptDesc = row.description;
				String cryptNote = row.note;
				String cryptWebsite = row.website;
				String cryptPass = row.password;
				String cryptUsername = row.username;
				try {
				    descriptionText.setText(ch.decrypt(cryptDesc));
				    passwordText.setText(ch.decrypt(cryptPass));
				    usernameText.setText(ch.decrypt(cryptUsername));
				    noteText.setText(ch.decrypt(cryptNote));
				    websiteText.setText(ch.decrypt(cryptWebsite));
				} catch (CryptoHelperException e) {
				    Log.e(TAG,e.toString());
				}
		    }            
		}
    }

}
