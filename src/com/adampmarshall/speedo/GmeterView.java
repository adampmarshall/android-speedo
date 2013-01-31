package com.adampmarshall.speedo;

import java.math.BigDecimal;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.ShapeDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.*;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class GmeterView extends SurfaceView implements SurfaceHolder.Callback {

	class GmeterThread extends Thread {

		public static final int STATE_PAUSE = 2;
		public static final int STATE_RUNNING = 4;
		private static final float GMETER_PIXELFACTOR = 20;

		/** Message handler used by thread to interact with TextView */
		private Handler mHandler;
		private SurfaceHolder mSurfaceHolder;
		private Context mContext;
		private int mMode;
		private boolean mRun;
		private int mCanvasWidth;
		private int mCanvasHeight;
		private Bitmap mBackgroundImage;
		private ShapeDrawable mBall;

		private SensorManager mSensorManager;
		private Sensor mAccelorometer;

		private Paint mLinePaint;
		private BigDecimal mXaxis = new BigDecimal(0.0);
		private BigDecimal mZaxis = new BigDecimal(0.0);

		public GmeterThread(SurfaceHolder surfaceHolder, Context context,
				SensorManager sensor) {

			mSurfaceHolder = surfaceHolder;
			mSensorManager = sensor;
			mContext = context;

			Resources res = context.getResources();

			// load background image as a Bitmap instead of a Drawable b/c
			// we don't need to transform it and it's faster to draw this way
			mBackgroundImage = BitmapFactory.decodeResource(res,
					R.drawable.background);

			mLinePaint = new Paint();
			mLinePaint.setAntiAlias(true);
			mLinePaint.setARGB(255, 0, 255, 0);

			mAccelorometer = mSensorManager
					.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
			mSensorManager.registerListener(seListener, mAccelorometer,
					SensorManager.SENSOR_DELAY_UI);

		}

		public void pause() {
			synchronized (mSurfaceHolder) {
				if (mMode == STATE_RUNNING)
					setState(STATE_PAUSE);
			}
		}

		public void setState(int mode) {
			synchronized (mSurfaceHolder) {
				setState(mode, null);
			}
		}

		public void setState(int mode, CharSequence message) {
			synchronized (mSurfaceHolder) {

				mSensorManager.unregisterListener(seListener);

			}
		}

		private void updateUISensorEvent(SensorEvent event) {

			mXaxis = new BigDecimal(event.values[0]);
			mZaxis = new BigDecimal(event.values[2]);

			mXaxis = mXaxis.setScale(3, BigDecimal.ROUND_HALF_UP);
			mZaxis = mZaxis.setScale(3, BigDecimal.ROUND_HALF_UP);

		};

		private final SensorEventListener seListener = new SensorEventListener() {

			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub

			}

			public void onSensorChanged(SensorEvent event) {
				// TODO Auto-generated method stub
				updateUISensorEvent(event);
			}

		};

		@Override
		public void run() {
			while (mRun) {
				Canvas c = null;
				try {
					if (mSurfaceHolder != null) {
						c = mSurfaceHolder.lockCanvas(null);
						synchronized (mSurfaceHolder) {
							if (mMode == STATE_RUNNING)
								updateBall();
							doDraw(c);
						}
					}
				} catch (Exception ex) {
					// do nothing just don't crash
				} finally {
					// do this in a finally so that if an exception is thrown
					// during the above, we don't leave the Surface in an
					// inconsistent state
					if (c != null) {
						mSurfaceHolder.unlockCanvasAndPost(c);
					}
				}
			}
		}

		private void doDraw(Canvas canvas) {
			// Draw the background image. Operations on the Canvas accumulate
			// so this is like clearing the screen.
			canvas.drawBitmap(mBackgroundImage, 0, 0, null);
			canvas.drawCircle(mCanvasWidth / 2
					+ (mXaxis.floatValue() * GMETER_PIXELFACTOR), mCanvasHeight
					/ 2 + (mZaxis.floatValue() * GMETER_PIXELFACTOR), 10,
					mLinePaint);

		}

		private void updateBall() {
			// TODO Auto-generated method stub

		}

		public void setRunning(boolean b) {
			mRun = b;

			// Get an instance of the SensorManager

		}

		/* Callback invoked when the surface dimensions change. */
		public void setSurfaceSize(int width, int height) {
			// synchronized to make sure these all change atomically
			synchronized (mSurfaceHolder) {
				mCanvasWidth = width;
				mCanvasHeight = height;

				// don't forget to resize the background image
				mBackgroundImage = Bitmap.createScaledBitmap(mBackgroundImage,
						width, height, true);
			}
		}

	}

	private GmeterThread thread;

	public GmeterView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// TODO Auto-generated constructor stub

		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		thread = new GmeterThread(
				holder,
				context,
				(SensorManager) context
						.getSystemService(android.content.Context.SENSOR_SERVICE));

		setFocusable(true); // make sure we get key events

	}

	public GmeterThread getThread() {
		return thread;
	}

	/**
	 * Standard window-focus override. Notice focus lost so we can pause on
	 * focus lost. e.g. user switches to take a call.
	 */
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		if (!hasWindowFocus)
			thread.pause();
	}

	/* Callback invoked when the surface dimensions change. */
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		thread.setSurfaceSize(width, height);
	}

	/*
	 * Callback invoked when the Surface has been created and is ready to be
	 * used.
	 */
	public void surfaceCreated(SurfaceHolder holder) {
		// start the thread here so that we don't busy-wait in run()
		// waiting for the surface to be created
		thread.setRunning(true);
		thread.start();
	}

	/*
	 * Callback invoked when the Surface has been destroyed and must no longer
	 * be touched. WARNING: after this method returns, the Surface/Canvas must
	 * never be touched again!
	 */
	public void surfaceDestroyed(SurfaceHolder holder) {
		// we have to tell thread to shut down & wait for it to finish, or else
		// it might touch the Surface after we return and explode
		boolean retry = true;
		thread.setRunning(false);
		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
	}
}
