package at.postd;

import java.io.File;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
		String fileName = "new-photo-name.jpg";
		//create parameters for Intent with filename
		ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.TITLE, fileName);
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
//	public void executeMultipartPost() throws Exception {
//		try {
//			ByteArrayOutputStream bos = new ByteArrayOutputStream();
//			bm.compress(CompressFormat.JPEG, 75, bos);
//			byte[] data = bos.toByteArray();
//			HttpClient httpClient = new DefaultHttpClient();
//			HttpPost postRequest = new HttpPost(
//					"http://10.0.2.2/cfc/iphoneWebservice.cfc?returnformat=json&amp;method=testUpload");
//			ByteArrayBody bab = new ByteArrayBody(data, "forest.jpg");
//			// File file= new File("/mnt/sdcard/forest.png");
//			// FileBody bin = new FileBody(file);
//			MultipartEntity reqEntity = new MultipartEntity(
//					HttpMultipartMode.BROWSER_COMPATIBLE);
//			reqEntity.addPart("uploaded", bab);
//			reqEntity.addPart("photoCaption", new StringBody("sfsdfsdf"));
//			postRequest.setEntity(reqEntity);
//			HttpResponse response = httpClient.execute(postRequest);
//			BufferedReader reader = new BufferedReader(new InputStreamReader(
//					response.getEntity().getContent(), "UTF-8"));
//			String sResponse;
//			StringBuilder s = new StringBuilder();
//
//			while ((sResponse = reader.readLine()) != null) {
//				s = s.append(sResponse);
//			}
//			System.out.println("Response: " + s);
//		} catch (Exception e) {
//			// handle exception here
//			Log.e(e.getClass().getName(), e.getMessage());
//		}
//	}
//
//	
//}