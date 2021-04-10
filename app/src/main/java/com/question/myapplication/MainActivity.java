package com.question.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();

    private static final int CAMERA_IMAGE_REQUEST = 1;
    private static final int PICK_IMAGE_REQUEST = 1;

    private int DEFAULT_MAX_WIDTH = 612;
    private int DEFAULT_MAX_HEIGHT = 816;
    private int DEFAULT_ORIENTATION = 0;

    private static final int READ_EXTERNAL_STORAGE_PERMISSION_REQUEST = 6;

    private ImageView mImageProfile;
    private Button mButtonUploadProfilePic;
    private Uri mCameraImageUri;
    private String mImageEncoding;
    private String mNewFileName;
    private TextView mTextStatus;
    private boolean mPhotoLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageProfile = (ImageView) findViewById(R.id.image_profile_pic);
        mButtonUploadProfilePic = (Button) findViewById(R.id.button_upload_profile_pic);

        mButtonUploadProfilePic.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                startGalleryActivity();
                            } else {
                                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION_REQUEST);
                            }
                        } else {
                            startGalleryActivity();

                        }
                    }
                }
        );
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        Log.i(TAG, "onRequestPermissionsResult: requestCode: " +requestCode);
        if (requestCode == 5) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraActivity();
            } else {
                Toast.makeText(this, "CAMERA Permission not granted", Toast.LENGTH_SHORT).show();
            }
        }else if (requestCode == 6) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startGalleryActivity();
            } else {
                Toast.makeText(this, "GALLERY Permission not granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCameraActivity(){
        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.TITLE, "Profile Image");
        mCameraImageUri = this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra("android.intent.extras.CAMERA_FACING", 1);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraImageUri);
        startActivityForResult(cameraIntent, CAMERA_IMAGE_REQUEST);
    }

    private void startGalleryActivity(){
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("onActivityResult: ", String.valueOf(requestCode));

        if (resultCode == this.RESULT_OK) {
            if (requestCode == CAMERA_IMAGE_REQUEST){

                String physicalPath = extractPhysicalPathFromUri(mCameraImageUri);
                Log.i(TAG, "onActivityResult: " + physicalPath);
                File finalFile = new File(physicalPath);
                Bitmap photo =null;
                try {
                    photo=compressBitmap(finalFile, DEFAULT_MAX_HEIGHT, DEFAULT_MAX_WIDTH,DEFAULT_ORIENTATION);;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mImageEncoding = getImageEncoding(photo);
                Random random = new Random();
                int newFileNamePrefix = random.nextInt(80 - 65) + 65;
                mNewFileName = String.valueOf(newFileNamePrefix) + ".png";

                mImageProfile.setImageBitmap(photo);
                mPhotoLoaded = true;
                mTextStatus.setError(null);
                // CallUpdateProfilepicDetails(fileBinaryData,strProPic);
            } else if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {

                String picturePath = null;
                Bitmap photo =null;
                Uri selectedImage = data.getData();
                if (Build.VERSION.SDK_INT >= 19) {
                    if (selectedImage != null && !selectedImage.toString().equals("null")) {
                        System.out.println("greater 19:" + "kitkat");
                        picturePath = getImagePath(selectedImage);
                        System.out.println("mSelectedFissslssePath res" + picturePath);

                    } else {
                        System.out.println("greater 19:" + "not kitkat");
                        picturePath = getImagePath(selectedImage);
                        System.out.println("mSelectedFissslePath res" + picturePath);
                    }
                }
                File finalFile = new File(picturePath);
                try {
                    photo= compressBitmap(finalFile, DEFAULT_MAX_HEIGHT, DEFAULT_MAX_WIDTH,DEFAULT_ORIENTATION);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Converting Bitmap to String
                mImageEncoding = getImageEncoding(photo);
                mNewFileName = finalFile.getName();
                mImageProfile.setImageBitmap(photo);

                mPhotoLoaded = true;
                mTextStatus.setError(null);

            } else {
                Toast.makeText(this, R.string.error_uploading_profile_pic, Toast.LENGTH_SHORT).show();
            }

        }
    }

    public String extractPhysicalPathFromUri(Uri contentUri) {
        try {
            String[] proj = {
                    MediaStore.Images.Media.DATA
            };
            Cursor cursor = this.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (Exception e) {
            return contentUri.getPath();
        }
    }

    public static Bitmap compressBitmap(File imageFile, int maxHeight, int maxWidth, int newOrientation)
            throws IOException {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        //Calculating Sample Size
        options.inSampleSize = calculateSampleSize(options, maxHeight, maxWidth);

        options.inJustDecodeBounds = false;

        Bitmap scaledBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

        ExifInterface exifInterface;
        exifInterface = new ExifInterface(imageFile.getAbsolutePath());
        int currentOrientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
        Matrix matrix = new Matrix();
        if (currentOrientation == 6) {
            matrix.postRotate(90);
        } else if (currentOrientation == 3) {
            matrix.postRotate(180);
        } else if (currentOrientation == 8) {
            matrix.postRotate(270);
        }
        if(newOrientation>0){
            matrix.postRotate(newOrientation);
        }
        scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth()
                , scaledBitmap.getHeight(), matrix, true);
        return scaledBitmap;
    }

    private String getImageEncoding(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        Log.i("encodedImage", encodedImage);
        return encodedImage;
    }

    private static int calculateSampleSize(BitmapFactory.Options options, int reqHeight, int reqWidth) {

        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        int halfHeight = height / 2;
        int halfWidth = width / 2;

        if (height > reqHeight || width > reqWidth) {
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public String getImagePath(Uri uri) {
        String path = "";
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            cursor.moveToFirst();
            String document_id = cursor.getString(0);
            document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
            cursor.close();

            cursor = getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
            cursor.moveToFirst();
            path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            System.out.println("path111:" + path);
            cursor.close();
        } catch (Exception e) {

        }
        return path;
    }
}