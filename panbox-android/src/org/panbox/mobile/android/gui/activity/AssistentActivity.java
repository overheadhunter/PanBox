/*
 * 
 *               Panbox - encryption for cloud storage 
 *      Copyright (C) 2014-2015 by Fraunhofer SIT and Sirrix AG 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additonally, third party code may be provided with notices and open source
 * licenses from communities and third parties that govern the use of those
 * portions, and any licenses granted hereunder do not alter any rights and
 * obligations you may have under such open source licenses, however, the
 * disclaimer of warranty and limitation of liability provisions of the GPLv3 
 * will apply to all the product.
 * 
 */
package org.panbox.mobile.android.gui.activity;

import java.io.IOException;
import java.util.List;

import org.panbox.mobile.android.R;
import org.panbox.mobile.android.gui.webcam.CVImageProcessor;
import org.panbox.mobile.android.gui.webcam.QRCodeListener;
import org.panbox.mobile.android.gui.webcam.QRCodeProcessor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class AssistentActivity extends Activity implements OnClickListener, QRCodeListener {

	
	public static int counter = 0;
	@Override
	protected void onStart() {
		Log.v("AssistantActivity", "in onStart()");
		super.onStart();
	}

	@Override
	protected void onResume() {
		Log.v("AssistantActivity", "in onResume()");
		imageProcessor.addSquareCodeListener(AssistentActivity.this);	// set the listener that will process the scanning result
		super.onResume();
	}

	@Override
	protected void onPause() {
		Log.v("AssistantActivity", "in onPause()");
		super.onPause();
	}

	@Override
	protected void onStop() {
		Log.v("AssistantActivity", "in onStop()");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.v("AssistantActivity", "in onDestroy()");
		super.onDestroy();
	}

	private ImageProcessingView processor;
	private Preview mPreview;
	private QRCodeProcessor imageProcessor = null;
	private FixedAspectLayout layout;
	private Button btnPairing;
	
	
	@Override 
	protected void onCreate(Bundle savedInstanceState) {
		Log.v("Assistant Activity", "in onCreate()");
		super.onCreate(savedInstanceState);
		Log.v("Counter", " " + counter);
		setContentView(R.layout.pb_scan_assistent);
		
		getActionBar().hide();
		
		btnPairing = (Button)findViewById(R.id.man_pairing_button);
		
		btnPairing.setOnClickListener(this);
	
		
		imageProcessor = new QRCodeProcessor();

		// Create our Preview view and set it as the content of our activity.
		try {
			layout = (FixedAspectLayout)findViewById(R.id.qr_scanner);
			processor = new ImageProcessingView(this, imageProcessor);
			mPreview = new Preview(this, processor);
			layout.addView(mPreview);
			layout.addView(processor);
			
		} catch (IOException e) {
			e.printStackTrace();
			new AlertDialog.Builder(this).setMessage(e.getMessage()).create()
					.show();
		}
		
		//===================================Start testing code (this method must be placed in the qrCodeDetected() method=================================================
		
		//String result = "192.168.1.12:fgkd343alM";
		
		//runGeneralPairingRequester(result);
		//==================================End testing code==================================================

	}
	
	@Override
	public void qrCodeDetected(String pairingPassword) {
		counter++;
		Log.v("AssistantActivity:", "in qrCodeDetected(), code was detected " + counter + " times");
		
			pairingPassword = pairingPassword.trim();
			if (pairingPassword.matches("^[0-9A-Za-z.]+:[A-Z0-9a-z+/]+={0,2}$")) {
				Log.v("Assistant Activity",
						"Scanned valid QR code! Will start the pairing!!!");
				imageProcessor.removeSquareCodeListener(AssistentActivity.this); 	// need to deregister the listener, 
																					// otherwise this listener gets notified multiple times, 
																					// and qrCodeDetected() method is invoked also multiple times
				Intent pairingExecutionActivity = new Intent(this,
						PairingExecutionActivity.class);
				pairingExecutionActivity.putExtra("pairingPassword",
						pairingPassword);
				startActivity(pairingExecutionActivity);
			} else {	
				Toast invalidQr = Toast.makeText(getApplicationContext(),
						getIntent().getExtras().getString("pb_pairing_error_occurred"),
						Toast.LENGTH_SHORT);
				invalidQr.show();
			}
	}
	
	
	@Override
	public void onClick(View v) {
		
		Intent pairingActivity = new Intent(this, PairingActivity.class);
		
		startActivity(pairingActivity);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		Log.v("AssistantActivity:", "in onNewIntent()");
		super.onNewIntent(intent);
		
		imageProcessor.addSquareCodeListener(AssistentActivity.this);	// set the listener that will process the scanning result
	}
	
}

	
//----------------------------------------------------------------------

class ImageProcessingView extends View implements Camera.PreviewCallback {
	public static final int SUBSAMPLING_FACTOR = 4;

	private CVImageProcessor imageProcessor;

	public ImageProcessingView(AssistentActivity context,
			CVImageProcessor processor) throws IOException {
		super(context);
		this.imageProcessor = processor;
	}

	public void onPreviewFrame(final byte[] data, final Camera camera) {
		try {
			Camera.Size size = camera.getParameters().getPreviewSize();
			processImage(data, size.width, size.height);
			camera.addCallbackBuffer(data);
		} catch (RuntimeException e) {
			// The camera has probably just been released, ignore.
		}
	}

	protected void processImage(byte[] data, int width, int height) {

		int[] argbData = convertYUV420_NV21toARGB8888(data, width, height);
		
		Bitmap bitmap = Bitmap.createBitmap(argbData, width, height, Bitmap.Config.ARGB_8888);

		if (imageProcessor != null) {
			imageProcessor.process(bitmap);
		}

		postInvalidate();
	}

	public static int[] convertYUV420_NV21toARGB8888(byte[] data, int width,
			int height) {
		int size = width * height;
		int offset = size;
		int[] pixels = new int[size];
		int u, v, y1, y2, y3, y4;

		// i along Y and the final pixels
		// k along pixels U and V
		for (int i = 0, k = 0; i < size; i += 2, k += 2) {
			y1 = data[i] & 0xff;
			y2 = data[i + 1] & 0xff;
			y3 = data[width + i] & 0xff;
			y4 = data[width + i + 1] & 0xff;

			u = data[offset + k] & 0xff;
			v = data[offset + k + 1] & 0xff;
			u = u - 128;
			v = v - 128;

			pixels[i] = convertYUVtoARGB(y1, u, v);
			pixels[i + 1] = convertYUVtoARGB(y2, u, v);
			pixels[width + i] = convertYUVtoARGB(y3, u, v);
			pixels[width + i + 1] = convertYUVtoARGB(y4, u, v);

			if (i != 0 && (i + 2) % width == 0)
				i += width;
		}

		return pixels;
	}

	private static int convertYUVtoARGB(int y, int u, int v) {
		int r, g, b;

		r = y + (int) 1.402f * u;
		g = y - (int) (0.344f * v + 0.714f * u);
		b = y + (int) 1.772f * v;
		r = r > 255 ? 255 : r < 0 ? 0 : r;
		g = g > 255 ? 255 : g < 0 ? 0 : g;
		b = b > 255 ? 255 : b < 0 ? 0 : b;
		return 0xff000000 | (r << 16) | (g << 8) | b;
	}

	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
		Paint paint = new Paint();
		paint.setColor(Color.RED);
		paint.setTextSize(30);

		//String s = "Please scan the QR Code by showing it to the camera.";
		//float textWidth = paint.measureText(s);
		//canvas.drawText(s, (getWidth() - textWidth) / 2, 30, paint);
	}
}

//----------------------------------------------------------------------

class Preview extends SurfaceView implements SurfaceHolder.Callback {
	SurfaceHolder mHolder;
	Camera mCamera;
	Camera.PreviewCallback previewCallback;

	@SuppressWarnings("deprecation")
	Preview(Context context, Camera.PreviewCallback previewCallback) {
		super(context);
		this.previewCallback = previewCallback;

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		mCamera = openFrontAndBackCamera();
		try {
			try {
				Camera.Parameters parameters = mCamera.getParameters();
				parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
				mCamera.setParameters(parameters);
			} catch (Exception e) {
				Log.v("AssistentActivity::surfaceCreated", "Failed to set focus auto. Will continue with just default settings.");
			}
			try {
				Camera.Parameters parameters = mCamera.getParameters();
				parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
				mCamera.setParameters(parameters);
			} catch (Exception e) {
				Log.v("AssistentActivity::surfaceCreated", "Failed to set flash off. Will continue with just default settings.");
			}
			mCamera.setPreviewDisplay(holder);
			// put the camera in a portrait mode
			mCamera.setDisplayOrientation(90);
		} catch (IOException exception) {
			mCamera.release();
			mCamera = null;
			// TODO: add more exception handling logic here
		}
	}

	private Camera openFrontAndBackCamera() {
		int numberOfCameras = Camera.getNumberOfCameras();
		if (numberOfCameras > 0) {
			return Camera.open(0); // TODO: Make this configurable!
		}
		return null;
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// Surface will be destroyed when we return, so stop the preview.
		// Because the CameraDevice object is not a shared resource, it's very
		// important to release it when the activity is paused.
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
	}

	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.05;
		double targetRatio = (double) w / h;
		if (sizes == null)
			return null;

		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		// Try to find an size match aspect ratio and size
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		// Cannot find the one match the aspect ratio, ignore the requirement
		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// Now that the size is known, set up the camera parameters and begin
		// the preview.
		Camera.Parameters parameters = mCamera.getParameters();

		List<Size> sizes = parameters.getSupportedPreviewSizes();
		Size optimalSize = getOptimalPreviewSize(sizes, w, h);
		//parameters.setPreviewSize(optimalSize.width, optimalSize.height);
		parameters.setPreviewSize(optimalSize.width, optimalSize.height);

		mCamera.setParameters(parameters);
		if (previewCallback != null) {
			mCamera.setPreviewCallbackWithBuffer(previewCallback);
			Camera.Size size = parameters.getPreviewSize();
			byte[] data = new byte[size.width
					* size.height
					* ImageFormat
							.getBitsPerPixel(parameters.getPreviewFormat()) / 8];
			mCamera.addCallbackBuffer(data);
		}
		mCamera.startPreview();
	}

}