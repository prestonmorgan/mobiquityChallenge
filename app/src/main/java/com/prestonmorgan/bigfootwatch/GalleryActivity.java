package com.prestonmorgan.bigfootwatch;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class GalleryActivity extends ActionBarActivity {

    final static private String DB_APP_KEY = "iu6vsm9jiz8xpnl";
    final static private String DB_APP_SECRET = "z97yoyo67gtgje2";
    final static public String EXTRA_PHOTO_BMP = "com.mycompany.myfirstapp.PHOTO_BMP";
    private DropboxAPI<AndroidAuthSession> mDBApi;
    private ArrayList<String> filenames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        AppKeyPair appKeys = new AppKeyPair(DB_APP_KEY, DB_APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
        mDBApi.getSession().startOAuth2Authentication(GalleryActivity.this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gallery, menu);
        return true;
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
                new GetGalleryTask().execute();
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
    }

    public void setupListView() {
        ArrayAdapter<String> itemsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, filenames);
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(itemsAdapter);
        listView.setClickable(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                getPhoto(filenames.get(position));
            }
        });
    }

    private void getPhoto(String filename) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File cacheDir = this.getCacheDir();
            File imageFile = File.createTempFile(timeStamp, ".jpg", cacheDir);
            new GetPhotoTask().execute(imageFile.getAbsolutePath(), filename);
        } catch (IOException ex) {
            Log.e("ImageCreation", ex.getMessage());
        }
    }

    private void showPhoto(String filepath) {
        Intent intent = new Intent(this, PhotoActivity.class);
        File toDelete = new File(filepath);
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(filepath, bmOptions);
        bitmap = scaleDownBitmap(bitmap, 200);
        intent.putExtra(EXTRA_PHOTO_BMP, bitmap);
        toDelete.delete();
        startActivity(intent);
    }

    // From http://stackoverflow.com/questions/3528735/failed-binder-transaction
    private Bitmap scaleDownBitmap(Bitmap photo, int newHeight) {
        int width = (int) (newHeight * photo.getWidth() / ((double) photo.getHeight()));
        photo = Bitmap.createScaledBitmap(photo, width, newHeight, true);
        return photo;
    }

    private class GetPhotoTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... inputs) {
            FileOutputStream outputStream = null;
            String filepath = inputs[0];
            try {
                File file = new File(filepath);
                outputStream = new FileOutputStream(file);
                mDBApi.getFile("/" + inputs[1], null, outputStream, null);
            } catch (Exception e) {
                Log.e("GetPhoto", "Error getting the photo",  e);
                filepath = "";
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {}
                }
            }
            return filepath;
        }

        protected void onPostExecute(String filepath) {
            if (filepath.isEmpty()) {
                Context context = getApplicationContext();
                CharSequence text;
                text = "Could not connect to image";
                int duration = Toast.LENGTH_SHORT;
                Toast.makeText(context, text, duration).show();
            } else {
                showPhoto(filepath);
            }
        }
    }

    private class GetGalleryTask extends AsyncTask<Void, Void, Boolean> {
        protected Boolean doInBackground(Void... input) {
            try {
                DropboxAPI.Entry directory = mDBApi.metadata("/", 0, null, true, null);
                int length = directory.contents.size();
                filenames = new ArrayList<String>(length);
                for (int i = 0; i < length; ++i) {
                    filenames.add(directory.contents.get(i).fileName());
                }
                return true;
            } catch (DropboxException e) {
                Log.e("GalleryRetrieval", "Error getting the directory", e);
                return false;
            }
        }

        protected void onPostExecute(Boolean success) {
            if (!success) {
                Context context = getApplicationContext();
                CharSequence text;
                text = "Could not connect to gallery";
                int duration = Toast.LENGTH_SHORT;
                Toast.makeText(context, text, duration).show();
                finish();
            } else {
                setupListView();
            }
        }
    }
}
