#!/bin/sh
jarsigner -verbose -keystore /home/steven/Documents/android.keystore -signedjar /home/steven/Desktop/android-passwordsafe_signed.apk bin/android-passwordsafe.apk android
