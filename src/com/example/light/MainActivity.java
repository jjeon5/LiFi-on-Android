package com.example.light;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Sensor;


import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.hardware.Camera.Parameters;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity{
	
	TextView textField;
    EditText input;
    ImageView imageView;
    
	SensorManager mySensorManager;
	Sensor LightSensor;
	public String s = new String();
	public boolean isOn = false;
	String is="";
 	String was="";
 	long interval, then;
 	int inter;
 	public ScheduledExecutorService scheduleTaskExecutor;
 	Future future;
 	double oneCount=0;
 	double zeroCount=0;
 	float onValue;
 	float offValue;
 	float dx, dy, avgSlope;
 	float[] slopeArray;
 	public String stri="";
 	public float y;
 	public float yThen;
 	public float onThreshold;
 	public float offThreshold;
 	int offCount;
 	public Thread thread, thread2;
 	public boolean firstTime;
 	int firstCount;
	
	Camera camera;
	Parameters paramOn, paramOff;
	String keySeq;
	char[] a;

 	protected void onStop() {
 		  super.onStop();
 		  if (camera != null) {
 		   camera.release();
 		  }
 		  scheduleTaskExecutor.shutdownNow();
 		 }
 	
    @Override
    public void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); 
        
       textField = (TextView)findViewById(R.id.textView);
       input = (EditText)findViewById(R.id.editText);

    	   mySensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
           LightSensor = mySensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
           if(LightSensor != null){
        	   textField.setText("Sensor.TYPE_LIGHT Available");
           }else{
        	   textField.setText("Sensor.TYPE_LIGHT NOT Available");
           }
          
           scheduleTaskExecutor = Executors.newScheduledThreadPool(1);
           firstTime=true;
           firstCount=0;
    }

    public void onToggleClicked(View view) {
        if (((ToggleButton) view).isChecked()) {

        	mySensorManager.registerListener(
        			LightSensorListener, 
        			LightSensor, 
        			SensorManager.SENSOR_DELAY_FASTEST);
        
        	future = scheduleTaskExecutor.scheduleAtFixedRate(record, 0, 2, TimeUnit.MILLISECONDS);
        } else {
        	future.cancel(true);	
        	yThen = 0;
        	mySensorManager.unregisterListener(LightSensorListener);
        	try {
				readMessage();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
        }
    }

    Runnable record = new Runnable() {
        @Override
        public void run() {
        	dy=y-yThen;
        	if(dy>3000)
        		isOn=true;
        	else if(dy<-3000)
        		isOn=false;
        	
        	if(isOn)
        		s=s+"1";
        	else
        		s=s+"0";
        	yThen=y;
        }
    };
    
   private final SensorEventListener LightSensorListener = new SensorEventListener(){
   @Override
   public void onAccuracyChanged(Sensor sensor, int accuracy) {
   }
   @Override
   public void onSensorChanged(SensorEvent event) {
    if(event.sensor.getType() == Sensor.TYPE_LIGHT){
    	y=event.values[0];
    }
   }
   };
   
    public void readMessage() throws UnsupportedEncodingException{ 
    	textField.setTextSize(20);
    	String nu=decode(s);
    	toRealString(nu);
    	s = "";
	}
    public String decode(String str){
    	zeroCount =0;
    	oneCount =0;
    	String nus = new String();
    	for(int k=0;k<str.length();k++){
    		if(str.charAt(k)=='1'){
    			for(int o=0;o<Math.round(zeroCount/5);o++){
        			nus=nus+"0";	
        			}
    			zeroCount=0;
    			oneCount++;
    		}
    		else{
    			for(int x=0;x<Math.round(oneCount/5);x++){
    			nus=nus+"1";	
    			}
    			oneCount=0;
    			zeroCount++;
    		}
    	}
    	if(nus.indexOf('1')!=-1)
    		nus=nus.substring((nus.indexOf('1')+1),(nus.length()-1));
    	return nus;
    }
    public void toImage(String str){
    	byte[] bval = new BigInteger(str, 2).toByteArray();
    	textField.setText(Arrays.toString(bval));
    	Bitmap bMap = BitmapFactory.decodeByteArray(bval, 0, bval.length);
    	imageView.setImageBitmap(bMap);
    }
    public void toRealString(String str) throws UnsupportedEncodingException{
    	if(str.length()%8==0){
    		byte[] bval = new BigInteger(str, 2).toByteArray();
    		String decoded = new String(bval, "UTF-8");
    		textField.setText(decoded);
    	}
    	else
    		textField.setText("Try Again");
    }
    
    private void getCamera() {
        if (camera == null) {
            try {
                camera = Camera.open();
                paramOn = camera.getParameters();
                paramOff = camera.getParameters();
                paramOn.setFlashMode(Parameters.FLASH_MODE_TORCH);
                paramOff.setFlashMode(Parameters.FLASH_MODE_OFF);
            } catch (RuntimeException e) {
                Log.e("Camera Error. Failed to Open. Error: ", e.getMessage());
            }
        }
    }
    
    public void on(){
        	camera.setParameters(paramOn);
            camera.startPreview();
    }
    
    public void off(){
        camera.setParameters(paramOff);
        camera.stopPreview();
    }
    
    public void send(View view){
    	keySeq = Integer.toBinaryString(Integer.parseInt(input.getText().toString()));
    	getCamera();
    	a = keySeq.toCharArray();
    	for(int i=0;i<keySeq.length();i++)
    	{
    			if(a[i]=='1')
    				on();
    			else
    				off();
    			try {
    			    Thread.sleep(7);
    			} catch(InterruptedException ex) {
    			    Thread.currentThread().interrupt();
    			}
    	}
    	off();
    	textField.setText(keySeq);
    }
    
}