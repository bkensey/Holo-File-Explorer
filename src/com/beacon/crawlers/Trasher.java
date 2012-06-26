package com.beacon.crawlers;

import java.io.File;
import java.util.List;

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

public class Trasher extends AsyncTask<Object, Void, Integer> {

	private static final String TAG = Trasher.class.getName();

	private MainActivity caller;
	private AbortionFlag flag;
	private ProgressDialog deleteProgressDialog;

	private static final int success = 0;
	private static final int err_deleting_folder = 1;
	private static final int err_deleting_child_file = 2;
	private static final int err_deleting_file = 3;

	private File errorFile;

	public Trasher(MainActivity activity) {
		caller = activity;
		flag = new AbortionFlag();
	}

	@Override
	protected void onPreExecute() {
		caller.runOnUiThread(new Runnable() {

			@Override
			public void run() {

				String message = caller.getString(R.string.deleting_path);
				deleteProgressDialog = new ProgressDialog(caller);
				deleteProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				deleteProgressDialog.setMessage(message);
				deleteProgressDialog.setButton(caller.getString(R.string.run_in_background), new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						dialog.dismiss();

					}
				});
				deleteProgressDialog.setButton2(caller.getString(R.string.cancel), new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						dialog.dismiss();
						Trasher.this.flag.abort();
					}
				});
				deleteProgressDialog.show();
			}
		});
	}

	@Override
	protected Integer doInBackground(Object... params) {
		
		Log.v(TAG, "Started doInBackground");
		Object files = params[0];		
		if (files instanceof List<?>) {
			for (File file : (List<File>) files) {
				int result = recursiveDelete(file);
				if (result != success)
					return result;
			}
			return success;
		} else
			return recursiveDelete((File) files);

	}

	@Override
	protected void onPostExecute(Integer result) {
		
		if (result == success) {
			caller.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (deleteProgressDialog.isShowing()) {
						deleteProgressDialog.dismiss();
					}
					caller.refreshCurrentPage();
				}
			});
			Toast.makeText(caller, R.string.file_deleted, Toast.LENGTH_SHORT).show();
		} else {
			final int resultCopy = result;
			caller.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (deleteProgressDialog.isShowing()) {
						deleteProgressDialog.dismiss();
					}
					switch (resultCopy) {
					case err_deleting_folder:
						Toast.makeText(caller, caller.getString(R.string.error_deleting_folder, errorFile.getAbsolutePath()),
								Toast.LENGTH_LONG).show();
						break;
					case err_deleting_child_file:
						Toast.makeText(caller, caller.getString(R.string.error_deleting_child_file, errorFile.getAbsolutePath()),
								Toast.LENGTH_SHORT).show();
						break;
					case err_deleting_file:
						Toast.makeText(caller, caller.getString(R.string.error_deleting_file, errorFile.getAbsolutePath()),
								Toast.LENGTH_LONG).show();
						break;
					}
				}
			});
		}

	}

	/**
	 * Recursively delete a file or directory and all of its children.
	 * 
	 * @returns 0 if successful, error value otherwise.
	 */
	private int recursiveDelete(File file) {
		if (file.isDirectory() && file.listFiles() != null)
			for (File childFile : file.listFiles()) {
				if (childFile.isDirectory()) {
					int result = recursiveDelete(childFile);
					if (result > 0) {
						return result;
					}
				} else {
					if (!childFile.delete()) {
						errorFile = childFile;
						return err_deleting_child_file;
					}
				}
			}

		if (!file.delete()) {
			errorFile = file;
			return file.isFile() ? err_deleting_file : err_deleting_folder;
		}

		return success;
	}
}