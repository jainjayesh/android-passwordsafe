package org.syntaxpolice.apws.ServiceTest;

import org.openintents.intents.CryptoIntents;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ServiceTest extends Activity {
	public final String TAG="SERVICE_TEST";
	public final Integer ENCRYPT_REQUEST = 1;
	public final Integer DECRYPT_REQUEST = 2;
	public final Integer GET_PASSWORD = 3;
	public final String uri = "content://org.openintents.keys/org.syntaxpolice.apws.ServiceTest/opensocial";
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		EditText inputText = (EditText) findViewById(R.id.input_entry);
		inputText.setText(uri,
			android.widget.TextView.BufferType.EDITABLE);
		
// ---------------- clicky
		Button encryptIntentButton = (Button) findViewById(R.id.encrypti);
		Button decryptIntentButton = (Button) findViewById(R.id.decrypti);
		Button getButton           = (Button) findViewById(R.id.get);
		Button outToInButton       = (Button) findViewById(R.id.outToIn);
		
		encryptIntentButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				clickMaster (ENCRYPT_REQUEST);
			}});
		decryptIntentButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				clickMaster (DECRYPT_REQUEST);
			}});
		getButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View arg0) {
					clickMaster (GET_PASSWORD);
				}});
		//move output text box to input:
		outToInButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				EditText outputText = (EditText) findViewById(R.id.output_entry);
				EditText inputText = (EditText) findViewById(R.id.input_entry);
				String newInputStr = outputText.getText().toString();
				inputText.setText(newInputStr, android.widget.TextView.BufferType.EDITABLE);
				
			}});	
		
    }//oncreate
    
    private void clickMaster (Integer request) {
		EditText inputText = (EditText) findViewById(R.id.input_entry);
		String inputStr = inputText.getText().toString();
        Intent i = new Intent();
		i.putExtra(CryptoIntents.EXTRA_TEXT, inputStr);
        
    	if (request == ENCRYPT_REQUEST) {
            i.setAction(CryptoIntents.ACTION_ENCRYPT);
            i.setType("text/plain");
    	} else if (request == DECRYPT_REQUEST) {
            i.setAction(CryptoIntents.ACTION_DECRYPT);
            i.setType("text/plain");
    	} else if (request == GET_PASSWORD) {
    		i.setData(Uri.parse(inputStr));
    		i.setAction (CryptoIntents.ACTION_GET_PASSWORD);
    	}
        try {
        	startActivityForResult(i, request);
        } catch (ActivityNotFoundException e) {
			Log.e(TAG, "failed to invoke intent: " + e.toString());
        }
    }
    
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        String resultText = data.getStringExtra (CryptoIntents.EXTRA_TEXT);
		EditText outputText = (EditText) findViewById(R.id.output_entry);
		outputText.setText(resultText, android.widget.TextView.BufferType.EDITABLE);
    }
}
