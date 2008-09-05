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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * FrontDoor Activity
 * 
 * This activity just acts as a splash screen and gets the password from the
 * user that will be used to decrypt/encrypt password entries.
 * 
 * @author Steven Osborn - http://steven.bitsetters.com
 */
public class FrontDoor extends Activity {

    private static String TAG = "FrontDoor";
    
    private EditText pbeKey;
    private DBHelper dbHelper;
    private TextView IntroText;
    private String PBEKey;
    private String confirmKey; 
    private CryptoHelper ch;
    private boolean firstTime = false;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
	super.onCreate(icicle);
	dbHelper = new DBHelper(this);
	ch = new CryptoHelper();

	//Setup layout
	setContentView(R.layout.front_door);
	ImageView icon = (ImageView) findViewById(R.id.entry_icon);
	icon.setImageResource(R.drawable.passicon);
	TextView header = (TextView) findViewById(R.id.entry_header);
	String version = getString(R.string.version);
	String appName = getString(R.string.app_name);
	String head = appName + " " + version + "\n";
	header.setText(head);

	pbeKey = (EditText) findViewById(R.id.password);
	IntroText = (TextView) findViewById(R.id.first_time);
	confirmKey = dbHelper.fetchPBEKey();
	if(confirmKey.length() == 0) {
	    firstTime = true;
	    IntroText.setVisibility(View.VISIBLE);
	}

	Button continueButton = (Button) findViewById(R.id.continue_button);

	continueButton.setOnClickListener(new View.OnClickListener() {

	    public void onClick(View arg0) {
		PBEKey = pbeKey.getText().toString();
		ch.setPassword(PBEKey);
		
		// Password must be at least 4 characters
		if(PBEKey.length() < 4) {
            Toast.makeText(FrontDoor.this, R.string.notify_blank_pass,
                    Toast.LENGTH_SHORT).show();
		    return;
		}
		
		// If it's the user's first time to enter a password,
		// we have to store it in the database.  We are going to 
		// store an encrypted hash of the password.
		if(firstTime) {
		    byte[] md5Key = CryptoHelper.md5String(PBEKey);
		    String hexKey = CryptoHelper.toHexString(md5Key);
		    String cryptKey = "";
		    Log.i(TAG, "Saving Password: " + hexKey );
		    try {
		    	cryptKey = ch.encrypt(hexKey);
		    	dbHelper.storePBEKey(cryptKey);
		    } catch (CryptoHelperException e) {
		    	Log.e(TAG,e.toString());
		    }
		} else if(!checkUserPassword()) {
		    // Check the user's password and display a 
		    // message if it's wrong
            Toast.makeText(FrontDoor.this, R.string.invalid_password,
                    Toast.LENGTH_SHORT).show();
		    return;
		}
		PassList.setPBEKey(PBEKey);
		
		setResult(RESULT_OK);
		finish();
	    }

	});
    }
    
    /**
     * 
     * @return
     */
    private boolean checkUserPassword() {
		byte[] md5Pass = CryptoHelper.md5String(PBEKey);
		String hexPass = CryptoHelper.toHexString(md5Pass);
		String decryptConfirm = "";
		try {
		    decryptConfirm = ch.decrypt(confirmKey);
		} catch (CryptoHelperException e) {
		    Log.e(TAG,e.toString());
		}
		if(decryptConfirm.compareTo(hexPass) == 0) {
		    return true;
		}
		return false;
    }
}
