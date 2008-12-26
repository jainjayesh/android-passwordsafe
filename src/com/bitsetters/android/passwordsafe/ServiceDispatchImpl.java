package com.bitsetters.android.passwordsafe;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class ServiceDispatchImpl extends Service {
	private CryptoHelper ch;
	private String masterKey;
    
    @Override
    public IBinder onBind(Intent intent) {
        // Select the interface to return.  If your service only implements
        // a single interface, you can just return it here without checking
        // the Intent.
    	return (mBinder);
    }

    @Override
    public void onCreate() {
      super.onCreate();
	  Log.d( "ServieDispatchImpl","onCreate" );
    }
    
    @Override
    public void onDestroy() {
	  super.onDestroy();
	  masterKey = null;
	  ch = null;
	  Log.d( "ADDERSERVICEIMPL","onDestroy" );
    }

    /**
     * The ServiceDispatch is defined through IDL
     */
    private final ServiceDispatch.Stub mBinder = new ServiceDispatch.Stub() {
    	private String TAG = "SERVICEDISPATCH";

    	public String encrypt (String clearText)  {
    		String cryptoText = null;
    		try {
    			cryptoText = ch.encrypt (clearText); 
    		} catch (CryptoHelperException e) {
    			Log.e(TAG, e.toString());
    		}  
    		return (cryptoText);
    	}

    	public String decrypt (String cryptoText)  {
    		String clearText = null;
    		try {
    			clearText = ch.decrypt (cryptoText); 
    		} catch (CryptoHelperException e) {
    			Log.e(TAG, e.toString());
    		}  
    		return (clearText);
    	}

    	public void setPassword (String masterKeyIn){
			ch = new CryptoHelper(CryptoHelper.EncryptionMedium);
			ch.setPassword(masterKeyIn);
			masterKey = masterKeyIn;
			Toast.makeText(ServiceDispatchImpl.this,
  					"Service started with master key.",
    				Toast.LENGTH_SHORT).show();
    	}

		@Override
		public String getPassword() {
			return masterKey;
		}
    };

}
