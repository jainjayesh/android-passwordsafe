package org.syntaxpolice.apws.ServiceTest;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;

public class ServiceTest extends Activity {
	public final String TAG="SERVICE_TEST";
	public final String ACTION_ENCRYPT = "org.openintents.action.ENCRYPT";
	public final String ACTION_DECRYPT = "org.openintents.action.DECRYPT";
	public final String BODY = "org.openintents.extras.EXTRA_CRYPTO_BODY";
	public final Integer ENCRYPT_REQUEST = 1;
	public final Integer DECRYPT_REQUEST = 2;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		EditText inputText = (EditText) findViewById(R.id.input_entry);
		inputText.setText("Text to be Encrypted :)", android.widget.TextView.BufferType.EDITABLE);
		
// ---------------- clicky
		Button encryptIntentButton = (Button) findViewById(R.id.encrypti);
		Button decryptIntentButton = (Button) findViewById(R.id.decrypti);
		Button outToInButton       = (Button) findViewById(R.id.outToIn);
		
		encryptIntentButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				clickMaster (ENCRYPT_REQUEST);
			}});
		decryptIntentButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				clickMaster (DECRYPT_REQUEST);
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
		i.putExtra(BODY, inputStr);
        i.setType("text/plain");
        
    	if (request == ENCRYPT_REQUEST) {
            i.setAction(ACTION_ENCRYPT);
    		
    	} else if (request == DECRYPT_REQUEST) {
            i.setAction(ACTION_DECRYPT);
    	}
        try {
        	startActivityForResult(i, request);
        } catch (ActivityNotFoundException e) {
			Toast.makeText(ServiceTest.this,
					"Failed to invoke encrypt/decrypt!",
					Toast.LENGTH_SHORT).show();
			Log.e(TAG, "failed to invoke encrypt");
        }
    }
    
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        String resultText = data.getStringExtra (BODY);
		EditText outputText = (EditText) findViewById(R.id.output_entry);
		outputText.setText(resultText, android.widget.TextView.BufferType.EDITABLE);
    }
}
