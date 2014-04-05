package com.speed.basiccamera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class CameraActivity extends Activity {
	
	private static final String TAG = "CameraActivity";
	public static final String NAME_FILE = "temp.jpg";
	
	/** PreView Photo Elements*/
	private RelativeLayout mRlPreviewPhoto;
	private ImageView mIvPreviewPhoto;
	private ImageButton mIbCancel;
	private ImageButton mIbRepick;
	private ImageButton mIbOk;
	
	/** Camera Elements*/
	private RelativeLayout mRlCameraView;
	private RelativeLayout mRlCamera;
	private Camera mCamera;
    private CameraPreview mPreview;
    private ImageButton mBtTakePhoto;
    
    /** Data Elements*/
    private boolean inPreview = false;
    private Bitmap mBitmap = null;
    private OrientationEventListener mOrientationListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_camera);
        
        // Create an instance of Camera
        mCamera = getCameraInstance();
        
        //Init 
        initViewElements ();
        initCameraParams ();
        initListeners();
    }
    
    /**
     * Init view elements
     */
    private void initViewElements () {
    	// Create our Preview view and set it as the content of our activity.
    	mRlCameraView = (RelativeLayout) findViewById(R.id.rlCameraView);
        mPreview = new CameraPreview(this, mCamera);
        mRlCamera = (RelativeLayout) findViewById(R.id.rlCamera);
        mBtTakePhoto = (ImageButton) findViewById(R.id.btTakePhoto);
        
        mRlCamera.addView(mPreview);
        
        mRlPreviewPhoto = (RelativeLayout) findViewById(R.id.rlPreviewPhoto);
        mIbCancel = (ImageButton) findViewById(R.id.ibCancel);
        mIbRepick = (ImageButton) findViewById(R.id.ibRepick);
        mIbOk = (ImageButton) findViewById(R.id.ibOk);
        mIvPreviewPhoto = (ImageView) findViewById(R.id.ivPreviewPhoto);
    }
    
    /**
     * Init Camera params
     */
    private void initCameraParams () {
    	// get Camera parameters
    	Camera.Parameters params = mCamera.getParameters();
    	
    	List<String> focusModes = params.getSupportedFocusModes();
    	if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
    		// Autofocus mode is supported
    		params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
    	}
    	
    	// set size of camera object
    	List<Size> sizes = params.getSupportedPictureSizes();
    	Camera.Size size = sizes.get(0);
    	for(int i=0;i<sizes.size();i++)
    	{
    	    if(sizes.get(i).width > size.width)
    	        size = sizes.get(i);
    	}
    	params.setPictureSize(size.width, size.height);
    	
    	//Camera orientation
    	int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 90; break;
            case Surface.ROTATION_90: degrees = 0; break;
            case Surface.ROTATION_180: degrees = 90; break;
            case Surface.ROTATION_270: degrees = 180; break;
        }

        mCamera.setDisplayOrientation(degrees);
    	
        //Camera quality
    	params.setPictureFormat(ImageFormat.JPEG);
    	params.setJpegQuality(100); //1-100 with 100 as the best quality
    	
    	mCamera.setParameters(params);
    }
    
    /**
     * Init listeners for this activity
     */
    private void initListeners () {
    	mBtTakePhoto.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
				doPhoto();
	        }
    	});
    	
    	mBtTakePhoto.setOnLongClickListener(new View.OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				mBtTakePhoto.setEnabled(false);
				try {
					mCamera.autoFocus(autoFocusCallback);
				}catch (Exception e) {
					if (BuildOptions.DEBUG) {
						Log.d(TAG, "Error: " + e);
					}
				}
				return false;
			}
		});
    	
    	mPreview.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mBtTakePhoto.setEnabled(false);
				try {
					mCamera.autoFocus(autoFocusCallback);
				}catch (Exception e) {
					if (BuildOptions.DEBUG) {
						Log.d(TAG, "Error: " + e);
					}
				}
			}
		});
    	
    	mIbCancel.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				inPreview = false;
				onBack();
			}
		});
    	
    	mIbOk.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				writeBitmapInCacheDir();
				finish();
			}
		});
    	
    	mIbRepick.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				hidePreviewPhoto();
				showCamera();
				if (mBitmap != null) {
					mBitmap.recycle();
				}
			}
		});
    	
    	mOrientationListener = new OrientationEventListener(CameraActivity.this) {
            @Override
            public void onOrientationChanged(int orientation) {
            	initCameraParams();
            }
        };
        
        mOrientationListener.enable();
    }
    
    /**
     * Write bitmap into a file in Cache dir
     */
    private void writeBitmapInCacheDir () {
    	//write image into a file
    	File image = new File (getCacheDir().getAbsolutePath(), NAME_FILE);
		try {
			FileOutputStream fos = new FileOutputStream(image);
			mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
			fos.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}  catch (IOException e) {
			e.printStackTrace();
		}
		
		//send result
		setResult(RESULT_OK, new Intent("inline-data").putExtra("data", Uri.fromFile(image)));
		//clear the bitmap
		if (mBitmap != null) {
			mBitmap.recycle();
		}
    }
    
    /**
     * Callback for auto focus
     */
    private AutoFocusCallback autoFocusCallback = new AutoFocusCallback(){

    	  @Override
    	  public void onAutoFocus(boolean arg0, Camera arg1) {
    		  mBtTakePhoto.setEnabled(true);
    }};
    
	 /**
	  * Callback for getting the picture from camera
	  */
	 private PictureCallback pictureCallback = new PictureCallback() {

		    @Override
		    public void onPictureTaken(byte[] data, Camera camera) {
	
		    	mCamera.startPreview();
		    	if (BuildOptions.DEBUG) {
		    		Log.d(TAG, "Taking photo");
		    	}
		        onPictureTake(data);
		        hideCamera();
				showPreviewPhoto();
		    }
	 };
	 
	 /**
	  * Make a photo
	  */
	 private void doPhoto () {
			
 	    // get an image from the camera
         mCamera.takePicture(null, null, pictureCallback);
	 }
	 
	 /**
	  * Picture taken from camera
	  * @param data - picture
	  */
	 private void onPictureTake(byte[] data) {

		mBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
		
		//rotate image to landscape
		int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 90; break;
            case Surface.ROTATION_90: degrees = 0; break;
            case Surface.ROTATION_180: degrees = 90; break;
            case Surface.ROTATION_270: degrees = 180; break;
        }
        
		mBitmap = rotate(mBitmap, degrees);
		
		//if height or width it's more than 2048, the openGL will crash because the size of buffer it's more than 2048x2048
		int width = mBitmap.getWidth();
		int height = mBitmap.getHeight();
		if (width > 2048) {
			width = 2048;
		}
		if (height > 2048) {
			height = 2048;
		}
		//generate scaled image to 2048x2048
		mBitmap = generateProportionalScaledBitmapInPixelsFromFile(mBitmap, width, height);
	 }
	 
	 @Override
	 public void onResume() {
	    super.onResume();
	    try {
	    	startCamera();
	    	mOrientationListener.enable();
	    }catch (Exception e) {
	    	finish();
	    }
	 }

	 @Override
	 protected void onPause() {
		 super.onPause();
		 mOrientationListener.disable();
	 }
	 
	 /**
	  * Start the camera
	  */
	 private void startCamera ()  {
	     mCamera.startPreview();        // release the camera for other applications
	 }
    
    /**
     * Get Camera instance
     * @return current camera
     */
	public static Camera getCameraInstance(){
	    Camera c = null;
	    try {
	        c = Camera.open(); // attempt to get a Camera instance
	    }
	    catch (Exception e){
	        // Camera is not available (in use or does not exist)
	    	if (BuildOptions.DEBUG) {
	    		Log.d(TAG, "Error: " + e);
	    	}
	    }
	    return c; // returns null if camera is unavailable
	}
	
	/**
	 * Show Camera
	 */
	private void showCamera () {
		mRlCameraView.setVisibility(View.VISIBLE);
	}
	
	/**
	 * Hide Camera
	 */
	private void hideCamera () {
		mRlCameraView.setVisibility(View.GONE);
	}
	
	/**
	 * Show Preview Photo viewer
	 */
	private void showPreviewPhoto () {
		mRlPreviewPhoto.setVisibility(View.VISIBLE);
		inPreview = true;
		if (mBitmap != null) {
			mIvPreviewPhoto.setImageBitmap(mBitmap);
		}
	}
	
	/**
	 * Hide Preview Photo viewer
	 */
	private void hidePreviewPhoto () {
		mRlPreviewPhoto.setVisibility(View.GONE);
		inPreview = false;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			onBack();
		}
		return super.onKeyDown(keyCode, event);
	}
  
	/**
	 * do back
	 */
	private void onBack() {
      	
      	if (inPreview) {
      		inPreview = false;
      		hidePreviewPhoto();
      		showCamera();
      	}else {
      		if (mBitmap != null) {
      			mBitmap.recycle();
      		}
      		finish();
      	}
	}
	
	/**
	 * Rotate an bitmap
	 * @param b
	 * @param degrees
	 * @return
	 */
	private static Bitmap rotate(Bitmap b, int degrees) {
	    if (degrees != 0 && b != null) {
	        Matrix m = new Matrix();

	        m.setRotate(degrees, (float) b.getWidth() / 2, (float) b.getHeight() / 2);
	        try {
	            Bitmap b2 = Bitmap.createBitmap(
	                    b, 0, 0, b.getWidth(), b.getHeight(), m, true);
	            if (b != b2) {
	                b.recycle();
	                b = b2;
	            }
	        } catch (OutOfMemoryError ex) {
	        	//TODO
	           throw ex;
	        }
	    }
	    return b;
	}
	
	/**
	 * Generate a scaled bitmap
	 * @param tempBitmap
	 * @param widthInPixels
	 * @param heightInPixels
	 * @return
	 */
	public static Bitmap generateProportionalScaledBitmapInPixelsFromFile(Bitmap tempBitmap, int widthInPixels, int heightInPixels) {
        if (BuildOptions.DEBUG) { Log.d(TAG, "---------- Generating Scaled Bitmap ---------"); }
        if (BuildOptions.DEBUG) { Log.d(TAG, "- Original WidthInPixels -> " + tempBitmap.getWidth()); }
        if (BuildOptions.DEBUG) { Log.d(TAG, "- Original HeigthInPixels -> " + tempBitmap.getHeight()); }
        if (BuildOptions.DEBUG) { Log.d(TAG, "- AspectRatio -> " + ((float) tempBitmap.getWidth() / (float) tempBitmap.getHeight())); }

        //Proportional resize of the image
        double resizeProportionWidth = (float) widthInPixels / (float) tempBitmap.getWidth();
        double resizeProportionHeight = (float) heightInPixels / (float) tempBitmap.getHeight();
        
        if (BuildOptions.DEBUG) { Log.d(TAG, "----- Output Bitmap"); }
        try {
	        if (resizeProportionWidth < resizeProportionHeight) {
	        	if (BuildOptions.DEBUG) { Log.d(TAG, "- Generating bitmap of " + widthInPixels + "x" + (int) (tempBitmap.getHeight() * resizeProportionWidth)); }
		        if (BuildOptions.DEBUG) { Log.d(TAG, "- AspectRatio -> " + (widthInPixels / (tempBitmap.getHeight() * resizeProportionWidth))); }
	            return Bitmap.createScaledBitmap(tempBitmap, widthInPixels, (int) (tempBitmap.getHeight() * resizeProportionWidth), true);
	        } else {
	        	if (BuildOptions.DEBUG) { Log.d(TAG, "- Generating bitmap of " + (int) (tempBitmap.getWidth() * resizeProportionHeight) + "x" + heightInPixels); }
		        if (BuildOptions.DEBUG) { Log.d(TAG, "- AspectRatio -> " + ((tempBitmap.getWidth() * resizeProportionHeight) / heightInPixels)); }
	            return Bitmap.createScaledBitmap(tempBitmap, (int) (tempBitmap.getWidth() * resizeProportionHeight), heightInPixels, true);
	        }
	        
        }catch (OutOfMemoryError e) {
    	   return null; //Return null and control this exception in the parent view
        }
    }
}