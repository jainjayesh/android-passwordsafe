package com.bitsetters.android.passwordsafe;

// Adder service interface.
interface ServiceDispatch {
  void setPassword (String masterPasswordIn);
  String getPassword ();
  String encrypt (String clearText);
  String decrypt (String cryptoText);
}
