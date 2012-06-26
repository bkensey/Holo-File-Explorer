package com.beacon.crawlers;

import java.io.File;

import com.holo.actions.AbortionFlag;
import com.holo.actions.FileActions;
import com.holo.fileexplorer.R;
import com.holo.fileexplorer.MainActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class Mover extends AsyncTask<File, Integer, Boolean> {
	
	private static final String TAG = Mover.class.getName();

	private int mode = 0;
	private AbortionFlag flag;
	private MainActivity caller;
	private ProgressDialog moveProgressDialog;

	public Mover(MainActivity context, int mode) {
		caller = context;
		this.mode = mode;
		flag = new AbortionFlag();
	}

	@Override
	protected void onPreExecute() {
		FileActions.setPasteUnavailable();
		caller.runOnUiThread(new Runnable() {

			@Override
			public void run() {

				String message = caller.getString(R.string.copying_path);
				if (mode == FileActions.PASTE_MODE_MOVE) {
					message = caller.getString(R.string.moving_path);
				}
				moveProgressDialog = new ProgressDialog(caller);
				moveProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				moveProgressDialog.setMessage(message);
				moveProgressDialog.setButton(caller.getString(R.string.run_in_background), new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						dialog.dismiss();

					}
				});
				moveProgressDialog.setButton2(caller.getString(R.string.cancel), new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						dialog.dismiss();
						Mover.this.flag.abort();
					}
				});
				moveProgressDialog.show();

			}
		});
	}

	@Override
	protected Boolean doInBackground(File... params) {

		Log.v(TAG, "Started doInBackground");
		File destDir = params[0];
		return FileActions.paste(caller, mode, destDir, flag);

	}

	@Override
	protected void onPostExecute(Boolean result) {

		Log.v(TAG, "Inside post execute. Result of paste operation is - " + result);
		if (result) {
			// TODO consider leaving file there for second copy
			if (mode == FileActions.PASTE_MODE_MOVE || mode == FileActions.PASTE_MODE_COPY) {
				Log.v(TAG, "Paste mode was MOVE - set src file to null");
				FileActions.clearPaste();
			}
			caller.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (moveProgressDialog.isShowing()) {
						moveProgressDialog.dismiss();
					}
					if (mode == FileActions.PASTE_MODE_COPY) {
						Toast.makeText(caller.getApplicationContext(), caller.getString(R.string.copy_complete), Toast.LENGTH_LONG);
					} else {
						Toast.makeText(caller.getApplicationContext(), caller.getString(R.string.move_complete), Toast.LENGTH_LONG);
					}

					caller.refreshCurrentPage();
					// TODO: if caller is different from the copy/cut
					// originator, and both are looking at the same page, also
					// refresh originator
				}
			});
		} else {
			caller.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (moveProgressDialog.isShowing()) {
						moveProgressDialog.dismiss();
					}
					Toast.makeText(caller.getApplicationContext(), caller.getString(R.string.generic_operation_failed), Toast.LENGTH_LONG);
				}
			});
		}
	}
}
