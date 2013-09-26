FileSelector
============

FileSelector is a library designed to facilitate the ability to browse
the Android filesystem and select a filepath to open or save files.

The code is taken from: 

  http://developer.samsung.com/android/technical-docs/Implementing-a-file-selector-dialog

and modified into a library project.  The modifications include, but are not
limited to, adding `fs_` prefixes to all resource ids and various bugfixes.

This code was chosen because it did not have a restrictive license at the time
of copying (2013-09-26), and it has numerous nifty features such as the ability
to create new folders when saving and the ability to set a file filter.

Developed By
============

Originally created by an unknown author for http://developer.samsung.com.

Modified into a library project and improved with various bugfixes and new
features by pragma78@gmail.com for https://github.com/pragma-/networklog.

Example
=======

    package com.samsung.sprc.fileselector;

    import android.app.Activity;
    import android.os.Bundle;
    import android.view.View;
    import android.view.View.OnClickListener;
    import android.widget.Button;
    import android.widget.Toast;

    public class FileSelectorActivity extends Activity {

      private Button mLoadButton;

      private Button mSaveButton;

      /** Sample filters array */
      final String[] mFileFilter = { "*.*", ".jpeg", ".txt", ".png" };

      @Override
        public void onCreate(final Bundle savedInstanceState) {
          super.onCreate(savedInstanceState);
          setContentView(R.layout.main);

          mLoadButton = (Button) findViewById(R.id.fs_button_load);
          mSaveButton = (Button) findViewById(R.id.fs_button_save);

          mLoadButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
              new FileSelector(FileSelectorActivity.this, FileOperation.LOAD, mLoadFileListener, mFileFilter).show();
            }
          });

          mSaveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
              new FileSelector(FileSelectorActivity.this, FileOperation.SAVE, mSaveFileListener, mFileFilter).show();
            }
          });
        }

      OnHandleFileListener mLoadFileListener = new OnHandleFileListener() {
        @Override
          public void handleFile(final String filePath) {
            Toast.makeText(FileSelectorActivity.this, "Load: " + filePath, Toast.LENGTH_SHORT).show();
          }
      };

      OnHandleFileListener mSaveFileListener = new OnHandleFileListener() {
        @Override
          public void handleFile(final String filePath) {
            Toast.makeText(FileSelectorActivity.this, "Save: " + filePath, Toast.LENGTH_SHORT).show();
          }
      };
    }
