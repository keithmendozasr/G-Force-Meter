package info.homepluspower.gforcemeter;

import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class CalcThread extends Thread
{
	public static final int CALIBRATE			= 0;
	public static final int SENSOR_STOP			= 1;
	public static final int GRAVITY_CHANGE		= 2;
	
	private class MyHandler extends Handler
	{
//		private float calcGVal(float value)
//		{
//			Log.d(TAG, String.format("Value of value: %+f", value));
//			
//			float GRAVITY = SensorManager.GRAVITY_EARTH;
//			float curGVal = value;
//			curGVal /= GRAVITY;
//			Log.d(TAG, String.format("Value of curGVal: %+f", curGVal));
//			
//			return curGVal;
//		}
		
		public void handleMessage(Message msg)
		{	
			switch(msg.what)
			{
				case CALIBRATE:
				{
					Log.d(TAG, "Calibrating");
					mHandler.removeMessages(GRAVITY_CHANGE);
					
					gOffset = (Float)msg.obj - SensorManager.GRAVITY_EARTH;
					lastGVal = minGVal = maxGVal = (float) 1.0;
					updateTextViews();
					break;
				}	
				
				case GRAVITY_CHANGE:
				{
					mHandler.removeMessages(GRAVITY_CHANGE);
					
					Log.d(TAG, String.format("Value of msg.obj: %+f", (Float)msg.obj));
					Log.d(TAG, String.format("Offset: %+.2f", gOffset));
				
					float curGVal = ((Float)msg.obj - gOffset) / SensorManager.GRAVITY_EARTH;
					
					Log.d(TAG, String.format("Value of curGVal after offset: %.2f", curGVal));
					Log.d(TAG, String.format("Value of lastGVal: %+.2f", lastGVal));
					
					if(curGVal >= (lastGVal - 0.01) || curGVal <= (lastGVal + 0.01))
					{
						lastGVal = curGVal;
						Log.d(TAG, "Updating sensor data");
						
						minGVal = (minGVal > lastGVal) ? lastGVal : minGVal;
						maxGVal = (maxGVal < lastGVal) ? lastGVal : maxGVal;
						updateTextViews();
					}
					else
						Log.v(TAG, "Difference not enough");
					
					break;
				}	
				
				case SENSOR_STOP:
				{
					Log.d(TAG, "Sensor stopping");
					Looper.myLooper().quit();
					mHandler = null;
					break;
				}	
				default:
				{
					Log.e(TAG, "Got unknown message type " + Integer.toString(msg.what));
					break;
				}
			}
		}
	}
	
	private float minGVal, maxGVal, lastGVal;
	private float gOffset;
	
	private Handler uiHandler;
	private static Handler mHandler;
	
	private String TAG;
	
	CalcThread(String TAG, Handler uiHandler)
	{
		super("CalcThread");
		this.uiHandler = uiHandler;
		this.TAG = TAG;
	}
	
	public Handler getHandler()
	{
		return mHandler;
	}
	
	private void updateTextViews()
	{
		Message m = uiHandler.obtainMessage();
		String values[] = { 
				String.format("%+.2f", lastGVal), 
				String.format("%+.2f", minGVal),
				String.format("%+.2f", maxGVal)
			};
		m.obj = (Object)values;
		m.sendToTarget();
	}
	
	public void run() 
	{
		// TODO Auto-generated method stub
		Looper.prepare();
		Log.v(TAG, "Starting CalcThread");
		mHandler = new MyHandler();
		Looper.loop();
		Log.v(TAG, "Past Looper.loop()");
	}
}
