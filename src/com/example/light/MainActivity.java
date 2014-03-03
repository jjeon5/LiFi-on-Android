package com.example.light;

import org.ejml.data.DenseMatrix64F;
import org.ejml.simple.SimpleMatrix;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Sensor;


import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
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
 	SimpleMatrix G, H;
 	SimpleMatrix[] perms;
	
	

 	
 	
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
        
        G = new SimpleMatrix(new double[][]{
     			
    			{1, 1, 0, 1},
    			{1, 0, 1, 1},
    			{1, 0, 0, 0},
    			{0, 1, 1, 1},
    			{0, 1, 0, 0},
    			{0, 0, 1, 0},
    			{0, 0, 0, 1}
    	
    		});
     	H = new SimpleMatrix(new double[][]{
    	
    			{1, 0, 1, 0, 1, 0, 1},
    			{0, 1, 1, 0, 0, 1, 1},
    			{0, 0, 0, 1, 1, 1, 1}
    	
    		});
        perms = new SimpleMatrix[7];
        for(int x = 0; x < perms.length; x++)
    	{
          double[][] t = new double[7][1];
    		t[x][0] = 1;
    		perms[x] = new SimpleMatrix(t);
    	}
        
        
        
        
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
        	if(dy>200)
        		isOn=true;
        	else if(dy<-200)
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
    	//textField.setText(String.valueOf(event.values[0]));
    }
   }
   };
   
    public void readMessage() throws UnsupportedEncodingException{ 
    	textField.setText(toRealString(decode(read(s))));
    	s = "";
	}
    public String read(String str){
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
    public String toRealString(String str) throws UnsupportedEncodingException{
    	if(str.length()%8==0){
    		byte[] bval = new BigInteger(str, 2).toByteArray();
    		String decoded = new String(bval, "UTF-8");
    		return decoded;
    	}
    	else
    		return "Try Again";
    }
    public String decode(String s){
    	if(s.length()%7==0)
    	{
    		Queue<double[]> q = new LinkedList<double[]>();
    		String snip = "";
    		String fin ="";
    		for(int i=0;i<s.length();i++){
    			if(snip.length()==7){
    				q.add(checkData(snip));
    				snip="";
    			}
    			else
    				snip=snip+s.charAt(i);
    		}
    		double[] d;
    		while(!q.isEmpty()){
    			d= q.remove();
    			for(int a=0;a<4;a++)
    				fin=fin+String.valueOf(d[a]);
    		}
    		return fin;	
    	}
    	else
    		return "";
    }
    public double[] checkData(String d)
	{
		double[][] arr = new double[d.length()][1];
		
		for(int x = 0; x < d.length(); x++)
			arr[x][0] = Double.valueOf(d.substring(x,x+1));
			
		SimpleMatrix r = new SimpleMatrix(arr);

			SimpleMatrix result = H.mult(r);

			double[] z = getCol(result, 0);
			
			for(int x = 0; x < z.length; x++)
				z[x] %= 2;
				
			if(errorFree(z))
				return getCol(r, 0);	
				
			for(int x = 0; x < perms.length; x++)
			{
				
				SimpleMatrix sum = r.plus(perms[x]);
				SimpleMatrix m = H.mult(sum);
				double[] res = getCol(m, 0);
				for(int y = 0; y < res.length; y++)
					res[y] %= 2; //res is detectGood
				
				if(errorFree(res))
				{
					double[] done = getCol(sum, 0);
					for(int f = 0; f < done.length; f++)
						done[f] %= 2;
					return done;
				}
			}
			return null;
	}
	
	public static boolean errorFree(double[] d)
	{
		for(double e : d)
			if(e != 0)
				return false;
		return true; 
	}
    
	public static double[] getCol(SimpleMatrix d, int index)
	{
		double[] ret = new double[d.numRows()];
		
		for(int x = 0; x < ret.length; x++)
			ret[x] = d.get(x, index);
			
		return ret;
	
	}
	
	public static double[] getRow(SimpleMatrix d, int index)
	{
		double[] ret = new double[d.numCols()];
		
		for(int x = 0; x < ret.length; x++)
			ret[x] = d.get(index, x);
			
		return ret;
	
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