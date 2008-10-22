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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

//import org.bouncycastle.jce.provider.BouncyCastleProvider;

import android.util.Log;

/**
 * Crypto helper class.
 * 
 * Basic crypto class that uses Bouncy Castle Provider to
 * encrypt/decrypt data using PBE (Password Based Encryption) via
 * 128Bit AES. I'm fairly new to both Crypto and Java so if you 
 * notice I've done something terribly wrong here please let me
 * know.
 *
 * @author Steven Osborn - http://steven.bitsetters.com
 */
public class CryptoHelper {

    private static String TAG = "CryptoHelper";
    protected static PBEKeySpec pbeKeySpec;
    protected static PBEParameterSpec pbeParamSpec;
    protected static SecretKeyFactory keyFac;
    protected static String algorithm = "PBEWithMD5And128BitAES-CBC-OpenSSL";
//  protected static String algorithm = "PBEWithSHA1And128BitAES-CBC-BC";  // slower
//	protected static String algorithm = "PBEWithSHA1And256BitAES-CBC-BC";  // even slower
    protected static String password = null;          
    protected static SecretKey pbeKey;
    protected static Cipher pbeCipher;

    private static final byte[] salt = {
		(byte)0xfc, (byte)0x76, (byte)0x80, (byte)0xae,
		(byte)0xfd, (byte)0x82, (byte)0xbe, (byte)0xee,
    };

    private static final int count = 20;

    /**
     * 
     * @throws Exception
     */
    CryptoHelper() {
		pbeParamSpec = new PBEParameterSpec(salt,count);
		try {
		    keyFac = SecretKeyFactory
		    .getInstance(algorithm,"BC");
		} catch (NoSuchAlgorithmException e) {
		    Log.e(TAG,"CryptoHelper(): "+e.toString());
		} catch (NoSuchProviderException e) {
		    Log.e(TAG,"CryptoHelper(): "+e.toString());		
		}
    }

    /**
     * 
     * @param message
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static byte[] md5String(String message) {
	
		byte[] input = message.getBytes();
	
		MessageDigest hash;
		ByteArrayInputStream	bIn = null;
		DigestInputStream	dIn = null;
	
		try {
		    hash = MessageDigest.getInstance("MD5");
	
		    bIn = new ByteArrayInputStream(input);
		    dIn = new DigestInputStream(bIn, hash);
	
		    for(int i=0;i<input.length;i++) {
		    	dIn.read();
		    }
	
		} catch (NoSuchAlgorithmException e) {
		    Log.e(TAG,"md5String(): "+e.toString());
		} catch (IOException e) {
		    Log.e(TAG,"md5String(): "+e.toString());
		}
	
		return dIn.getMessageDigest().digest();
    }

    /**
     * 
     * @param bytes
     * @return
     */
    public static String toHexString(byte bytes[]) {
	
		StringBuffer retString = new StringBuffer();
		for (int i = 0; i < bytes.length; ++i) {
		    retString.append(Integer
			    .toHexString(0x0100 + (bytes[i] & 0x00FF))
			    .substring(1));
		}
		return retString.toString();
    }
    
    public static byte[] hexStringToBytes(String hex) {
    	
    	byte [] bytes = new byte [hex.length() / 2];
		int j = 0;
		for (int i = 0; i < hex.length(); i += 2)
		{
			try {
				String hexByte=hex.substring(i, i+2);

				Integer I = new Integer (0);
				I = Integer.decode("0x"+hexByte);
				int k = I.intValue ();
				bytes[j++] = new Integer(k).byteValue();
			} catch (NumberFormatException e)
			{
				Log.i(TAG,e.getLocalizedMessage());
				return bytes;
			}
		}
    	return bytes;
    }

    /**
     * Set the password to be used as an encryption key
     * 
     * @param pass
     * @throws Exception
     */
    public void setPassword(String pass) {
		password = pass;
		pbeKeySpec = new PBEKeySpec(password.toCharArray());
		try {
		    pbeKey = keyFac.generateSecret(pbeKeySpec);
		    pbeCipher = Cipher
		    .getInstance(algorithm,"BC");
		} catch (InvalidKeySpecException e) {
		    Log.e(TAG,"setPassword(): "+e.toString());
		} catch (NoSuchAlgorithmException e) {
		    Log.e(TAG,"setPassword(): "+e.toString());
		} catch (NoSuchProviderException e) {
		    Log.e(TAG,"setPassword(): "+e.toString());
		} catch (NoSuchPaddingException e) {
		    Log.e(TAG,"setPassword(): "+e.toString());
		}
    }

    /**
     * encrypt a string
     * 
     * @param plaintext
     * @return encrypted String
     * @throws Exception
     */
    public String encrypt(String plaintext) throws CryptoHelperException {
		if(password == null) {
		    String msg = "Must call setPassword before runing encrypt.";
		    throw new CryptoHelperException(msg);
		}
		byte[] ciphertext = {};
	
		try {
		    pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
		    ciphertext = pbeCipher.doFinal(plaintext.getBytes());
		} catch (IllegalBlockSizeException e) {
		    Log.e(TAG,"encrypt(): "+e.toString());
		} catch (BadPaddingException e) {
		    Log.e(TAG,"encrypt(): "+e.toString());
		} catch (InvalidKeyException e) {
		    Log.e(TAG,"encrypt(): "+e.toString());
		} catch (InvalidAlgorithmParameterException e) {
		    Log.e(TAG,"encrypt(): "+e.toString());
		}
	
		String stringCiphertext=toHexString(ciphertext);
		return stringCiphertext;
    }

    /**
     * unencrypt previously encrypted string
     * 
     * @param ciphertext
     * @return decrypted String
     * @throws Exception
     */
    public String decrypt(String ciphertext) throws CryptoHelperException {
		if(password == null) {
		    String msg = "Must call setPassword before runing decrypt.";
		    throw new CryptoHelperException(msg);
		}
	
		byte[] byteCiphertext=hexStringToBytes(ciphertext);
		byte[] plaintext = {};
		
		try {
		    pbeCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
		    plaintext = pbeCipher.doFinal(byteCiphertext);
		} catch (IllegalBlockSizeException e) {
		    Log.e(TAG,"decrypt(): "+e.toString());
		} catch (BadPaddingException e) {
		    Log.e(TAG,"decrypt(): "+e.toString());
		} catch (InvalidKeyException e) {
		    Log.e(TAG,"decrypt(): "+e.toString());
		} catch (InvalidAlgorithmParameterException e) {
		    Log.e(TAG,"decrypt(): "+e.toString());
		}
	
		return new String(plaintext);
    }

}
