package com.prestonmorgan.bigfootwatch;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.io.FileInputStream;


public class UploadActivity extends ActionBarActivity {

    final static private String DB_APP_KEY = "iu6vsm9jiz8xpnl";
    final static private String DB_APP_SECRET = "z97yoyo67gtgje2";
    public String mCurrentPhotoPath;
    private DropboxAPI<AndroidAuthSession> mDBApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        Intent intent = getIntent();
        mCurrentPhotoPath = intent.getStringExtra(MainActivity.EXTRA_PHOTO_PATH);
        TextView path = (TextView) findViewById(R.id.pathText);
        path.setText(mCurrentPhotoPath);

        AppKeyPair appKeys = new AppKeyPair(DB_APP_KEY, DB_APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
        mDBApi.getSession().startOAuth2Authentication(UploadActivity.this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
    }

    public void onUploadClick(View view) {
        new UploadPhotoTask().execute(mCurrentPhotoPath);
    }

    private class UploadPhotoTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... path) {
            File file = new File(path[0]);
            try {
                FileInputStream inputStream = new FileInputStream(file);
                mDBApi.putFile("/" + file.getName(), inputStream, file.length(), null, null);
            } catch (Exception e) {
                //For some reason, I keep getting ENOENT when trying to open file.
                Log.e("PhotoUpload", e.getMessage());
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            finish();
        }
    }
}
