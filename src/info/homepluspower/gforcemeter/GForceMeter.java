package info.homepluspower.gforcemeter;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;

public class GForceMeter extends Activity implements SensorEventListener, OnClickListener {
	private static CalcThread calcThread;
	
	private static TextView gCurTxt, gMinTxt, gMaxTxt;
	
	private static SensorManager sensorMgr = null;
	
	private static boolean calibrate = false;
	
	private Button calibrateButton, exitButton;
	
	private static String TAG;
	
	private float lastUpdate; 
	
	private class MyHandler extends Handler
	{
		public void handleMessage(Message msg)
		{
			String[] values = (String[])msg.obj;
			gCurTxt.setText(values[0]);
			gMinTxt.setText(values[1]);
			gMaxTxt.setText(values[2]);
		}
	}
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        TAG = getString(R.string.app_name);
    
        calibrateButton = (Button)findViewById(R.id.CalibrateButton);
        calibrateButton.setOnClickListener(this);
        
        exitButton = (Button)findViewById(R.id.ExitButton);
        exitButton.setOnClickListener(this);
        
        gCurTxt = (TextView)findViewById(R.id.gCurTxt);
        gMinTxt = (TextView)findViewById(R.id.gMinTxt);
        gMaxTxt = (TextView)findViewById(R.id.gMaxTxt);
        
        lastUpdate = 0;
        calibrate = true;
        
        Log.d(TAG, "onCreate done");
    }

	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
	}
	
	public void onSensorChanged(SensorEvent arg0) {
		// TODO Auto-generated method stub
		
		switch(arg0.sensor.getType())
		{
			case Sensor.TYPE_ACCELEROMETER:
			{
				float currTime = System.currentTimeMillis();
				if(currTime < (lastUpdate+500))
					return;
				
				lastUpdate = currTime;
				
				if(calcThread == null)
				{
					Log.d(TAG, "CalcThread not running");
					return;
				}
				
				Handler h = calcThread.getHandler();
				if(h == null)
				{
					Log.e(TAG, "Failed to get CalcThread Handler");
					return;
				}
				
				Message m = Message.obtain(h);
				if(m == null)
				{
					Log.e(TAG, "Failed to get Message instance");
					return;
				}
				
				m.obj = (Object)arg0.values[1];
				if(calibrate)
				{
					calibrate = false;
					
					m.what = CalcThread.CALIBRATE;
					h.sendMessageAtFrontOfQueue(m);
				}
				else
				{
					m.what = CalcThread.GRAVITY_CHANGE;
					m.obj = (Object)arg0.values[1];
					m.sendToTarget();
				}
				
				break;
			}
		}
	}
	
	private void startSensing()
	{
		if(calcThread == null)
		{
			calcThread = new CalcThread(TAG, new MyHandler());
			calcThread.start();
		}
		
		if(sensorMgr == null)
		{
			sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
			Sensor sensor = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			
			if (!sensorMgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI))
			{
				// on accelerometer on this device
				Log.e(TAG, "No accelerometer available");
				sensorMgr.unregisterListener(this, sensor);
				sensorMgr = null;
				return;
			}
		}
		
		calibrate = true;
	}
	
	private void stopSensing()
	{
		if(sensorMgr != null)
		{
			sensorMgr.unregisterListener(this);
			sensorMgr = null;
		}
		
		if(calcThread != null)
		{
			Handler h = calcThread.getHandler();
			if(h != null)
			{
				Message m = Message.obtain(h);
				if(m != null)
				{
					m.what = CalcThread.SENSOR_STOP;
					h.sendMessageAtFrontOfQueue(m);
				}
			}
			
			calcThread = null;
		}
	}
	
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
		if(arg0.getId() == R.id.CalibrateButton)
		{
			Log.d(TAG, "----Calibrate button clicked----");
			calibrate = true;
		}
		else if(arg0.getId() == R.id.ExitButton)
		{
			Log.d(TAG, "----Exit button clicked----");
			finish();
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "----onPause called----");
		stopSensing();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "----onResume called----");
		startSensing();
	}
}