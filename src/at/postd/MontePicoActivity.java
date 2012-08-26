package at.postd;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MontePicoActivity extends Activity {
	private File imageFile;
	private Uri imageUri;
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static int RESULT_LOAD_IMAGE = 1;
//	private static String API_URI = "http://192.168.1.115:4567";
		private static String API_URI = "http://an-ti.eu:4567";


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i("montepico","starting");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Button downloadRandomImageButton = (Button) findViewById(R.id.random);
		downloadRandomImageButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				downloadFile(API_URI+"/random");				
			}
		});

		Button buttonLoadImage = (Button) findViewById(R.id.filemanager);
		buttonLoadImage.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {

				Intent i = new Intent(
						Intent.ACTION_PICK,
						android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

				startActivityForResult(i, RESULT_LOAD_IMAGE);
			}
		});
	}

	
	Bitmap bmImg;

	void downloadFile(String fileUrl){
		URL myFileUrl =null; 
		
		try {
			myFileUrl= new URL(fileUrl);
			HttpURLConnection conn= (HttpURLConnection) myFileUrl.openConnection();
			conn.setDoInput(true);
			conn.connect();
			
			InputStream is = conn.getInputStream();

			bmImg = BitmapFactory.decodeStream(is);
			
			ImageView imageView = (ImageView) findViewById(R.id.imageView1);
							
			imageView.setImageBitmap(bmImg);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		} 
	}
	

	public void takePhoto(View view) {
		//define the file-name to save photo taken by Camera activity

		ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.TITLE, "MontePICO");
		values.put(MediaStore.Images.Media.DESCRIPTION,"Image capture by camera");
		//imageUri is the current activity attribute, define and save it for later usage (also in onSaveInstanceState)
		imageUri = getContentResolver().insert(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
		//create new Intent
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
		intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
		startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);

	}


	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				imageFile = convertImageUriToFile(imageUri, this);
				ImageView imageView = (ImageView) findViewById(R.id.imageView1);
				imageView.setImageBitmap(BitmapFactory.decodeFile(imageFile.getPath()));
				UploadPictureTask task = new UploadPictureTask();
				task.execute("");
			} else if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, "Picture was not taken", Toast.LENGTH_SHORT);
			} else {
				Toast.makeText(this, "Picture was not taken", Toast.LENGTH_SHORT);
			}
		}
		if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
			Uri selectedImage = data.getData();
			imageFile = convertImageUriToFile(selectedImage, this);

			String[] filePathColumn = { MediaStore.Images.Media.DATA };

			Cursor cursor = getContentResolver().query(selectedImage,
					filePathColumn, null, null, null);
			cursor.moveToFirst();

			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			String picturePath = cursor.getString(columnIndex);
			cursor.close();

			ImageView imageView = (ImageView) findViewById(R.id.imageView1);
			imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));

			UploadPictureTask task = new UploadPictureTask();
			task.execute("");
		}
	}


	public static File convertImageUriToFile (Uri imageUri, Activity activity)  {
		Cursor cursor = null;
		try {
			String [] proj={MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID, MediaStore.Images.ImageColumns.ORIENTATION};
			cursor = activity.managedQuery( imageUri,
					proj, // Which columns to return
					null,       // WHERE clause; which rows to return (all rows)
					null,       // WHERE clause selection arguments (none)
					null); // Order-by clause (ascending by name)
			int file_ColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			int orientation_ColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION);
			if (cursor.moveToFirst()) {
				String orientation =  cursor.getString(orientation_ColumnIndex);
				return new File(cursor.getString(file_ColumnIndex));
			}
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}


	private class UploadPictureTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... parms) {

			try {

				HttpClient httpClient = new DefaultHttpClient();
				HttpPost postRequest = new HttpPost(
						API_URI+"/upload");

				Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getPath());
				Bitmap bmpCompressed = Bitmap.createScaledBitmap(bitmap, 640, 480, true);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();

				// CompressFormat set up to JPG, you can change to PNG or whatever you want;

				bitmap.compress(CompressFormat.JPEG, 50, bos);

				byte[] data = bos.toByteArray();

				FileBody bin = new FileBody(imageFile);

				MultipartEntity reqEntity = new MultipartEntity(
						HttpMultipartMode.BROWSER_COMPATIBLE);

				reqEntity.addPart("file", new ByteArrayBody(data, imageFile.getName()));

				reqEntity.addPart("photoCaption", new StringBody("XXX MONTEPICO"));
				postRequest.setEntity(reqEntity);
				HttpResponse response = httpClient.execute(postRequest);
				BufferedReader reader = new BufferedReader(new InputStreamReader(
						response.getEntity().getContent(), "UTF-8"));
				String sResponse;
				StringBuilder s = new StringBuilder();

				while ((sResponse = reader.readLine()) != null) {
					s = s.append(sResponse);
				}
				System.out.println("Response: " + s);
			} catch (Exception e) {
				// handle exception here
				Log.e(e.getClass().getName(), e.getMessage());
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			Context context = getApplicationContext();	
			Toast.makeText(context,"Upload Successfull!", Toast.LENGTH_SHORT);				
		}
	}
}
