Note that the import and export commands generate and operate on unencrypted data.  That means that an unencrypted version of your password file will live on your SD card (and perhaps your desktop computer).  You will probably either want to delete these files or encrypt them by some other means in order maintain the level of security offered by Android Password Safe's Encryption capabilities.

# Export #

  1. Make sure the G1 is not connected via the USB cable.
  1. From the Categories screen, select the menu item **Export Database**.
  1. The file "passwordsafe.csv" will be created on the SD card.
  1. Connect the G1 to your computer with the USB cable.
  1. Look for "passwordsafe.csv" in the base directory of the USB storage.
  1. Note that this file is unencrypted, and you may want to delete it later.

![http://android-passwordsafe.googlecode.com/files/export.png](http://android-passwordsafe.googlecode.com/files/export.png)

# Import #

  1. On your computer, use a spreadsheet program like Excel.
  1. Create the following headers in row 1:
> > "Category","Description","Website","Username","Password","Notes"
  1. In rows 2 and lower, add your appropriate data.
  1. Perform a "Save As.." with a name of "passwordsafe.csv" and type of "CSV".
  1. Connect the G1 to your computer with the USB cable.
  1. Copy "passwordsafe.csv" onto the base directory of the USB storage on your G1.
  1. Disconnect the USB cable.
  1. On the G1 in Password Safe, from the Categories screen, select the menu item **Import Database**.
  1. Your first prompt will ask "Do you want to replace the database?".  Answer Yes or No.
    * Answering Yes will delete all data currently in Password Safe and replace it with the CSV data.
    * Answering No will effectively append the new data.
  1. After you have verified that your data has been imported successfully, you may want to delete any unencrypted copies of this file that you may have created.