package com.beacon.crawlers;

import java.io.File;

import com.holo.actions.AbortionFlag;
import com.holo.actions.FileActions;
import com.holo.actions.MD5Checksum;
import com.holo.fileexplorer.R;
import com.holo.fileexplorer.FileView;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class CheckSummer extends AsyncTask<File, Integer, String> {
	private static final String TAG = Mover.class.getName();

	private AbortionFlag flag;
	private FileView caller;
	private ProgressDialog sumProgressDialog;

	public CheckSummer(FileView context) {
		caller = context;		
		flag = new AbortionFlag();
	}

	@Override
	protected void onPreExecute() {
		caller.runOnUiThread(new Runnable() {

			@Override
			public void run() {

				sumProgressDialog = new ProgressDialog(caller);
				sumProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				sumProgressDialog.setMessage("Summing file. Be back in a sec.");
				sumProgressDialog.setButton(caller.getString(R.string.run_in_background), new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						dialog.dismiss();

					}
				});
				sumProgressDialog.setButton2(caller.getString(R.string.cancel), new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						dialog.dismiss();
						CheckSummer.this.flag.abort();
					}
				});
				sumProgressDialog.show();

			}
		});
	}

	@Override
	protected String doInBackground(File... params) {
		Log.v(TAG, "Started doInBackground");
		File f = params[0];
		try {
			return MD5Checksum.getMD5Checksum(f);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	@Override
	protected void onPostExecute(String result) {

		Log.v(TAG, "Inside post execute. Result of MD5 operation is - " + result);
		if (result.length() > 0) {
			caller.setFileMD5(result);
			caller.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (sumProgressDialog.isShowing()) {
						sumProgressDialog.dismiss();
					}
					
					Toast.makeText(caller.getApplicationContext(), caller.getString(R.string.md5_complete), Toast.LENGTH_LONG);
				}
			});
		} else {
			caller.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (sumProgressDialog.isShowing()) {
						sumProgressDialog.dismiss();
					}
					Toast.makeText(caller.getApplicationContext(), caller.getString(R.string.generic_operation_failed), Toast.LENGTH_LONG);
				}
			});
		}
	}
}
