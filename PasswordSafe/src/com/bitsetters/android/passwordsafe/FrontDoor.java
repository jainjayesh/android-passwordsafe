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
package com.bitsetters.android.passwordsafe;


import org.openintents.intents.CryptoIntents;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.bitsetters.android.passwordsafe.AskPassword;
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

	private boolean debug = false;
	private static String TAG = "FrontDoor";

	private DBHelper dbHelper;
	private String masterKey;
	private CryptoHelper ch;

	// service elements
    private ServiceDispatch service;
    private ServiceDispatchConnection conn;

	public static String SERVICE_NAME = "com.bitsetters.android.passwordsafe.ServiceDispatchImpl";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		initService(); // start up the PWS service so other applications can query.
	}

	//currently only handles result from askPassword function.
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		switch (resultCode) {
		case RESULT_OK:
			masterKey = data.getStringExtra("masterKey");
			try {
				service.setPassword(masterKey); // should already be connected.
			} catch (RemoteException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
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
    	Intent callbackIntent = getIntent(); 
    	int callbackResult = RESULT_CANCELED;
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
        } else {
        	// get the body text out of the extras. we'll encrypt or decrypt this.
        	String inputBody = thisIntent.getStringExtra (CryptoIntents.EXTRA_TEXT);
        	String outputBody = "";
        	// which action?
        	if (action.equals (CryptoIntents.ACTION_ENCRYPT)) {
        		try {
        			outputBody = ch.encrypt (inputBody);
                	callbackResult = RESULT_OK;
                	// stash the encrypted/decrypted text in the extra
                	callbackIntent.putExtra(CryptoIntents.EXTRA_TEXT, outputBody);
        		} catch (CryptoHelperException e) {
        			Log.e(TAG, e.toString());
        		}  catch (NullPointerException e) {
        			if (debug) Log.e(TAG, e.toString() + "ch: " + ch + " inputBody: " + inputBody );
        			
        		}
        	} else if (action.equals (CryptoIntents.ACTION_DECRYPT)) {
        		try {
        			outputBody = ch.decrypt (inputBody);
                	callbackResult = RESULT_OK;
                	// stash the encrypted/decrypted text in the extra
                	callbackIntent.putExtra(CryptoIntents.EXTRA_TEXT, outputBody);
        		} catch (CryptoHelperException e) {
        			Log.e(TAG, e.toString());
        		}
        	} else if (action.equals (CryptoIntents.ACTION_GET_PASSWORD)
        			|| action.equals (CryptoIntents.ACTION_SET_PASSWORD)) {
        		try {
        			callbackIntent = getSetPassword (thisIntent, callbackIntent);
                	callbackResult = RESULT_OK;
        		} catch (CryptoHelperException e) {
        			Log.e(TAG, e.toString());
        			Toast.makeText(FrontDoor.this,
        					"There was a crypto error while retreiving the requested password: " + e.getMessage(),
        					Toast.LENGTH_SHORT).show();
        		} catch (Exception e) {
        			Log.e(TAG, e.toString());
        			//TODO: Turn this into a proper error dialog.
        			Toast.makeText(FrontDoor.this,
        					"There was an error in retreiving the requested password: " + e.getMessage(),
        					Toast.LENGTH_SHORT).show();
        		}
        	}
        	setResult(callbackResult, callbackIntent);
        }
        finish();
	}

	private Intent getSetPassword (Intent thisIntent, Intent callbackIntent) throws CryptoHelperException, Exception {
        String action = thisIntent.getAction();
        //TODO: Consider moving this elsewhere. Maybe DBHelper? Also move strings to resource.
        DBHelper dbHelper = new DBHelper(this);
        Log.d(TAG, "GET_or_SET_PASSWORD");
        String username = null;
        String password = null;
        String callingPackage = getCallingPackage();
        String clearCategory  = thisIntent.getStringExtra (CryptoIntents.EXTRA_CATEGORY);
        if (clearCategory == null || clearCategory.equals("")) {
        	clearCategory = callingPackage;
        }
        /*TODO: if clearCategory != callingPackage, ask the user if it's permissible:
         * "Application 'org.syntaxpolice.ServiceTest' wants to access the
				password for 'opensocial'.
				[ ] Grant access this time.
				[ ] Always grant access.
				[ ] Always grant access to all passwords in org.syntaxpolice.ServiceTest category?
				[ ] Don't grant access"
         */
        if (clearCategory != callingPackage)  throw new Exception ("It is currently not permissible for this application to request passwords from this category.");

        String clearDescription = thisIntent.getStringExtra (CryptoIntents.EXTRA_DESCRIPTION);

        if (clearDescription == null) throw new Exception ("EXTRA_DESCRIPTION not set.");

        String category = ch.encrypt(clearCategory);
        String description = ch.encrypt(clearDescription);

    	PassEntry row = dbHelper.fetchPassword(category, description);
        if (action.equals (CryptoIntents.ACTION_GET_PASSWORD)) {
        	if (row.id > 1) {
        		username = ch.decrypt(row.username);
        		password = ch.decrypt(row.password);
        	} else throw new Exception ("Could not find password with given description in category corresponding to calling application.");

        	// stashing the return values:
        	callbackIntent.putExtra(CryptoIntents.EXTRA_USERNAME, username);
        	callbackIntent.putExtra(CryptoIntents.EXTRA_PASSWORD, password);
        } else if (action.equals (CryptoIntents.ACTION_SET_PASSWORD)) {
            String clearUsername  = thisIntent.getStringExtra (CryptoIntents.EXTRA_USERNAME);
            String clearPassword = thisIntent.getStringExtra (CryptoIntents.EXTRA_PASSWORD);
            if (clearUsername == null || clearPassword == null) {
            		throw new Exception ("EXTRAS USERNAME and PASSWORD must be set.");
            }  
            row.username = ch.encrypt(clearUsername);
            row.password = ch.encrypt(clearPassword);
        	if (row.id > 1) { //exists already 
        		if (clearUsername.equals("") && clearPassword.equals("")) {
        			dbHelper.deletePassword(row.id);
        		} else {
        			dbHelper.updatePassword(row.id, row);
        		}
        	} else {// add a new one
                row.description = description;
	            row.website = ""; // TODO: Should we send these fields in extras also?
	            row.note = "";

	            CategoryEntry c = new CategoryEntry();
	            c.name = category;
	            row.category =dbHelper.addCategory(c); //doesn't add category if it already exists
	            dbHelper.addPassword(row);
        	}
        }
        
        dbHelper.close();
        dbHelper = null;

        return (callbackIntent);
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		if (debug)
			Log.d(TAG, "onPause()");

		releaseService();
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
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		releaseService();
	}


	//--------------------------- service stuff ------------
	private void initService() {
		conn = new ServiceDispatchConnection();
		Intent i = new Intent();
		i.setClassName( "com.bitsetters.android.passwordsafe", SERVICE_NAME );
		//TODO: Get these strings from a resource?
		startService(i);
		bindService( i, conn, Context.BIND_AUTO_CREATE);
	}

	private void releaseService() {
		if (conn != null ) {
			unbindService( conn );
			conn = null;
		}
	}

	class ServiceDispatchConnection implements ServiceConnection
	{
		public void onServiceConnected(ComponentName className, 
				IBinder boundService )
		{
			service = ServiceDispatch.Stub.asInterface((IBinder)boundService);
			try {
				if (service.getPassword() == null) {
					// the service isn't running
					Intent askPass = new Intent(getApplicationContext(),
							AskPassword.class);

					final Intent thisIntent = getIntent();
					String inputBody = thisIntent.getStringExtra (CryptoIntents.EXTRA_TEXT);

					askPass.putExtra (CryptoIntents.EXTRA_TEXT, inputBody);
					//TODO: Is there a way to make sure all the extras are set?	
					startActivityForResult (askPass, 0);

				} else {
					//service already started, so don't need to ask pw.
					masterKey = service.getPassword();
					actionDispatch();
				}
			} catch (RemoteException e) {
				Log.d(TAG, e.toString());
			}
			Log.d( TAG,"onServiceConnected" );
		}
		
		public void onServiceDisconnected(ComponentName className)
		{
			service = null;
			Log.d( TAG,"onServiceDisconnected" );
		}
	};

}
