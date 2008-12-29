package org.openintents.intents;

// Version Dec 28, 2008

public class CryptoIntents {

	/**
	 * Activity Action: Encrypt the string given in the extra TEXT.
	 * Returns the encrypted string in the extra TEXT.
	 * 
	 * <p>Constant Value: "org.openintents.action.ENCRYPT"</p>
	 */
	public static final String ACTION_ENCRYPT = "org.openintents.action.ENCRYPT";

	/**
	 * Activity Action: Decrypt the string given in the extra TEXT.
	 * Returns the decrypted string in the extra TEXT.
	 * 
	 * <p>Constant Value: "org.openintents.action.DECRYPT"</p>
	 */
	public static final String ACTION_DECRYPT = "org.openintents.action.DECRYPT";
	

	public static final String ACTION_GET_PASSWORD = "org.openintents.action.GET_PASSWORD";
	public static final String ACTION_SET_PASSWORD = "org.openintents.action.SET_PASSWORD";
	
	/**
	 * The text to encrypt or decrypt.
	 * 
	 * <p>Constant Value: "org.openintents.extra.TEXT"</p>
	 */
	public static final String EXTRA_TEXT = "org.openintents.extra.TEXT";

}
