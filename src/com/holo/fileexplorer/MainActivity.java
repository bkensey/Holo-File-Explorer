/*
 * Copyright 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.holo.fileexplorer;

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.holo.fileexplorer.R;
import com.holo.root.ExtendedMount;
import com.holo.root.RootSupport;
import com.holo.actions.FileActionSupport;
import com.stericson.RootTools.*;
import com.viewpagerindicator.UnderlinePageIndicator;

import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends SherlockFragmentActivity implements
		OnPageChangeListener {

	private MenuItem mUp;
	private MenuItem mBookmarks;
	private MenuItem mSearch;
	private MenuItem mNewFolder;
	private MenuItem mSort;
	private MenuItem mRefresh;
	private MenuItem mSettings;
	private MenuItem mExit;
	private MenuItem mReadOnly;
	private MenuItem mReadWrite;

	// Dialog Codes
	public static final int DIALOG_NEW_FOLDER = 1;
	public static final int DIALOG_DELETE = 2;
	public static final int DIALOG_RENAME = 3;
	public static final int DIALOG_MULTI_DELETE = 4;
	public static final int DIALOG_FILTER = 5;
	public static final int DIALOG_DETAILS = 6;
	public static final int DIALOG_BOOKMARKS = 7;
	public static final int DIALOG_ZIP = 8;
	public static final int DIALOG_WARNING_EXISTS = 9;
	public static final int DIALOG_CHANGE_FILE_EXTENSION = 10;
	public static final int DIALOG_MULTI_COMPRESS_ZIP = 11;

	// Used to pass parameters to onCreateDialog method
	private String mDialogArgument;

	// ViewPager variables
	public FileFragmentPagerAdapter mViewPager;
	public ViewPager mPager;
	private UnderlinePageIndicator mPageIndicator;

	// Preference variables
	private OnSharedPreferenceChangeListener listener;

	private Menu mMainOptionsMenu;

	private boolean shouldRestartApp = false;
	private int mThemeId = -1;
	public boolean firstRun = true;
	private String newZipName = "";

	// Root and Busybox availability flags
	public boolean rootEnabled = false;
	public boolean busyBoxIsAvailable = false;
	public boolean rootIsAvailable = false;
	public ArrayList<ExtendedMount> mounts = null;

	/**
	 * Where all of the magic starts. This is run when the application first
	 * opens. It determines the current theme and instantiates the ViewPager
	 * object.
	 * 
	 * @param savedInstanceState
	 *            the freeze-dried state of the app before it was put into
	 *            hibernation (unless this is the initial start-up.
	 * @return nothing
	 */
	public void onCreate(Bundle savedInstanceState) {
		// android.os.Debug.startMethodTracing("lsd");
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		String themeTest = prefs.getString("theme", "identity");

		if (themeTest.equalsIgnoreCase("light")) {
			mThemeId = R.style.AppTheme_Light;
		} else if (themeTest.equalsIgnoreCase("dark")) {
			mThemeId = R.style.AppTheme_Dark;
		} else if (themeTest.equalsIgnoreCase("darkactionbar")) {
			mThemeId = R.style.AppTheme_Light_DarkActionBar;
		} else if (themeTest.equalsIgnoreCase("identity")) {
			mThemeId = R.style.AppTheme_Identity;
		}
		this.setTheme(mThemeId);

		// This must be called after setTheme() on pre-Honeycomb devices in
		// order for the correct theme background to show up properly. If this
		// is called first, the background from the theme defined in the
		// manifest will always be used regardless of new themes set via
		// setTheme().
		super.onCreate(savedInstanceState);

		setContentView(R.layout.fragment_pager);

		ActionBar bar = getSupportActionBar();
		bar.setTitle("");

		mViewPager = new FileFragmentPagerAdapter(this,
				getSupportFragmentManager());

		mPager = (ViewPager) findViewById(R.id.pager);

		mPager.setAdapter(mViewPager);
		mPager.setHorizontalScrollBarEnabled(true);

		// TODO this is a pretty much a hack job for now, and proper state
		// retaining needs to be implemented for tablets
		mPager.setOffscreenPageLimit(mViewPager.getCount());

		// Set the drawable for the divider between each page in the ViewPager
		mPager.setPageMargin(FileActionSupport.convertDip2Pixels(this, 3));
		mPager.setPageMarginDrawable(R.color.icsblue);

		mPageIndicator = (UnderlinePageIndicator) findViewById(R.id.indicator);
		mPageIndicator.setViewPager(mPager);
		
		rootEnabled = prefs.getBoolean("rootEnabled", false);
		
		if (rootEnabled) {
			if (RootTools.isRootAvailable()) {
				// su exists, so we can actually see if busybox is installed.
				// Unrooted users cannot use any of the root features, so lets
				// not
				// bother them with busybox

				// Check to see if busybox is available
				if (RootTools.isBusyboxAvailable()) {
					busyBoxIsAvailable = true;
				} else {
					// TODO change to RootTools method "2" to in order to
					// determine
					// what the User did in the market
					RootTools.offerBusyBox(this);
				}

				// Check to see if root access has been granted
				if (RootTools.isAccessGiven()) {
					// We're good to go!
					rootIsAvailable = true;
					try {
						mounts = RootSupport.getMounts();

					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			} else {
				// Not rooted. This line should never be reached since the root
				// preferences options should be disabled on non-rooted devices.
			}
		} else {
			// Root functionality is not enabled in the preferences
		}
	}

//	private void checkRoot() {
//		SharedPreferences prefs = PreferenceManager
//				.getDefaultSharedPreferences(this);
//		try {
//			if (prefs.getBoolean("rootEnabled", false)
//					&& (RootManager.Default == null || !RootManager.Default
//							.isRoot())) {
//				requestRoot();
//				rootEnabled = true;
//			} else if (RootManager.Default != null) {
//				exitRoot();
//			}
//
//		} catch (Exception e) {
//			// Logger.LogWarning("Couldn't get root.", e);
//		}
//
//	}
//
//	private void requestRoot() {
//		// new Thread(new Runnable(){public void run(){
//		RootManager.Default.requestRoot();
//		// }}).start();
//	}
//
//	private void exitRoot() {
//		if (RootManager.Default == null)
//			return;
//		new Thread(new Runnable() {
//			public void run() {
//				RootManager.Default.exitRoot();
//			}
//		}).start();
//	}

	/**
	 * This is basically run whenever the app is closed and then reopened.
	 */
	public void onResume() {
		super.onResume();

		if (shouldRestartApp) {
			shouldRestartApp = false;

			Intent i = getBaseContext().getPackageManager()
					.getLaunchIntentForPackage(
							getBaseContext().getPackageName());
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
		}

		listenForThemeChange();
		mPageIndicator.setOnPageChangeListener(this);

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("theme", mThemeId);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mMainOptionsMenu = menu;
		MenuInflater menuInflater = getSupportMenuInflater();
		menuInflater.inflate(R.menu.message_list_main, menu);

		mUp = menu.findItem(R.id.action_up);
		mBookmarks = menu.findItem(R.id.action_open_bookmarks);
		mSearch = menu.findItem(R.id.action_search);
		mNewFolder = menu.findItem(R.id.action_new_folder);
		mSort = menu.findItem(R.id.action_sort);
		mRefresh = menu.findItem(R.id.action_refresh);
		mSettings = menu.findItem(R.id.action_settings);
		mExit = menu.findItem(R.id.action_exit);
		mReadOnly = menu.findItem(R.id.action_ro);
		mReadWrite = menu.findItem(R.id.action_rw);

		// Calling super after populating the menu is necessary here to ensure
		// that the action bar helpers have a chance to handle this event.
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		mUp.setVisible(true);
		mBookmarks.setVisible(true);
		mSearch.setVisible(true);
		mNewFolder.setVisible(true);
		mSort.setVisible(true);
		mRefresh.setVisible(true);
		mSettings.setVisible(true);
		mExit.setVisible(true);
		mReadOnly.setVisible(false);
		mReadWrite.setVisible(false);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Toast.makeText(this, "Tapped home", Toast.LENGTH_SHORT).show();
			break;
		case R.id.action_rw:
			String currentPath = mViewPager
					.getFragment(mPager, mPager.getCurrentItem())
					.getCurrentDir().getAbsolutePath();

			boolean result = RootTools.remount(currentPath, "rw");
			ArrayList<Mount> mounts = new ArrayList<Mount>();
			try {
				mounts = RootTools.getMounts();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mounts.clear();
			Log.v("MOUNT RW?",
					"RW WRITABLE? " + new File(currentPath).canWrite());
			break;
		case R.id.action_ro:
			RootTools.remount(
					mViewPager.getFragment(mPager, mPager.getCurrentItem())
							.getCurrentDir().getAbsolutePath(), "ro");
			break;
		case R.id.action_up:
			mViewPager.getFragment(mPager, mPager.getCurrentItem())
					.upOneLevel();
			break;
		case R.id.action_new_folder:
			LayoutInflater inflater = LayoutInflater.from(this);
			View view = inflater.inflate(R.layout.dialog_new_folder, null);
			final EditText et = (EditText) view.findViewById(R.id.foldername);
			et.setText("");
			new AlertDialog.Builder(this)
					.setTitle(R.string.create_new_folder)
					.setView(view)
					.setPositiveButton(android.R.string.ok,
							new OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									mViewPager.getFragment(mPager,
											mPager.getCurrentItem())
											.createNewFolder(
													et.getText().toString());
								}

							})
					.setNegativeButton(android.R.string.cancel,
							new OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									// Cancel should not do anything beyond
									// closing the dialog.
								}

							}).show();
			break;
		case R.id.action_open_bookmarks:
			Toast.makeText(this,
					"Tapped bookmarks (Not implemented yet. Patience.)",
					Toast.LENGTH_SHORT).show();
			break;
		case R.id.action_refresh:
			// getSupportActionBarHelper().setRefreshActionItemState(true);
			refreshCurrentPage(); // TODO: preserve scroll position and show
									// progress
			// getSupportActionBarHelper().setRefreshActionItemState(false);
			Toast.makeText(this, "Folder Refreshed", Toast.LENGTH_SHORT).show();
			break;
		case R.id.action_search:
			Toast.makeText(this,
					"Tapped search  (Not implemented yet. Patience.)",
					Toast.LENGTH_SHORT).show();
			break;
		// case R.id.action_storage_summary:
		// Toast.makeText(this,
		// "Tapped storage summary  (Not implemented yet. It'll be good though.)",
		// Toast.LENGTH_SHORT).show();
		// break;
		case R.id.action_sort:
			// Do nothing. This has a submenu
			break;
		case R.id.sort_alphabetical:
			item.setChecked(true);
			mViewPager.getFragment(mPager, mPager.getCurrentItem()).sort(
					FileListFragment.SORT_ALPHABETICAL);
			break;
		case R.id.sort_last_modified:
			item.setChecked(true);
			mViewPager.getFragment(mPager, mPager.getCurrentItem()).sort(
					FileListFragment.SORT_LAST_MODIFIED);
			break;
		case R.id.sort_reverse:
			// Toggle the checked state
			Boolean newState = !item.isChecked();
			item.setChecked(newState);
			mViewPager.getFragment(mPager, mPager.getCurrentItem())
					.sortDirection(newState);
			break;

		case R.id.action_settings:
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				Intent intent = new Intent(this, EditPreferences.class);
				intent.putExtra("themeid", mThemeId);
				startActivity(new Intent(this, EditPreferences.class));
			} else {
				startActivity(new Intent(this, EditPreferencesHC.class));
			}
			break;
		case R.id.action_exit:
			// finish();
			android.os.Process.killProcess(android.os.Process.myPid());
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	// This triggers an activity reload after a theme change so that the new
	// styles and assets can be pulled in
	private void listenForThemeChange() {

		listener = new OnSharedPreferenceChangeListener() {

			@Override
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {
				if (key.equals("theme")) {

					shouldRestartApp = true;

				}
			}
		};

		PreferenceManager.getDefaultSharedPreferences(this)
				.registerOnSharedPreferenceChangeListener(listener);
	}

	/**
	 * Since Dialogs are (almost always) attached to an activity, they are all
	 * defined here to provide simple, combined access. Courtesy of OpenIntents:
	 * <p>
	 * http://code.google.com/p/openintents/source/browse/#svn/trunk/samples/
	 * TestFileManager
	 * <p>
	 * *Note: commented code is straight from OI, and uncommented has been
	 * modified to work with HFE
	 * 
	 * @param id
	 *            id code of the desired dialog. Defined as a set of constants
	 *            within MainActivity
	 * @param bundle
	 *            the bundle containing any parameters to be used by the dialog
	 * @return a reference to the open dialog
	 */
	//
	@Override
	protected Dialog onCreateDialog(int id, Bundle bundle) {
		switch (id) {
		case DIALOG_NEW_FOLDER:
			LayoutInflater inflater = LayoutInflater.from(this);
			View view = inflater.inflate(R.layout.dialog_new_folder, null);
			final EditText et = (EditText) view.findViewById(R.id.foldername);
			et.setText("");
			// accept "return" key
			TextView.OnEditorActionListener returnListener = new TextView.OnEditorActionListener() {
				public boolean onEditorAction(TextView exampleView,
						int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_NULL
							&& event.getAction() == KeyEvent.ACTION_DOWN) {
						// match this behavior to your OK button
						// createNewFolder(et.getText().toString());
						dismissDialog(DIALOG_NEW_FOLDER);
					}
					return true;
				}

			};
			// et.setOnEditorActionListener(returnListener);
			// // end of code regarding "return key"
			//
			// return new AlertDialog.Builder(this)
			// .setIcon(android.R.drawable.ic_dialog_alert)
			// .setTitle(R.string.create_new_folder)
			// .setView(view)
			// .setPositiveButton(android.R.string.ok,
			// new OnClickListener() {
			//
			// public void onClick(DialogInterface dialog,
			// int which) {
			// createNewFolder(et.getText().toString());
			// }
			//
			// })
			// .setNegativeButton(android.R.string.cancel,
			// new OnClickListener() {
			//
			// public void onClick(DialogInterface dialog,
			// int which) {
			// // Cancel should not do anything.
			// }
			//
			// }).create();

			// case DIALOG_RENAME:
			// inflater = LayoutInflater.from(this);
			// view = inflater.inflate(R.layout.dialog_new_folder, null);
			// final EditText et2 = (EditText)
			// view.findViewById(R.id.foldername);
			// // accept "return" key
			// TextView.OnEditorActionListener returnListener2 = new
			// TextView.OnEditorActionListener() {
			// public boolean onEditorAction(TextView exampleView,
			// int actionId, KeyEvent event) {
			// if (actionId == EditorInfo.IME_NULL
			// && event.getAction() == KeyEvent.ACTION_DOWN) {
			// renameFileOrFolder(mContextFile, et2.getText()
			// .toString()); // match this behavior to your OK
			// // button
			// dismissDialog(DIALOG_RENAME);
			// }
			// return true;
			// }
			//
			// };
			// et2.setOnEditorActionListener(returnListener2);
			// // end of code regarding "return key"
			// return new AlertDialog.Builder(this)
			// .setTitle(R.string.menu_rename)
			// .setView(view)
			// .setPositiveButton(android.R.string.ok,
			// new OnClickListener() {
			//
			// public void onClick(DialogInterface dialog,
			// int which) {
			//
			// renameFileOrFolder(mContextFile, et2
			// .getText().toString());
			// }
			//
			// })
			// .setNegativeButton(android.R.string.cancel,
			// new OnClickListener() {
			//
			// public void onClick(DialogInterface dialog,
			// int which) {
			// // Cancel should not do anything.
			// }
			//
			// }).create();

		case DIALOG_ZIP:
			inflater = LayoutInflater.from(this);
			view = inflater.inflate(R.layout.dialog_new_folder, null);
			final EditText editText = (EditText) view
					.findViewById(R.id.foldername);
			// accept "return" key
			TextView.OnEditorActionListener returnListener3 = new TextView.OnEditorActionListener() {
				public boolean onEditorAction(TextView exampleView,
						int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_NULL
							&& event.getAction() == KeyEvent.ACTION_DOWN) {
						mViewPager.getFragment(mPager, mPager.getCurrentItem())
								.zipInit(editText.getText().toString());
						// if (new File(mContextFile.getParent() +
						// File.separator
						// + editText.getText().toString()).exists()) {
						// mDialogArgument = editText.getText().toString();
						// showDialog(DIALOG_WARNING_EXISTS);
						// } else {
						// new CompressManager(FileManagerActivity.this)
						// .compress(mContextFile, editText.getText()
						// .toString());
						// } // match this behavior to your OK button
						dismissDialog(DIALOG_ZIP);
					}
					return true;
				}

			};
			editText.setOnEditorActionListener(returnListener3);
			// end of code regarding "return key"
			return new AlertDialog.Builder(this)
					.setTitle(R.string.zip_dialog)
					.setView(view)
					.setPositiveButton(android.R.string.ok,
							new OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									mViewPager.getFragment(mPager,
											mPager.getCurrentItem()).zipInit(
											editText.getText().toString());
									// if (new File(mContextFile.getParent()
									// + File.separator
									// + editText.getText().toString())
									// .exists()) {
									// mDialogArgument = editText.getText()
									// .toString();
									// showDialog(DIALOG_WARNING_EXISTS);
									// } else {
									// new CompressManager(
									// FileManagerActivity.this)
									// .compress(mContextFile,
									// editText.getText()
									// .toString());
									// }
								}
							})
					.setNegativeButton(android.R.string.cancel,
							new OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// Cancel should not do anything.
								}
							}).create();

		}
		return super.onCreateDialog(id, bundle);

	}

	/**
	 * A part of the PageChangeListener interface. Not currently in use here.
	 */
	@Override
	public void onPageScrollStateChanged(int arg0) {
		// TODO Auto-generated method stub

	}

	/**
	 * A part of the PageChangeListener interface. Not currently in use here.
	 */
	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	/**
	 * A part of the PageChangeListener interface. Since there are numerous UI
	 * elements that are contextually based on the currently selected page, we
	 * need to trigger UI updates after the user swipes to a new page. The pager
	 * is contained within the main activity, but the relevant data must be
	 * pushed from the currently selected list fragment. Therefore, the takeOver
	 * method is called on the fragment, which then reaches back up and updates
	 * the action bar, etc.
	 * 
	 * @param position
	 *            the integer index of the currently selected page
	 * @return nothing
	 * 
	 */
	@Override
	public void onPageSelected(int position) {
		Log.i("MainActivity: ", "onPageSelected: " + position);
		// Toast.makeText(this, "selected " + position,
		// Toast.LENGTH_SHORT).show();
		FileListFragment fragment = mViewPager.getFragment(mPager, position);
		if (fragment != null)
			fragment.takeOver(mMainOptionsMenu);
	}

	/**
	 * Used by the Action Mode to reset which viewPager page is in focus. Useful
	 * for when an out-of-focus page list has checked items and the user presses
	 * one of the action mode buttons (only one page can have checked items at a
	 * time). Active page is set back to the one with the checked items so that
	 * there is no confusion over what selection the action mode button is
	 * operating on.
	 * 
	 * @param pageNum
	 *            the integer index of the page we want to select
	 * @return nothing
	 */
	public void setPage(int pageNum) {
		mPager.setCurrentItem(pageNum);
	}

	/**
	 * Used by the Action Mode to get the directory of the current folder view.
	 * Main activity is able to determine this, whereas the Action Mode has no
	 * way of determining this. Used with the Paste action. After a file has
	 * been copied or cut, this allows the Action Bar to enable swiping to a
	 * different file view and pasting there.
	 * 
	 * @return a file referencing the directory displayed within the currently
	 *         selected ViewPager page
	 */
	public File getCurrentDir() {
		return mViewPager.getFragment(mPager, mPager.getCurrentItem())
				.getCurrentDir();
	}

	/**
	 * Used by the Action Mode to refresh the current folder view. Main activity
	 * is able to determine this, whereas the Action Mode has no way of
	 * determining this.
	 * 
	 * @return nothing
	 */
	public void refreshCurrentPage() {
		mViewPager.getFragment(mPager, mPager.getCurrentItem()).refresh();
	}

	/**
	 * KeyDown/KeyUp code derived from
	 * http://developer.android.com/sdk/android-2.0.html (read very bottom of
	 * page)
	 * <p>
	 * The user may tap the back button, but then drag off of the button to
	 * avoid a button press
	 * 
	 * @see android.support.v4.app.FragmentActivity#onKeyDown(int,
	 *      android.view.KeyEvent)
	 * 
	 * 
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			event.startTracking();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * 
	 */
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.isTracking()
				&& !event.isCanceled()) {
			// *** DO ACTION HERE ***
			if (mViewPager.getFragment(mPager, mPager.getCurrentItem())
					.backOneLevel()) {
				return true;
			} else {
				new AlertDialog.Builder(this)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setMessage("Quit the application?")
						.setPositiveButton("Yes",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										// android.os.Debug.stopMethodTracing();
										finish();
									}

								}).setNegativeButton("No", null).show();
			}
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	public void setFirstRun(boolean firstRun) {
		this.firstRun = firstRun;
	}

	public void setNewZipName(String newZipName) {
		this.newZipName = newZipName;
	}

	public String getNewZipName() {
		return this.newZipName;
	}
}
