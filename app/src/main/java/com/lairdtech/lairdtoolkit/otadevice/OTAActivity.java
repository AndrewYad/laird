/*****************************************************************************
* Copyright (c) 2014 Laird Technologies. All Rights Reserved.
* 
* The information contained herein is property of Laird Technologies.
* Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
* This heading must NOT be removed from the file.
******************************************************************************/

package com.lairdtech.lairdtoolkit.otadevice;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lairdtech.bt.ble.vsp.FifoAndVspManager.FifoAndVspManagerState;
import com.lairdtech.bt.ble.vsp.FileAndFifoAndVspManager.FileState;
import com.lairdtech.bt.ble.vsp.VirtualSerialPortDevice;
import com.lairdtech.lairdtoolkit.R;
import com.lairdtech.lairdtoolkit.bases.BaseActivity;
import com.lairdtech.misc.DebugWrapper;


public class OTAActivity extends BaseActivity implements OTAManagerUiCallback{
	private static final int FILE_SELECT_REQUEST_CODE = 2;

	private static final String COLOR_GREEN = "#009933";
	private static final String COLOR_RED = "#993333";

	private Button mBtnFileSelect;
	private Button mBtnFileDownload;
	private Button mBtnFileStopDownloading;

	private TextView mValueFileNameTv;
	private TextView mValueSavedAsTv;
	private TextView mValueValidDeviceTv;
	private TextView mValueStatusTv;
	private TextView mValueErrorsTv;

	private ProgressBar mProgressBar;
	private TextView mTvProgressBarProgress;

	private OTAManager mOTAManager;

	private boolean isPrefResetModuleAfterSending = false;


	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		setContentView(R.layout.activity_ota);
		super.onCreate(savedInstanceState);

		mOTAManager = new OTAManager(this, this);
		setBleDeviceBase(mOTAManager.getVSPDevice());

		initialiseDialogAbout(getResources().getString(R.string.about_ota));
		initialiseDialogFoundDevices("VSP");
		
		/*
		 * check if the user chose a .uwc file from a file browser app
		 */
		Intent i = getIntent();
		if(i != null){
			Uri result = i.getData();
			if(result != null){
				mOTAManager.setFile(result);
				mProgressBar.setMax((int) mOTAManager.getFileWrapper().getFileTotalSize());
				mProgressBar.setProgress(0);
				mTvProgressBarProgress.setText(0+"/"+mProgressBar.getMax());
			}
		}
	}

	/*
	 * *************************************
	 * UI methods
	 * *************************************
	 */
	@Override
	protected void bindViews(){
		super.bindViews();

		mBtnFileSelect = (Button) findViewById(R.id.btnFileSelect);
		mBtnFileDownload = (Button) findViewById(R.id.btnFileDownload);
		mBtnFileStopDownloading = (Button) findViewById(R.id.btnFileStopDownloading);

		mValueFileNameTv = (TextView) findViewById(R.id.valueFileNameTv);
		mValueSavedAsTv = (TextView) findViewById(R.id.valueSavedAsTv);
		mValueValidDeviceTv = (TextView) findViewById(R.id.valueValidDeviceTv);
		mValueStatusTv = (TextView) findViewById(R.id.valueStatusTv);
		mValueErrorsTv = (TextView) findViewById(R.id.valueErrorsTv);

		mProgressBar = (ProgressBar) findViewById(R.id.progressBar);    
		mTvProgressBarProgress = (TextView) findViewById(R.id.progressBarProgressTv);
	}

	@Override
	protected void setListeners(){
		super.setListeners();

		mBtnFileSelect.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				/*
				 * open file browser to select a file
				 */
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);

				intent.setType("*/*");
				startActivityForResult(intent, FILE_SELECT_REQUEST_CODE );	
			}
		});

		mBtnFileDownload.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(mOTAManager == null) return;
				/*
				 * start uploading procedure
				 */
				if(mOTAManager.getVSPDevice().isConnected() == true){
					mBtnFileDownload.setEnabled(false);
					mOTAManager.startDataTransfer();

					mBtnFileSelect.setEnabled(false);
					mBtnFileDownload.setEnabled(false);
					mBtnFileStopDownloading.setEnabled(true);

					mValueStatusTv.setText(R.string.value_status_uploading);
					mValueStatusTv.setTextColor(Color.BLUE);
					mValueErrorsTv.setText("");


				} else{
					DebugWrapper.toastMsg(mActivity, "Must be connected with a BLE device");
				}
			}
		});

		mBtnFileStopDownloading.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(mOTAManager == null) return;
				/*
				 * Stop uploading procedure
				 */
				mOTAManager.stopFileUploading();

				mValueStatusTv.setText(R.string.value_status_stopped);
				mValueStatusTv.setTextColor(Color.parseColor(COLOR_RED));

				mBtnFileSelect.setEnabled(true);
				mBtnFileDownload.setEnabled(true);
				mBtnFileStopDownloading.setEnabled(false);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == FILE_SELECT_REQUEST_CODE ) {
			if (resultCode == Activity.RESULT_CANCELED) {
				DebugWrapper.toastMsg(this, "Cancelled");
			} else if(resultCode == Activity.RESULT_OK){
				/*
				 * a file was chosen
				 */
				if (data != null) {
					Uri result;
					result = data.getData();
					mOTAManager.setFile(result);
					// display file uploading progress
					mProgressBar.setMax((int) mOTAManager.getFileWrapper().getFileTotalSize());
					mProgressBar.setProgress(0);
					mTvProgressBarProgress.setText(0+"/"+mProgressBar.getMax());

					mValueFileNameTv.setText(mOTAManager.getFileWrapper().getFileName());
					mValueSavedAsTv.setText(mOTAManager.getFileWrapper().getModuleFileName());
					mValueStatusTv.setText(R.string.value_status_waiting);
					mValueStatusTv.setTextColor(Color.GRAY);

					if(mOTAManager.getFifoAndVspManagerState() == FifoAndVspManagerState.READY_TO_SEND_DATA){
						mBtnFileSelect.setEnabled(true);
						mBtnFileDownload.setEnabled(true);
						mBtnFileStopDownloading.setEnabled(false);
					} else{
						mBtnFileSelect.setEnabled(true);
						mBtnFileDownload.setEnabled(false);
						mBtnFileStopDownloading.setEnabled(false);
					}
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.ota_menu, menu);
		getActionBar().setIcon(R.drawable.icon_ota);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		if (mBluetoothAdapterWrapper.isBleScanning() == true) {
			menu.findItem(R.id.action_scanning_indicator).setActionView(R.layout.progress_indicator);
		} else {
			menu.findItem(R.id.action_scanning_indicator).setActionView(null);
		}
		return true;
	}

	@Override
	public void onBackPressed(){
		if(mOTAManager.getFileState() == FileState.FILE_CHOSEN){
			mProgressBar.setProgress(0);
			mTvProgressBarProgress.setText(0+"/0");
		}
		mValueStatusTv.setText("");
		mValueValidDeviceTv.setText("");
		mValueErrorsTv.setText("");

		super.onBackPressed();
	}


	/*
	 * *************************************
	 * OTAManagerUiCallback
	 * *************************************
	 */
	@Override
	public void onUiConnected(BluetoothGatt gatt) {
		uiInvalidateBtnState();
	}

	@Override
	public void onUiDisconnect(BluetoothGatt gatt) {
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				if(mOTAManager.getFileState() == FileState.FILE_CHOSEN){
					mProgressBar.setProgress(0);
					mTvProgressBarProgress.setText(0+"/"+mProgressBar.getMax());
				}
				mValueStatusTv.setText("");
				mValueValidDeviceTv.setText("");
				mValueErrorsTv.setText("");

				mBtnFileSelect.setEnabled(true);
				mBtnFileDownload.setEnabled(false);
				mBtnFileStopDownloading.setEnabled(false);
			}
		});
		uiInvalidateBtnState();
	}

	@Override
	public void onUiConnectionFailure(
			final BluetoothGatt gatt){

		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				if(mOTAManager.getFileState() == FileState.FILE_CHOSEN){
					mProgressBar.setProgress(0);
					mTvProgressBarProgress.setText(0+"/"+mProgressBar.getMax());
				}
				mValueStatusTv.setText("");
				mValueValidDeviceTv.setText("");
				mValueErrorsTv.setText("");

				mBtnFileSelect.setEnabled(true);
				mBtnFileDownload.setEnabled(false);
				mBtnFileStopDownloading.setEnabled(false);
			}
		});
		uiInvalidateBtnState();
	}
	
	@Override
	public void onUiBatteryReadSuccess(String result) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUiReadRemoteRssiSuccess(int rssi) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUiBonded() {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void onUiVspServiceNotFound(BluetoothGatt gatt) {

		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				mValueValidDeviceTv.setText("No");
				mValueValidDeviceTv.setTextColor(Color.parseColor(COLOR_RED));

				mBtnFileSelect.setEnabled(true);
				mBtnFileDownload.setEnabled(false);
				mBtnFileStopDownloading.setEnabled(false);
			}
		});
		uiInvalidateBtnState();
	}

	@Override
	public void onUiVspRxTxCharsNotFound(BluetoothGatt gatt) {
		runOnUiThread(new Runnable(){
			@Override
			public void run() {

				mValueValidDeviceTv.setText("No");
				mValueValidDeviceTv.setTextColor(Color.parseColor(COLOR_RED));

				mBtnFileSelect.setEnabled(true);
				mBtnFileDownload.setEnabled(false);
				mBtnFileStopDownloading.setEnabled(false);
			}
		});
		uiInvalidateBtnState();
	}

	@Override
	public void onUiVspRxTxCharsFound(BluetoothGatt gatt) {
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				mValueValidDeviceTv.setText("Yes");
				mValueValidDeviceTv.setTextColor(Color.parseColor(COLOR_GREEN));

				if(mOTAManager.getFileState() == FileState.FILE_CHOSEN){
					mBtnFileSelect.setEnabled(true);
					mBtnFileDownload.setEnabled(true);
					mBtnFileStopDownloading.setEnabled(false);
				} else{
					mBtnFileSelect.setEnabled(true);
					mBtnFileDownload.setEnabled(false);
					mBtnFileStopDownloading.setEnabled(false);
				}
			}
		});
		uiInvalidateBtnState();
	}

	@Override
	public void onUiSendDataSuccess(final String dataSend) {
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				/*
				 * update the progress bar and stopping it from filling beyond our limit
				 */
				if(mOTAManager.getFileWrapper().getFileCurrentSizeRead() > mOTAManager.getFileWrapper().getFileTotalSize()){
					mProgressBar.setProgress((int) mOTAManager.getFileWrapper().getFileTotalSize());
					mTvProgressBarProgress.setText(mOTAManager.getFileWrapper().getFileTotalSize()+"/"+mProgressBar.getMax());
				} else{
					mProgressBar.setProgress((int) mOTAManager.getFileWrapper().getFileCurrentSizeRead());
					mTvProgressBarProgress.setText(mOTAManager.getFileWrapper().getFileCurrentSizeRead()+"/"+mProgressBar.getMax());
				}
			}
		});
	}

	@Override
	public void onUiReceiveSuccessData(final String dataReceived) {}

	@Override
	public void onUiReceiveErrorData(final String errorCode) {
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				mValueStatusTv.setText(R.string.value_status_failed);

				if(VirtualSerialPortDevice.ERROR_CODE_FILE_NOT_OPEN.equals(errorCode)){
					mValueErrorsTv.append(errorCode + "(File not open)");
				} else if(VirtualSerialPortDevice.ERROR_CODE_FSA_FAIL_OPENFILE.equals(errorCode)){
					mValueErrorsTv.append(errorCode + "(Failed to open file)");
				} else if(VirtualSerialPortDevice.ERROR_CODE_INCORRECT_MODE.equals(errorCode)){
					mValueErrorsTv.append(errorCode + "(Incorrect mode)");
				} else if(VirtualSerialPortDevice.ERROR_CODE_MEMORY_FULL.equals(errorCode)){
					mValueErrorsTv.append(errorCode + "(Memory is full)");
				} else if(VirtualSerialPortDevice.ERROR_CODE_NO_FILE_TO_CLOSE.equals(errorCode)){
					mValueErrorsTv.append(errorCode + "(No file to close)");
				} else if(VirtualSerialPortDevice.ERROR_CODE_UNEXPECTED_PARM.equals(errorCode)){
					mValueErrorsTv.append(errorCode + "(Unexpected parameter)");
				} else if(VirtualSerialPortDevice.ERROR_CODE_UNKNOWN_COMMAND.equals(errorCode)){
					mValueErrorsTv.append(errorCode + "(Unknown command)");
				} else if(VirtualSerialPortDevice.ERROR_CODE_FSA_FILENAME_TOO_LONG.equals(errorCode)){
					mValueErrorsTv.append(errorCode + "(Filename too long)");
				} else{
					mValueErrorsTv.append(errorCode);
				}

				mValueStatusTv.setTextColor(Color.parseColor(COLOR_RED));
				mValueErrorsTv.setTextColor(Color.parseColor(COLOR_RED));

				mBtnFileSelect.setEnabled(true);
				mBtnFileDownload.setEnabled(true);
				mBtnFileStopDownloading.setEnabled(false);

			}
		});
	}

	@Override
	public void onUiUploaded() {
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				mValueStatusTv.setText(R.string.value_status_success);
				mValueStatusTv.setTextColor(Color.parseColor(COLOR_GREEN));
				mBtnFileSelect.setEnabled(false);

				mBtnFileSelect.setEnabled(true);
				mBtnFileDownload.setEnabled(true);
				mBtnFileStopDownloading.setEnabled(false);
			}
		});
		mOTAManager.resetModule(isPrefResetModuleAfterSending);
	}
	
	/*
	 * *************************************
	 * other
	 * *************************************
	 */
	@Override
	protected void loadPref(){
		super.loadPref();
		isPrefResetModuleAfterSending = mSharedPreferences.getBoolean("pref_reset_module_after_sending", false);
	}
}