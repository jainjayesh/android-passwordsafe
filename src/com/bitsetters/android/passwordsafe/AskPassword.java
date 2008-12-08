/* $Id: AskPassword.java 54 2008-12-05 00:58:22Z rmceoin $
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * AskPassword Activity
 * 
 * This activity just acts as a splash screen and gets the password from the
 * user that will be used to decrypt/encrypt password entries.
 * 
 * @author Steven Osborn - http://steven.bitsetters.com
 */
public class AskPassword extends Activity {

	private boolean debug = true;
	private static String TAG = "AskPassword";

	private EditText pbeKey;
	private DBHelper dbHelper;
	private TextView introText;
	private TextView confirmText;
	private EditText confirmPass;
	private String PBEKey;
	private String masterKey;
	private CryptoHelper ch;
	private boolean firstTime = false;

	//probably remove these:
	public final String ACTION_ENCRYPT = "org.syntaxpolice.crypto.action.ENCRYPT";
	public final String ACTION_DECRYPT = "org.syntaxpolice.crypto.action.DECRYPT";
	
	public final String BODY = "org.syntaxpolice.crypto.extras.EXTRA_CRYPTO_BODY";
	public final String CALLBACK = "org.syntaxpolice.crypto.extras.EXTRA_CALLBACK";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		if (debug)
			Log.d(TAG, "onCreate()");

		dbHelper = new DBHelper(this);
		if (ch != null)
			Toast.makeText(AskPassword.this,
					"already have CH ",
					Toast.LENGTH_SHORT).show();
			
		ch = new CryptoHelper(CryptoHelper.EncryptionStrong);

		// Setup layout
		setContentView(R.layout.front_door);
		ImageView icon = (ImageView) findViewById(R.id.entry_icon);
		icon.setImageResource(R.drawable.passicon);
		TextView header = (TextView) findViewById(R.id.entry_header);
		String version = getString(R.string.version);
		String appName = getString(R.string.app_name);
		String head = appName + " " + version + "\n";
		header.setText(head);

		pbeKey = (EditText) findViewById(R.id.password);
		introText = (TextView) findViewById(R.id.first_time);
		confirmPass = (EditText) findViewById(R.id.pass_confirm);
		confirmText = (TextView) findViewById(R.id.confirm_lbl);
		masterKey = dbHelper.fetchMasterKey();
		if (masterKey.length() == 0) {
			firstTime = true;
			introText.setVisibility(View.VISIBLE);
			confirmText.setVisibility(View.VISIBLE);
			confirmPass.setVisibility(View.VISIBLE);
		}

		Button continueButton = (Button) findViewById(R.id.continue_button);

		continueButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				PBEKey = pbeKey.getText().toString();
				ch.setPassword(PBEKey);

				// Password must be at least 4 characters
				if (PBEKey.length() < 4) {
					Toast.makeText(AskPassword.this, R.string.notify_blank_pass,
							Toast.LENGTH_SHORT).show();
				    Animation shake = AnimationUtils
			        .loadAnimation(AskPassword.this, R.anim.shake);
			        
			        findViewById(R.id.password).startAnimation(shake);
					return;
				}

				// If it's the user's first time to enter a password,
				// we have to store it in the database. We are going to
				// store an encrypted hash of the password.
				if (firstTime) {

					// Make sure password and confirm fields match
					if (pbeKey.getText().toString().compareTo(
							confirmPass.getText().toString()) != 0) {
						Toast.makeText(AskPassword.this,
								R.string.confirm_pass_fail, Toast.LENGTH_SHORT)
								.show();
						return;
					}
					masterKey = CryptoHelper.generateDESKey();
					Log.i(TAG, "Saving Password: " + masterKey);
					try {
						String encryptedMasterKey = ch.encrypt(masterKey);
						dbHelper.storeMasterKey(encryptedMasterKey);
					} catch (CryptoHelperException e) {
						Log.e(TAG, e.toString());
					}
				} else if (!checkUserPassword()) {
					// Check the user's password and display a
					// message if it's wrong
					Toast.makeText(AskPassword.this, R.string.invalid_password,
							Toast.LENGTH_SHORT).show();
			        Animation shake = AnimationUtils
			        .loadAnimation(AskPassword.this, R.anim.shake);
			        
			        findViewById(R.id.password).startAnimation(shake);
					return;
				}

				Intent callbackIntent = new Intent();

				callbackIntent.putExtra("masterPass", PBEKey);
				callbackIntent.putExtra("masterKey", masterKey);


				// call-back the callback URL
				setResult(RESULT_OK, callbackIntent);
				finish();
			}
		});
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

	/**
	 * 
	 * @return
	 */
	private boolean checkUserPassword() {
		String encryptedMasterKey = dbHelper.fetchMasterKey();
		String decryptedMasterKey = "";
		try {
			decryptedMasterKey = ch.decrypt(encryptedMasterKey);
		} catch (CryptoHelperException e) {
			Log.e(TAG, e.toString());
		}
		if (ch.getStatus()==true) {
			masterKey=decryptedMasterKey;
			return true;
		}
		masterKey=null;
		return false;
	}
}
