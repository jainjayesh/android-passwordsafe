/* $Id$
 * 
 * Copyright 2007-2008 Steven Osborn
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
package com.jayesh.android.safevault;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.jayesh.android.safevault.AskPassword;

/**
 * FrontDoor Activity
 * 
 * This activity just acts as a splash screen and gets the password from the
 * user that will be used to decrypt/encrypt password entries.
 * 
 * @author Steven Osborn - http://steven.bitsetters.com
 */
public class FrontDoor extends Activity {

	private boolean debug = false;
	private static String TAG = "FrontDoor";

	private DBHelper dbHelper;
	private String masterKey;
	private CryptoHelper ch;

	//probably remove these:
	public final String ACTION_ENCRYPT = "org.openintents.action.ENCRYPT";
	public final String ACTION_DECRYPT = "org.openintents.action.DECRYPT";
	
	public final String BODY = "org.openintents.extras.EXTRA_CRYPTO_BODY";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		if (masterKey == null || masterKey.equals("")) {
			Intent askPass = new Intent(getApplicationContext(),
					AskPassword.class);

			final Intent thisIntent = getIntent();
        	String inputBody = thisIntent.getStringExtra (BODY);
        	
        	askPass.putExtra (BODY, inputBody);
        	//TODO: Is there a way to make sure all the extras are set?
        	
			startActivityForResult (askPass, 0);
		} else {
			actionDispatch();
		}
	}

	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		switch (resultCode) {
		case RESULT_OK:
			masterKey = data.getStringExtra("masterKey");
			actionDispatch();
			break;
		case RESULT_CANCELED:
			setResult(RESULT_CANCELED);
			finish();
			break;
		}
	}
	
	protected void actionDispatch () {    
		final Intent thisIntent = getIntent();
        final String action = thisIntent.getAction();
		PassList.setMasterKey(masterKey);
        CategoryList.setMasterKey(masterKey);
        if (ch == null) {
    		ch = new CryptoHelper(CryptoHelper.EncryptionMedium);
    		ch.setPassword(masterKey);
        }

        if (action == null || action.equals(Intent.ACTION_MAIN)){
        	//TODO: When launched from debugger, action is null. Other such cases?
        	Intent i = new Intent(getApplicationContext(),
        			CategoryList.class);
        	startActivity(i);
        	finish();
        } else {
        	// get the body text out of the extras. we'll encrypt or decrypt this.
        	String inputBody = thisIntent.getStringExtra (BODY);
        	String outputBody = "";
        	// which action?
        	if (action.equals (ACTION_ENCRYPT)) {
        		try {
        			outputBody = ch.encrypt (inputBody);
        		} catch (CryptoHelperException e) {
        			Log.e(TAG, e.toString());
        		}  catch (NullPointerException e) {
        			if (debug) Log.e(TAG, e.toString() + "ch: " + ch + " inputBody: " + inputBody );
        			
        		}
        	} else if (action.equals (ACTION_DECRYPT)) {
        		try {
        			outputBody = ch.decrypt (inputBody);
        		} catch (CryptoHelperException e) {
        			Log.e(TAG, e.toString());
        		}
        	}
        	Intent callbackIntent = new Intent();
        	callbackIntent.setType("text/plain");
        	// stash the encrypted/decrypted text in the extra
        	callbackIntent.putExtra(BODY, outputBody);

        	// call-back the callback URL
        	setResult(RESULT_OK, callbackIntent);
        	finish(); //maybe we don't want to finish so that we can continue to respond to requests?
        }
			
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (debug)
			Log.d(TAG, "onPause()");

		dbHelper.close();
		dbHelper = null;
	}

	@Override
	protected void onResume() {
		super.onPause();

		if (debug)
			Log.d(TAG, "onResume()");
		if (dbHelper == null) {
			dbHelper = new DBHelper(this);
		}

	}
}
