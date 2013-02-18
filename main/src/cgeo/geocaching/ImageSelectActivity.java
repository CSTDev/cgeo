package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageSelectActivity extends AbstractActivity {
    static final String EXTRAS_CAPTION = "caption";
    static final String EXTRAS_DESCRIPTION = "description";
    static final String EXTRAS_URI_AS_STRING = "uri";

    private static final String SAVED_STATE_IMAGE_CAPTION = "cgeo.geocaching.saved_state_image_caption";
    private static final String SAVED_STATE_IMAGE_DESCRIPTION = "cgeo.geocaching.saved_state_image_description";
    private static final String SAVED_STATE_IMAGE_URI = "cgeo.geocaching.saved_state_image_uri";

    private static final int SELECT_NEW_IMAGE = 1;
    private static final int SELECT_STORED_IMAGE = 2;

    private EditText captionView;
    private EditText descriptionView;

    // Data to be saved while reconfiguring
    private String imageCaption;
    private String imageDescription;
    private Uri imageUri;

    public ImageSelectActivity() {
        super("ImageSelectActivity");
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.visit_image);
        setTitle(res.getString(R.string.log_image));

        imageCaption = "";
        imageDescription = "";
        imageUri = Uri.EMPTY;

        // Get parameters from intent and basic cache information from database
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            imageCaption = extras.getString(EXTRAS_CAPTION);
            imageDescription = extras.getString(EXTRAS_DESCRIPTION);
            imageUri = Uri.parse(extras.getString(EXTRAS_URI_AS_STRING));
        }

        // Restore previous state
        if (savedInstanceState != null) {
            imageCaption = savedInstanceState.getString(SAVED_STATE_IMAGE_CAPTION);
            imageDescription = savedInstanceState.getString(SAVED_STATE_IMAGE_DESCRIPTION);
            imageUri = Uri.parse(savedInstanceState.getString(SAVED_STATE_IMAGE_URI));
        }

        final Button cameraButton = (Button) findViewById(R.id.camera);
        cameraButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                selectImageFromCamera();
            }
        });

        final Button storedButton = (Button) findViewById(R.id.stored);
        storedButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                selectImageFromStorage();
            }
        });

        captionView = (EditText) findViewById(R.id.caption);
        if (StringUtils.isNotBlank(imageCaption)) {
            captionView.setText(imageCaption);
        }

        descriptionView = (EditText) findViewById(R.id.description);
        if (StringUtils.isNotBlank(imageDescription)) {
            descriptionView.setText(imageDescription);
        }

        final Button saveButton = (Button) findViewById(R.id.save);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                saveImageInfo(true);
            }
        });

        final Button clearButton = (Button) findViewById(R.id.cancel);
        clearButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                saveImageInfo(false);
            }
        });

        loadImagePreview();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        currentEditTexts();
        outState.putString(SAVED_STATE_IMAGE_CAPTION, imageCaption);
        outState.putString(SAVED_STATE_IMAGE_DESCRIPTION, imageDescription);
        outState.putString(SAVED_STATE_IMAGE_URI, imageUri != null ? imageUri.getPath() : StringUtils.EMPTY);
    }

    public void saveImageInfo(boolean saveInfo) {
        if (saveInfo) {
            Intent intent = new Intent();
            currentEditTexts();
            intent.putExtra(EXTRAS_CAPTION, imageCaption);
            intent.putExtra(EXTRAS_DESCRIPTION, imageDescription);
            intent.putExtra(EXTRAS_URI_AS_STRING, imageUri.toString());

            setResult(RESULT_OK, intent);
        } else {
            setResult(RESULT_CANCELED);
        }

        finish();
    }

    private void currentEditTexts() {
        imageCaption = captionView.getText().toString();
        imageDescription = descriptionView.getText().toString();
    }

    private void selectImageFromCamera() {
        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        imageUri = getOutputImageFileUri(); // create a file to save the image
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri); // set the image file name

        // start the image capture Intent
        startActivityForResult(intent, SELECT_NEW_IMAGE);
    }

    private void selectImageFromStorage() {
        //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        //Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        //intent.addCategory(Intent.CATEGORY_OPENABLE);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/jpeg");

        startActivityForResult(Intent.createChooser(intent, "Select Image"), SELECT_STORED_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            // User cancelled the image capture
            showToast("Cancelled");
            return;
        }

        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_NEW_IMAGE) {

                // Image captured and saved to fileUri specified in the Intent
                //TODO: Some camera program ignore the URI passed in and store it in a location given in data
                showToast("Image saved to:\n" + imageUri);//data.getData());
                if (data != null) {
                    Log.d("SELECT_NEW_IMAGE data = " + data.toString());
                } else {
                    Log.d("SELECT_NEW_IMAGE data is null");
                }
            } else if (requestCode == SELECT_STORED_IMAGE) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaColumns.DATA };

                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String filePath = cursor.getString(columnIndex);
                imageUri = Uri.parse(filePath);
                cursor.close();
                if (data != null) {
                    Log.d("SELECT_STORED_IMAGE data = " + data.toString());
                } else {
                    Log.d("SELECT_STORED_IMAGE data is null");
                }
                //                imageUri = data.getData();
            }
        } else {
            // Image capture failed, advise user
            showToast("Unknown Error");
            return;
        }

        loadImagePreview();
    }

    private void loadImagePreview()
    {
        if (!new File(imageUri.getPath()).exists()) {
            Log.i("loading Image Preview with nonexistant file");
            return;
        }

        final ImageView imagePreview = (ImageView) findViewById(R.id.image_preview);
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = 8;
        final Bitmap bitmap = BitmapFactory.decodeFile(imageUri.getPath(), bitmapOptions);
        imagePreview.setImageBitmap(bitmap);
        imagePreview.setVisibility(View.VISIBLE);
    }

    private static Uri getOutputImageFileUri() {
        return Uri.fromFile(getOutputImageFile());
    }

    /** Create a File for saving an image or video */
    @SuppressLint("NewApi")
    // TODO: Get rid of that suppress
    private static File getOutputImageFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "cgeo");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.w("Failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
    }
}
