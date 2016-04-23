package com.hardcopy.arduinocontroller;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ArduinoControllerActivity extends Activity implements View.OnClickListener {

	private Context mContext = null;
	private UsbCommunication mSerialConn = null;
	private ReadListener readListener;
	
	private TextView mTextLog = null;
	private TextView mTextInfo = null;
	private Button mButton1;
	private Button mButton2;
	private Button mButton3;
	private Button mButton4;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// System
		mContext = getApplicationContext();
		
		// Layouts
		setContentView(R.layout.activity_arduino_controller);
		
		mTextLog = (TextView) findViewById(R.id.text_serial);
		mTextLog.setMovementMethod(new ScrollingMovementMethod());
		mTextInfo = (TextView) findViewById(R.id.text_info);
		mTextInfo.setMovementMethod(new ScrollingMovementMethod());
		mButton1 = (Button) findViewById(R.id.button_send1);
		mButton1.setOnClickListener(this);
		mButton2 = (Button) findViewById(R.id.button_send2);
		mButton2.setOnClickListener(this);
		mButton3 = (Button) findViewById(R.id.button_send3);
		mButton3.setOnClickListener(this);
		mButton4 = (Button) findViewById(R.id.button_send4);
		mButton4.setOnClickListener(this);
		
		// Initialize
		readListener = new ReadListener() {
			@Override
			public void onRead(String message) {
				mTextInfo.setText((String) message);
				mTextLog.append((String) message);
				mTextLog.append("\n");
			}

			@Override
			public void onError(String error) {
				Toast.makeText(ArduinoControllerActivity.this, error, Toast.LENGTH_SHORT).show();
			}
		};
		
		// Initialize Serial connector and starts Serial monitoring thread.
		mSerialConn = new UsbCommunication();
		mSerialConn.start(this, readListener);
		if (!mSerialConn.isConnected()) {
			Toast.makeText(this, "Não foi possível conectar. :( Por favor feche a aplicação e tente novamente.", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		mSerialConn.finalize();
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.button_send1:
			mSerialConn.send("a");
			break;
		case R.id.button_send2:
			mSerialConn.send("s");
			break;
		case R.id.button_send3:
			mSerialConn.send("d");
			break;
		case R.id.button_send4:
			mSerialConn.send("w");
			break;
		default:
			break;
		}
	}

}
