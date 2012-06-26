package com.holo.fileexplorer;

//import android.app.ActionBar;
import java.io.File;

import org.apache.commons.io.FileUtils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
//import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import com.actionbarsherlock.view.MenuItem;
import com.beacon.crawlers.CheckSummer;
import com.holo.fileexplorer.R;
import com.holo.actions.FileActionSupport;
import com.holo.actions.FileActions;

import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class FileView extends SherlockFragmentActivity implements View.OnClickListener {
	public static FileMeta mFileMeta;
	private Layout mfiles;

	private TextView fileName;
	private TextView fileSize;
	private TextView fileMD5;
	private TextView copiedMD5;
	private ImageView fileIcon;
	private Button openButton;
	private Button saveButton;
	private Button loadButton;
	private Button infoButton;
	private Button cancelButton;
	private Button generateButton;
	private Button compareButton;
	private ProgressBar fileProgress;
	private File file;
	private boolean md5Generated;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		md5Generated = false;

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			file = new File(extras.getString("absPath"));
		}
		
		
		
		ActionBar bar = getSupportActionBar();
		bar.setTitle(file.getAbsolutePath());
		bar.setDisplayHomeAsUpEnabled(true);

		setContentView(R.layout.file_view);

		loadFile();
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void loadFile() {
		LayoutInflater inflater = getLayoutInflater();
		// View view = inflater.inflate(R.layout.message_view_file, null);
		// mfiles = (LinearLayout) findViewById(R.id.file_container);
		fileName = (TextView) findViewById(R.id.file_name);
		fileSize = (TextView) findViewById(R.id.file_size);
		fileMD5 = (TextView) findViewById(R.id.file_md5);
		copiedMD5 = (TextView) findViewById(R.id.copied_md5);
		fileIcon = (ImageView) findViewById(R.id.file_icon);
		openButton = (Button) findViewById(R.id.open);
		saveButton = (Button) findViewById(R.id.save);
		infoButton = (Button) findViewById(R.id.info);
		loadButton = (Button) findViewById(R.id.load);
		cancelButton = (Button) findViewById(R.id.cancel);
		generateButton = (Button) findViewById(R.id.md5_generate);
		compareButton = (Button) findViewById(R.id.md5_compare);
		fileProgress = (ProgressBar) findViewById(R.id.progress);

		updatefileButtons();

		fileName.setText(file.getName());
		fileSize.setText(FileUtils.byteCountToDisplaySize(file.length()));
		fileIcon.setImageDrawable(FileActionSupport.getIcon(this, file));

		openButton.setOnClickListener(this);
		saveButton.setOnClickListener(this);
		loadButton.setOnClickListener(this);
		infoButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);
		generateButton.setOnClickListener(this);
		// compareButton.setOnClickListener(this);

		TextWatcher watcher = new TextWatcher() {
			public void afterTextChanged(Editable s) {
				if (fileMD5.length() > 0 && copiedMD5.length() > 0 && (fileMD5.getText().toString().equals(copiedMD5.getText().toString()))) {
					compareButton.setText("Match!");
				} else if (fileMD5.length() > 0 && copiedMD5.length() > 0
						&& !(fileMD5.getText().toString().equals(copiedMD5.getText().toString()))) {
					compareButton.setText("No match.");
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		};

		fileMD5.addTextChangedListener(watcher);
		copiedMD5.addTextChangedListener(watcher);

		// fileName.setText(fileInfo.mName);
		// fileInfoView.setText(UiUtilities.formatSize(mContext,
		// fileInfo.mSize));
		//
		// mfiles.addView(view);
		// mfiles.setVisibility(View.VISIBLE);
	}

	/**
	 * Updates the file buttons. Adjusts the visibility of the buttons as well
	 * as updating any tag information associated with the buttons.
	 */
	private void updatefileButtons() {
		
		if (file.getName().endsWith(".apk")) {
			openButton.setText("Install");
		}
		
		openButton.setVisibility(View.VISIBLE);
		infoButton.setVisibility(View.VISIBLE);
		// ImageView fileIcon = fileInfo.iconView;
		// Button openButton = fileInfo.openButton;
		// Button saveButton = fileInfo.saveButton;
		// Button loadButton = fileInfo.loadButton;
		// Button infoButton = fileInfo.infoButton;
		// Button cancelButton = fileInfo.cancelButton;
		//
		// if (!fileInfo.mAllowView) {
		// openButton.setVisibility(View.GONE);
		// }
		// if (!fileInfo.mAllowSave) {
		// saveButton.setVisibility(View.GONE);
		// }
		//
		// if (!fileInfo.mAllowView && !fileInfo.mAllowSave) {
		// // This file may never be viewed or saved, so block everything
		// fileInfo.hideProgress();
		// openButton.setVisibility(View.GONE);
		// saveButton.setVisibility(View.GONE);
		// loadButton.setVisibility(View.GONE);
		// cancelButton.setVisibility(View.GONE);
		// infoButton.setVisibility(View.VISIBLE);
		// } else if (fileInfo.loaded) {
		// // If the file is loaded, show 100% progress
		// // Note that for POP3 messages, the user will only see "Open" and
		// "Save",
		// // because the entire message is loaded before being shown.
		// // Hide "Load" and "Info", show "View" and "Save"
		// fileInfo.showProgress(100);
		// if (fileInfo.mAllowSave) {
		// saveButton.setVisibility(View.VISIBLE);
		//
		// boolean isFileSaved = fileInfo.isFileSaved();
		// saveButton.setEnabled(!isFileSaved);
		// if (!isFileSaved) {
		// saveButton.setText(R.string.message_view_file_save_action);
		// } else {
		// saveButton.setText(R.string.message_view_file_saved);
		// }
		// }
		// if (fileInfo.mAllowView) {
		// // Set the file action button text accordingly
		// if (fileInfo.mContentType.startsWith("audio/") ||
		// fileInfo.mContentType.startsWith("video/")) {
		// openButton.setText(R.string.message_view_file_play_action);
		// } else if (fileInfo.mAllowInstall) {
		// openButton.setText(R.string.message_view_file_install_action);
		// } else {
		// openButton.setText(R.string.message_view_file_view_action);
		// }
		// openButton.setVisibility(View.VISIBLE);
		// }
		// if (fileInfo.mDenyFlags == fileInfo.ALLOW) {
		// infoButton.setVisibility(View.GONE);
		// } else {
		// infoButton.setVisibility(View.VISIBLE);
		// }
		// loadButton.setVisibility(View.GONE);
		// cancelButton.setVisibility(View.GONE);
		//
		// updatePreviewIcon(fileInfo);
		// } else {
		// // The file is not loaded, so present UI to start downloading it
		//
		// // Show "Load"; hide "View", "Save" and "Info"
		// saveButton.setVisibility(View.GONE);
		// openButton.setVisibility(View.GONE);
		// infoButton.setVisibility(View.GONE);
		//
		// // If the file is queued, show the indeterminate progress bar. From
		// this point,.
		// // any progress changes will cause this to be replaced by the normal
		// progress bar
		// if (fileDownloadService.isfileQueued(fileInfo.mId)) {
		// fileInfo.showProgressIndeterminate();
		// loadButton.setVisibility(View.GONE);
		// cancelButton.setVisibility(View.VISIBLE);
		// } else {
		// loadButton.setVisibility(View.VISIBLE);
		// cancelButton.setVisibility(View.GONE);
		// }
		// }
		// openButton.setTag(fileInfo);
		// saveButton.setTag(fileInfo);
		// loadButton.setTag(fileInfo);
		// infoButton.setTag(fileInfo);
		// cancelButton.setTag(fileInfo);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.open:
			FileActions.openFile(this, file);
			break;
		case R.id.save:
			Toast.makeText(this, "Save not implemented yet", Toast.LENGTH_SHORT).show();
			break;
		case R.id.info:
			Toast.makeText(this, "May just ditch this button entirely", Toast.LENGTH_SHORT).show();
			break;
		case R.id.cancel:
			Toast.makeText(this, "Cancel not implemented yet", Toast.LENGTH_SHORT).show();
			break;
		case R.id.md5_generate:
			new CheckSummer(this).execute(file);
			break;
		case R.id.md5_compare:
			Toast.makeText(this, "MD5 Compare not implemented yet", Toast.LENGTH_SHORT).show();
			break;
		}
	}

	public void setFileMD5(String md5) {
		fileMD5.setText(md5);
	}
}
