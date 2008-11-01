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

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * CategoryEdit Activity
 * 
 * @author Randy McEoin
 */
public class CategoryEdit extends Activity {

    private EditText nameText;
    private Long RowId;
    private DBHelper dbHelper=null;
    private CryptoHelper ch;

    private static String TAG = "CategoryEdit";

    public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
	
		ch = new CryptoHelper();
		ch.setPassword(PassList.getPBEKey());

		if (dbHelper == null){
			dbHelper = new DBHelper(this);
		}
		
		String title = getResources().getString(R.string.app_name) + " - " +
		getResources().getString(R.string.edit_entry);
		setTitle(title);
		
		setContentView(R.layout.cat_edit);
	
		nameText = (EditText) findViewById(R.id.name);
	
		Button confirmButton = (Button) findViewById(R.id.save_category);
	
		RowId = icicle != null ? icicle.getLong(CategoryList.KEY_ID) : null;
		if (RowId == null) {
		    Bundle extras = getIntent().getExtras();            
		    RowId = extras != null ? extras.getLong(CategoryList.KEY_ID) : null;
		}
	
		populateFields();
	
		confirmButton.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View arg0) {
				// Don't allow the user to enter a blank name, we need
				// something useful to show in the list
				if(nameText.getText().toString().trim().length() == 0) {
		            Toast.makeText(CategoryEdit.this, R.string.notify_blank_name,
		                    Toast.LENGTH_SHORT).show();
				    return;
				}
				saveState();
				setResult(RESULT_OK);
				finish();
		    }
		});
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	if (RowId != null) {
    		outState.putLong(CategoryList.KEY_ID, RowId);
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
		populateFields();
    }

    private void saveState() {
		CategoryEntry entry =  new CategoryEntry();
	
		String namePlain = nameText.getText().toString();

		try {
		    entry.name = ch.encrypt(namePlain);
		} catch(CryptoHelperException e) {
		    Log.e(TAG,e.toString());
		}
	
	
		if(RowId == null) {
		    dbHelper.addCategory(entry);
		} else {
		    dbHelper.updateCategory(RowId, entry);
		}
    }

    private void populateFields() { 
		if (RowId != null) {
		    CategoryEntry row = dbHelper.fetchCategory(RowId);
		    if (row.id > -1) {
				String cryptName = row.name;
				try {
				    nameText.setText(ch.decrypt(cryptName));
				} catch (CryptoHelperException e) {
				    Log.e(TAG,e.toString());
				}
		    }            
		}
    }
}
