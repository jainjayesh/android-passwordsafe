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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class Restore {
	
	private static boolean debug = false;
	private static final String TAG = "Restore";
	
	DBHelper dbHelper=null;
	Handler myViewHandler=null;
	Context myCtx=null;

	RestoreDataSet restoreDataSet;

	public Restore(Handler handler, Context ctx) {
		myViewHandler=handler;
		myCtx=ctx;
	}
	
	public boolean read(String filename, String PBEKey) {
		if (debug) Log.d(TAG,"read("+filename+",)");
    	
		FileReader fr;
		try {
			fr = new FileReader(filename);
		} catch (FileNotFoundException e1) {
			// e1.printStackTrace();
			Toast.makeText(myCtx, myCtx.getString(R.string.restore_unable_to_open)+
				" "+e1.getLocalizedMessage(),
				Toast.LENGTH_LONG).show();
			return false;
		}

		SAXParserFactory spf = SAXParserFactory.newInstance();
		try {
			SAXParser sp = spf.newSAXParser();

			XMLReader xr = sp.getXMLReader(); 

			RestoreHandler myRestoreHandler = new RestoreHandler();
			xr.setContentHandler(myRestoreHandler); 

			xr.parse(new InputSource(fr)); 

			restoreDataSet = myRestoreHandler.getParsedData();

		} catch (ParserConfigurationException e) {
			//e.printStackTrace();
			Toast.makeText(myCtx, myCtx.getString(R.string.restore_unable_to_open)+
				" "+e.getLocalizedMessage(),
				Toast.LENGTH_LONG).show();
			return false;
		} catch (SAXException e) {
			//e.printStackTrace();
			Toast.makeText(myCtx, myCtx.getString(R.string.restore_unable_to_open)+
				" "+e.getLocalizedMessage(),
				Toast.LENGTH_LONG).show();
			return false;
		} catch (IOException e) {
			//e.printStackTrace();
			Toast.makeText(myCtx, myCtx.getString(R.string.restore_unable_to_open)+
				" "+e.getLocalizedMessage(),
				Toast.LENGTH_LONG).show();
			return false;
		} 

		if (restoreDataSet.getVersion() != Backup.CURRENT_VERSION) {
			Toast.makeText(myCtx, myCtx.getString(R.string.restore_bad_version)+
				" "+Integer.toString(restoreDataSet.getVersion()),
				Toast.LENGTH_LONG).show();
        	return false;
		}
		CategoryEntry firstCatEntry=restoreDataSet.getCategories().get(0);
		if (firstCatEntry==null) {
			Toast.makeText(myCtx, myCtx.getString(R.string.restore_error),
				Toast.LENGTH_LONG).show();
			return false;
		}
		CryptoHelper ch=new CryptoHelper();
		ch.setPassword(PBEKey);
		String firstCategory="";
		try {
			firstCategory = ch.decrypt(firstCatEntry.name);
		} catch (CryptoHelperException e) {
			Log.e(TAG,e.toString());
		}
		if (ch.getStatus() == false) {
			Toast.makeText(myCtx, myCtx.getString(R.string.restore_decrypt_error),
				Toast.LENGTH_LONG).show();
			return false;
		}
		if (debug) Log.d(TAG,"firstCategory="+firstCategory);

		dbHelper=new DBHelper(myCtx);

		String msg=myCtx.getString(R.string.restore_found)+" "+
        	Integer.toString(restoreDataSet.getTotalEntries())+" "+
        	myCtx.getString(R.string.restore_passwords)+" "+
        	restoreDataSet.getDate()+".\n"+
			myCtx.getString(R.string.dialog_restore_database_msg);
		Dialog confirm = new AlertDialog.Builder(myCtx)
		.setIcon(android.R.drawable.ic_menu_manage)
		.setTitle(R.string.dialog_restore_database_title)
		.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				restoreDatabase();
			}
		})
		.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dbHelper.close();
			}
		}) 
		.setMessage(msg)
		.create();
		confirm.show();

		return true;
	}
    
	private void restoreDatabase() {
		dbHelper.beginTransaction();
		dbHelper.deleteDatabase();

		for (CategoryEntry category : restoreDataSet.getCategories()) {
			if (debug) Log.d(TAG,"category="+category.name);
			dbHelper.addCategory(category);
		}
		int totalPasswords=0;
		for (PassEntry password : restoreDataSet.getPass()) {
			totalPasswords++;
			dbHelper.addPassword(password);
		}
		dbHelper.commit();
		dbHelper.close();

		Toast.makeText(myCtx, myCtx.getString(R.string.restore_complete)+
			" "+Integer.toString(totalPasswords),
			Toast.LENGTH_LONG).show();

		Message m = new Message();
		m.what = CategoryList.MSG_FILLDATA;
		myViewHandler.sendMessage(m);
	}
}
