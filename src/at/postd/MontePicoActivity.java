package at.postd;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MontePicoActivity extends Activity {
	private File imageFile;
	private Uri imageUri;
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static int RESULT_LOAD_IMAGE = 1;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
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
		}
		
		try {
			executeMultipartPost();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void executeMultipartPost() throws Exception {
		try {
	
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost postRequest = new HttpPost(
					"http://192.168.1.115:4567/upload");
			
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
}

//	private void doFileUpload(){
//		HttpURLConnection conn = null;
//		DataOutputStream dos = null;
//		DataInputStream inStream = null;
//		String lineEnd = "rn";
//		String twoHyphens = "--";
//		String boundary =  "*****";
//		int bytesRead, bytesAvailable, bufferSize;
//		byte[] buffer;
//		int maxBufferSize = 1*1024*1024;
//		String responseFromServer = "";
//		String urlString = "http://192.168.1.115:4567/upload";
//		try
//		{
//			//------------------ CLIENT REQUEST
//			FileInputStream fileInputStream = new FileInputStream(new File(imageFile.getAbsolutePath()));
//			// open a URL connection to the Servlet
//			URL url = new URL(urlString);
//			
//			// Open a HTTP connection to the URL
//			conn = (HttpURLConnection) url.openConnection();
//			// Allow Inputs
//			conn.setDoInput(true);
//			// Allow Outputs
//			conn.setDoOutput(true);
//			// Don't use a cached copy.
//			conn.setUseCaches(false);
//			// Use a post method.
//			conn.setRequestMethod("POST");
//			conn.setRequestProperty("Connection", "Keep-Alive");
//			conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
//			dos = new DataOutputStream( conn.getOutputStream() );
//			dos.writeBytes(twoHyphens + boundary + lineEnd);
//			dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + imageFile.getPath() + "\"" + lineEnd);
//			dos.writeBytes(lineEnd);
//			// create a buffer of maximum size
//			bytesAvailable = fileInputStream.available();
//			bufferSize = Math.min(bytesAvailable, maxBufferSize);
//			buffer = new byte[bufferSize];
//			// read file and write it into form...
//			bytesRead = fileInputStream.read(buffer, 0, bufferSize);
//			while (bytesRead > 0)
//			{
//				dos.write(buffer, 0, bufferSize);
//				bytesAvailable = fileInputStream.available();
//				bufferSize = Math.min(bytesAvailable, maxBufferSize);
//				bytesRead = fileInputStream.read(buffer, 0, bufferSize);
//			}
//			// send multipart form data necesssary after file data...
//			dos.writeBytes(lineEnd);
//			dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
//			// close streams
//			Log.e("Debug","File is written");
//			fileInputStream.close();
//			dos.flush();
//			dos.close();
//		}
//		catch (MalformedURLException ex)
//		{
//			Log.e("Debug", "error: " + ex.getMessage(), ex);
//		}
//		catch (IOException ioe)
//		{
//			Log.e("Debug", "error: " + ioe.getMessage(), ioe);
//		}
//		//------------------ read the SERVER RESPONSE
//		try {
//			inStream = new DataInputStream ( conn.getInputStream() );
//			String str;
//
//			while (( str = inStream.readLine()) != null)
//			{
//				Log.e("Debug","Server Response "+str);
//			}
//			inStream.close();
//
//		}
//		catch (IOException ioex){
//			Log.e("Debug", "error: " + ioex.getMessage(), ioex);
//		}
//	}
//}

//
//	private static final String MEDIA_TYPE_IMAGE = "jpg";
//	
//	private File imageFile;
//

//
//

//
//
//	public void sendPicture(View view) throws Exception {
//		create_upload(imageFile);
//	}
//
//	public void create_upload(File inputFile) {
//		HttpURLConnection connection = null;
//		DataOutputStream outputStream = null;
//		DataInputStream inputStream = null;
//
//		
//		String urlServer = "http://192.168.1.115:4567/upload";
//		String lineEnd = "\r\n";
//		String twoHyphens = "--";
//		String boundary =  "*****";
//
//		int bytesRead, bytesAvailable, bufferSize;
//		byte[] buffer;
//		int maxBufferSize = 1*1024*1024;
//
//		try
//		{
//			FileInputStream fileInputStream = new FileInputStream( inputFile );
//
//			URL url = new URL(urlServer);
//			connection = (HttpURLConnection) url.openConnection();
//
//			// Allow Inputs & Outputs
//			connection.setDoInput(true);
//			connection.setDoOutput(true);
//			connection.setUseCaches(false);
//
//			// Enable POST method
//			connection.setRequestMethod("POST");
//
//			connection.setRequestProperty("Connection", "Keep-Alive");
//			connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
//
//			outputStream = new DataOutputStream( connection.getOutputStream() );
//			outputStream.writeBytes(twoHyphens + boundary + lineEnd);
//			String pathToOurFile =  inputFile.getPath();
//			outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + pathToOurFile +"\"" + lineEnd);
//			outputStream.writeBytes(lineEnd);
//
//			bytesAvailable = fileInputStream.available();
//			bufferSize = Math.min(bytesAvailable, maxBufferSize);
//			buffer = new byte[bufferSize];
//
//			// Read file
//			bytesRead = fileInputStream.read(buffer, 0, bufferSize);
//
//			while (bytesRead > 0)
//			{
//				outputStream.write(buffer, 0, bufferSize);
//				bytesAvailable = fileInputStream.available();
//				bufferSize = Math.min(bytesAvailable, maxBufferSize);
//				bytesRead = fileInputStream.read(buffer, 0, bufferSize);
//			}
//
//			outputStream.writeBytes(lineEnd);
//			outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
//
//			// Responses from the server (code and message)
//			int serverResponseCode = connection.getResponseCode();
//			String serverResponseMessage = connection.getResponseMessage();
//
//			fileInputStream.close();
//			outputStream.flush();
//			outputStream.close();
//		} catch (Exception e) {
//			// TODO: handle exception
//			e.printStackTrace();
//		}
//
//	}
//	

//
//	
//}