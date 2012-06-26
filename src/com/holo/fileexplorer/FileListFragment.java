package com.holo.fileexplorer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;

import com.beacon.crawlers.Mover;
import com.beacon.crawlers.Trasher;
import com.holo.actions.FileActionSupport;
import com.holo.actions.FileActions;
import com.holo.fileexplorer.R;
import com.holo.root.ExtendedMount;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.RootToolsException;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.widget.ShareActionProvider;
import android.text.TextUtils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import com.actionbarsherlock.view.ActionMode;
import android.view.LayoutInflater;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

public class FileListFragment extends SherlockListFragment implements
		OnNavigationListener, OnItemLongClickListener, FilesAdapter.Callback {

	// Sort Constants
	public final static int SORT_ALPHABETICAL = 1;
	public final static int SORT_LAST_MODIFIED = 2;
	public final static int SORT_ASC = 100;
	public final static int SORT_DESC = 101;

	private int currentSortType = SORT_ALPHABETICAL;
	private boolean sortReversed = false;

	final Comparator<FileMeta> LAST_MODIFIED_COMPARATOR = new Comparator<FileMeta>() {
		public int compare(FileMeta f1, FileMeta f2) {
			int comparatorResult;
			if (!sortReversed) {
				// For when the 'sort reversed' option is unchecked. Last
				// Modified scheme should go Newest-Oldest by default
				comparatorResult = f2.mModifiedDate.compareTo(f1.mModifiedDate);
			} else {
				comparatorResult = f1.mModifiedDate.compareTo(f2.mModifiedDate);
			}
			return comparatorResult;
		}
	};

	final Comparator<FileMeta> ALPHABETICAL_COMPARATOR = new Comparator<FileMeta>() {
		public int compare(FileMeta f1, FileMeta f2) {

			int comparatorResult;
			if (!sortReversed) {
				// For when the 'sort reversed' option is unchecked.
				// Alphabetical scheme should go A-Z by default
				comparatorResult = f1.mLowerCaseName
						.compareTo(f2.mLowerCaseName);
			} else {
				comparatorResult = f2.mLowerCaseName
						.compareTo(f1.mLowerCaseName);
			}
			return comparatorResult;
		}
	};

	// UI Support
	private MainActivity mActivity;
	private SharedPreferences mPrefs;
	private Callback mCallback = EmptyCallback.INSTANCE;
	private boolean mIsViewCreated;

	// List variables
	private int mNum;
	private String mPath;
	private List<FileMeta> mFileItems;
	private List<FileMeta> dir;
	private List<FileMeta> fls;
	private ArrayList<String> mNavItems;
	private ArrayList<String> mNavTitles;
	private ArrayList<String> mNavPaths;
	private Stack<String> mHistory = new Stack<String>();
	private Stack<Integer> mHistoryScroll = new Stack<Integer>();
	private Stack<Integer> mHistoryScrollTop = new Stack<Integer>();

	// Navigation dropdown variables
	private int mNavSelected;
	private boolean mPauseNavSelect = false;

	// Misc Variables
	final String root = "/";
	private FilesAdapter mListAdapter;

	private boolean isSystemDir = false;

	/**
	 * If true, we disable the CAB even if there are selected messages. It's
	 * used in portrait on the tablet when the message view becomes visible and
	 * the message list gets pushed out of the screen, in which case we want to
	 * keep the selection but the CAB should be gone.
	 */
	private boolean mDisableCab;

	/**
	 * {@link ActionMode} shown when 1 or more message is selected.
	 */
	private ActionMode mSelectionMode;
	private SelectionModeCallback mLastSelectionModeCallback;

	/**
	 * Callback interface that owning activities must implement (future tablet
	 * stuff pulled from the ICS email app source)
	 */
	public interface Callback {
		public static final int TYPE_REGULAR = 0;
		public static final int TYPE_DRAFT = 1;
		public static final int TYPE_TRASH = 2;

		/**
		 * Called when an operation is initiated that can potentially advance
		 * the current message selection (e.g. a delete operation may advance
		 * the selection).
		 * 
		 * @param affectedMessages
		 *            the messages the operation will apply to
		 */
		public void onAdvancingOpAccepted(Set<Long> affectedFiles);

		/**
		 * Called when a drag & drop is initiated.
		 * 
		 * @return true if drag & drop is allowed
		 */
		public boolean onDragStarted();

		/**
		 * Called when a drag & drop is ended.
		 */
		public void onDragEnded();
	}

	/**
	 * This class's actual implementation of the Callback Interface. (future
	 * tablet stuff pulled from the ICS email app source)
	 */
	private static final class EmptyCallback implements Callback {
		public static final Callback INSTANCE = new EmptyCallback();

		@Override
		public void onAdvancingOpAccepted(Set<Long> affectedMessages) {
		}

		@Override
		public boolean onDragStarted() {
			return false; // We don't know -- err on the safe side.
		}

		@Override
		public void onDragEnded() {
		}
	}

	/**
	 * Create a new instance of FileListFragment, providing "num" as an
	 * argument.
	 */
	static FileListFragment newInstance(int num) {
		FileListFragment f = new FileListFragment();

		// Supply num input as an argument.
		Bundle args = new Bundle();
		args.putInt("num", num);
		f.setArguments(args);
		return f;
	}

	/**
	 * When creating, retrieve this instance's number from its arguments.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mNum = getArguments() != null ? getArguments().getInt("num") : 1;
		mActivity = (MainActivity) getActivity();
		mPrefs = mActivity.getSharedPreferences("preference_headers",
				Context.MODE_WORLD_READABLE);
		mFileItems = new ArrayList<FileMeta>();
		// TODO mnum not set
		Log.i("FragmentList", "Fragment #" + mNum + ": onCreate.");
	}

	/**
	 * The Fragment's UI is just a list fragment
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_pager_list, container,
				false);
		mIsViewCreated = true;
		mPath = mPrefs.getString("home_folder", "/sdcard");
		return v;
	}

	/**
	 * @return true if the content view is created and not destroyed yet. (i.e.
	 *         between {@link #onCreateView} and {@link #onDestroyView}.
	 */
	private boolean isViewCreated() {
		// Note that we don't use "getView() != null". This method is used in
		// updateSelectionMode()
		// to determine if CAB shold be shown. But because it's called from
		// onDestroyView(), at
		// this point the fragment still has views but we want to hide CAB, we
		// can't use getView() here.
		return mIsViewCreated;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// ^ Called when the activity's onCreate() method has returned.
		super.onActivityCreated(savedInstanceState);

		// getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		final ListView lv = getListView();
		lv.setOnItemLongClickListener(this);
		lv.setItemsCanFocus(false);
		lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		Log.i("FragmentList", "Fragment #" + mNum
				+ ": onActivityCreated. mPath set to " + mPath);

		fill(mPath);

		if (mActivity.firstRun)
			updateNav();
		mActivity.setFirstRun(false);

		// Restore path
		// if (savedInstanceState != null) {
		// // Restore last state for checked position.
		// mPath = savedInstanceState.getString("path");
		// mNavItems = savedInstanceState.getStringArrayList("navitems");
		// mNavSelected = savedInstanceState.getInt("navselected");
		// } else {
		// mPath = root;
		// }
		if (savedInstanceState != null) {
			// Fragment doesn't have this method. Call it manually.
			restoreInstanceState(savedInstanceState);
		}
	}

	public void onResume() {
		super.onResume();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("path", mPath);
		outState.putInt("navselected", mNavSelected);
		outState.putStringArrayList("navitems", mNavItems);
		Log.i("FragmentList", "Fragment #" + mNum + " instance state path = "
				+ mPath);
	}

	void restoreInstanceState(Bundle savedInstanceState) {
		mPath = savedInstanceState.getString("path");
		mNavSelected = savedInstanceState.getInt("navselected");
		mNavItems = savedInstanceState.getStringArrayList("navitems");
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		FileMeta f = mListAdapter.getItem(position);
		if (f.isDirectory) {

			// Cancel action mode unless user has cut or copied a selection and
			// is navigating up to paste it.
			if (!FileActions.canPaste()) {
				onDeselectAll();
			}
			pushScrollHistory();

			// Default current sort to alphabetical for newly opened folders
			currentSortType = SORT_ALPHABETICAL;
			fill(f.mPath);

			// Reset scroll position to the top for newly opened folders
			this.getListView().setSelectionFromTop(0, 0);

			// Make sure that the navigation drop-down doesn't fire on its first
			// new item after it gets repopulated
			mPauseNavSelect = true;
			updateNav();
		} else {
			onFileClick(f);
		}
	}

	private void pushScrollHistory() {
		// Remember current location in file structure
		mHistory.push(mPath);
		// Remember top listview item position
		mHistoryScroll.push(getListView().getFirstVisiblePosition());
		// Grab precise scroll position on top-most listview item
		View topView = getListView().getChildAt(0);
		int top = (topView == null) ? 0 : topView.getTop();
		mHistoryScrollTop.push(top);
	}

	private void onFileClick(FileMeta fm) {
		File file = new File(fm.mPath);
		FileActions.openFile(mActivity, file);
		// Intent fileIntent = new Intent(mActivity, FileView.class);
		// fileIntent.putExtra("absPath", fm.getPath());
		// startActivity(fileIntent);
		// Toast.makeText(mActivity, "File Clicked: " + fm.getName(),
		// Toast.LENGTH_SHORT).show();
	}

	private void changeDir(String destination) {
		fill(destination);
		updateNav();
	}

	private void fill(String f) {
		mPath = f;

		// for (ExtendedMount mount : mounts) {
		//
		// }

		if (mActivity.rootEnabled && !FileActionSupport.isReadable(mPath)) {
			if (mPath != "/")
				mPath += "/";

			List<String> output = null;
			String[] commands = new String[] {"ls -la " + mPath};
			try {
				output = RootTools.sendShell(commands, 0, 4000);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (RootToolsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// Process string output and populate FileMeta objects
			dir = new ArrayList<FileMeta>();
			fls = new ArrayList<FileMeta>();
			Iterator<String> iterator = output.iterator();

			while (iterator.hasNext()) {
				String[] fields = iterator.next().split("\\s+");
				// Don't try to handle last line of output. Will be something
				// like "sh-4.1$"
				if (fields.length == 1)
					break;
				// test to see if file size is present, indicats file/folder
				// status
				if (!FileActionSupport.isNumeric(fields[3])) {
					dir.add(new FileMeta(mActivity, fields[0], // permissions
							fields[1], // ??
							fields[2], // ??
							null, // number of children (not implemented)
							fields[3], // date modifed
							fields[4], // time modified
							fields[5], // filename
							mPath // parent path
					));
				} else {
					fls.add(new FileMeta(mActivity, fields[0], // permissions
							fields[1], // ??
							fields[2], // ??
							fields[3], // file size in bytes
							fields[4], // date modifed
							fields[5], // time modified
							fields[6], // filename
							mPath // parent path
					));
				}
			}
		} else {
			File currentDir = new File(f);
			File[] dirs = currentDir.listFiles();
			dir = new ArrayList<FileMeta>();
			fls = new ArrayList<FileMeta>();
			try {
				for (File ff : dirs) {
					if (ff.isDirectory())
						dir.add(new FileMeta(mActivity, ff));
					else {
						fls.add(new FileMeta(mActivity, ff));
					}
				}
			} catch (Exception e) {

			}
		}
		sort();
	}

	public void sort() {
		sort(currentSortType);
	}

	public void sort(int sortCode) {
		switch (sortCode) {
		case SORT_ALPHABETICAL:
			Collections.sort(dir, ALPHABETICAL_COMPARATOR);
			Collections.sort(fls, ALPHABETICAL_COMPARATOR);
			currentSortType = sortCode;
			break;
		case SORT_LAST_MODIFIED:
			Collections.sort(dir, LAST_MODIFIED_COMPARATOR);
			Collections.sort(fls, LAST_MODIFIED_COMPARATOR);
			currentSortType = sortCode;
			break;
		default:
			// Default to the generic alphabetical
			Collections.sort(dir, ALPHABETICAL_COMPARATOR);
			Collections.sort(fls, ALPHABETICAL_COMPARATOR);
			currentSortType = sortCode;
			break;
		}
		// Collections.sort(dir, currentComparator);
		// Collections.sort(fls, currentComparator);
		// if (!currentDir.getPath().equalsIgnoreCase("/"))
		// dir.add(0, new FileMeta(mActivity, "..", "Parent Directory",
		// currentDir.getParent()));
		mFileItems.clear();
		mFileItems.addAll(dir);
		mFileItems.addAll(fls);
		if (mListAdapter == null) {
			mListAdapter = new FilesAdapter(mActivity, this,
					R.layout.file_list_item_normal, mFileItems);
			this.setListAdapter(mListAdapter);
		} else {
			mListAdapter.notifyDataSetChanged();
		}
	}

	public void sortDirection(boolean newState) {
		sortReversed = newState;
		sort();
	}

	public File getCurrentDir() {
		return new File(mPath);
	}

	public void refresh() {
		// TODO preserve scroll state
		Set<String> selectedConversations = mListAdapter.getSelectedSet();
		fill(mPath);
		mListAdapter.setSelectedSet(selectedConversations);
	}

	// In addition to triggering the nav dropdown update from takeOver(), this
	// method updates the submenus for the sort action to reflect the currently
	// selected sort mode for this fragment
	public void takeOver(Menu mMainOptionsMenu) {
		int id = 0;
		switch (currentSortType) {
		case SORT_ALPHABETICAL:
			id = R.id.sort_alphabetical;
			break;
		case SORT_LAST_MODIFIED:
			id = R.id.sort_last_modified;
			break;
		default:
			id = R.id.sort_alphabetical;
		}

		// Take control of sort menu item
		MenuItem item = (MenuItem) mMainOptionsMenu.findItem(id);
		item.setChecked(true);

		MenuItem sortReverseMenuItem = (MenuItem) mMainOptionsMenu
				.findItem(R.id.sort_reverse);
		sortReverseMenuItem.setChecked(sortReversed);

		takeOver();
	}

	// Used when called from the ViewPagerAdapter on initialization, and by the
	// overloaded takeOver method here
	public void takeOver() {
		mPauseNavSelect = true;
		Log.i("FragmentList", "Fragment #" + mNum + ":takeOver");
		updateNav();
	}

	private void updateNav() {
		Log.i("FragmentList", "Fragment #" + mNum + ":updateNav");
		ActionBar bar = mActivity.getSupportActionBar();
		bar.setDisplayShowTitleEnabled(false);
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		String middle = "";
		mNavTitles = new ArrayList<String>();
		mNavPaths = new ArrayList<String>();
		File f = new File(mPath);
		String split = f.getPath();
		String[] tokens = split.split("/");
		if (tokens.length > 0) {
			for (int x = 0; x < tokens.length; x++) {
				mNavTitles.add(tokens[x] + "/");
				mNavPaths.add(middle + tokens[x] + "/");
				middle = middle + tokens[x] + "/";
			}
		} else {
			mNavTitles.add("/      ");
			mNavPaths.add("/");
		}

		mNavSelected = mNavTitles.size() - 1; // set last item as selected
												// (bears
												// improvement)

		// Context testContext = bar.getThemedContext();
		SpinnerAdapter mSpinnerAdapter = new ArrayAdapter<String>(mActivity,
				R.layout.nav_spinner_item, mNavTitles);
		bar.setListNavigationCallbacks(mSpinnerAdapter, this);
		bar.setSelectedNavigationItem(mNavSelected);
		Log.i("FragmentList", "Fragment #" + mNum + ":Nav item just clicked? ^");
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		if (mPauseNavSelect) {
			Log.i("FragmentList", "Fragment #" + mNum
					+ ":Nav Item click intercepted");
			mPauseNavSelect = false;
			return false;
		} else {
			Log.i("FragmentList", "Fragment #" + mNum + ":Nav Item clicked: "
					+ itemId);
			pushScrollHistory();
			fill(mNavPaths.get(itemPosition));
			return true;
		}
	}

	public void setCallback(Callback callback) {
		mCallback = (callback != null) ? callback : EmptyCallback.INSTANCE;
	}

	/**
	 * Show/hide the "selection" action mode, according to the number of
	 * selected messages and the visibility of the fragment. Also update the
	 * content (title and menus) if necessary.
	 */
	public void updateSelectionMode() {
		final int numSelected = getSelectedCount();
		if ((numSelected == 0) || mDisableCab || !isViewCreated()) {
			finishSelectionMode();
			return;
		}
		if (isInSelectionMode()) {
			updateSelectionModeView();
		} else {
			mLastSelectionModeCallback = new SelectionModeCallback();
			mActivity.startActionMode(mLastSelectionModeCallback);
		}
	}

	/**
	 * Finish the "selection" action mode.
	 * 
	 * Note this method finishes the contextual mode, but does *not* clear the
	 * selection. If you want to do so use {@link #onDeselectAll()} instead.
	 */
	private void finishSelectionMode() {
		if (isInSelectionMode()) {
			mLastSelectionModeCallback.mClosedByUser = false;
			mSelectionMode.finish();
		}
	}

	/** Update the "selection" action mode bar */
	private void updateSelectionModeView() {
		mSelectionMode.invalidate();
	}

	/**
	 * @return the number of messages that are currently selected.
	 */
	private int getSelectedCount() {
		return mListAdapter.getSelectedSet().size();
	}

	/**
	 * @return true if the list is in the "selection" mode.
	 */
	public boolean isInSelectionMode() {
		return mSelectionMode != null;
	}

	public void onDeselectAll() {
		mListAdapter.clearSelection();
		if (isInSelectionMode()) {
			finishSelectionMode();
		}
	}

	/** Implements {@link MessagesAdapter.Callback} */
	@Override
	public void onAdapterFavoriteChanged(FileListItem itemView,
			boolean newFavorite) {
		// mController.setMessageFavorite(itemView.mMessageId, newFavorite);
	}

	/** Implements {@link MessagesAdapter.Callback} */
	@Override
	public void onAdapterSelectedChanged(FileListItem itemView,
			boolean newSelected, int mSelectedCount) {
		updateSelectionMode();
	}

	private class SelectionModeCallback implements ActionMode.Callback {
		private MenuItem mCut;
		private MenuItem mCopy;
		private MenuItem mPaste;
		private MenuItem mArchive;
		private MenuItem mDelete;
		private MenuItem mShare;
		private MenuItem mRename;
		private MenuItem mDetails;
		private int viewPageNum;
		private ShareActionProvider mShareActionProvider;

		private boolean pasteReady = false;

		/* package */boolean mClosedByUser = true;

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mSelectionMode = mode;

			MenuInflater inflater = mActivity.getSupportMenuInflater();
			inflater.inflate(R.menu.message_list_cab, menu);
			mCut = menu.findItem(R.id.action_cut);
			mCopy = menu.findItem(R.id.action_copy);
			mPaste = menu.findItem(R.id.action_paste);
			mArchive = menu.findItem(R.id.action_zip);
			mDelete = menu.findItem(R.id.action_confirm_delete);
			mShare = menu.findItem(R.id.action_share);
			mRename = menu.findItem(R.id.action_rename);
			mDetails = menu.findItem(R.id.action_details);

			// Set file with share history to the provider and set the share
			// intent.
			mShareActionProvider = (ShareActionProvider) mShare
					.getActionProvider();
			mShareActionProvider
					.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);

			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			int num = getSelectedCount();
			// Set title -- "# selected"
			mSelectionMode.setTitle(mActivity.getResources().getQuantityString(
					R.plurals.message_view_selected_message_count, num, num));

			// Show appropriate menu items.
			if (!FileActions.canPaste()) {
				mCut.setVisible(true);
				mCopy.setVisible(true);
				mPaste.setVisible(false);
				mArchive.setVisible(true);
				mShare.setVisible(true);
			} else {
				mCut.setVisible(true);
				mCopy.setVisible(true);
				mPaste.setVisible(true);
				mArchive.setVisible(true);
				mShare.setVisible(true);
			}

			if (num == 1) {
				mRename.setVisible(true);
				mDetails.setVisible(true);
			} else {
				mRename.setVisible(false);
				mDetails.setVisible(false);
			}
			viewPageNum = mNum;
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			Set<String> selectedConversations = mListAdapter.getSelectedSet();
			Object[] selectedFilePaths = mListAdapter.getSelectedSet()
					.toArray();

			/*
			 * Paste gets special handling. All other Contextual Action Mode
			 * actions go in a switch statement below.
			 */

			// Paste clicked and files are available for pasting
			if (FileActions.canPaste() && item.getItemId() == R.id.action_paste) {
				new Mover(mActivity, FileActions.getPasteMode())
						.execute(mActivity.getCurrentDir());
				/*
				 * !!! TODO: Mover.java refreshes the directory after finishing,
				 * which keeps the following from running
				 * 
				 * Test to see if there is any current selection: If there
				 * aren't any selections, then the user's selection of paste can
				 * be presumed to be the final action for the Action Mode.
				 * Otherwise the action mode should be left open and
				 * invalidated/reloaded to remove the paste action.
				 */
				if (selectedFilePaths.length < 1) {
					onDeselectAll(); // Close Action Mode
				} else {
					mSelectionMode.invalidate(); // Update Action Mode
				}

				// Paste clicked but no files available. Should never happen.
			} else if (!FileActions.canPaste()
					&& item.getItemId() == R.id.action_paste) {
				Toast.makeText(mActivity, "No files stored in clipboard",
						Toast.LENGTH_SHORT).show();

				// No items selected and we've already determined that there is
				// no pending paste to keep the action mode open. Should never
				// happen.
			} else if (selectedFilePaths.length < 1) {
				Toast.makeText(
						mActivity,
						"Why is this action mode still open? Because it shouldn't be.",
						Toast.LENGTH_SHORT).show();

				// By process of elimination, we have established that paste has
				// not been clicked and that there is a selection to be operated
				// on. Get to it!
			} else {
				// Convert selected items into a list of files
				ArrayList<File> selectedFiles = new ArrayList<File>();
				for (Object s : selectedFilePaths) {
					selectedFiles.add(new File((String) s));
				}

				// Trigger appropriate CAB item code
				switch (item.getItemId()) {
				case R.id.action_share:
					mActivity.setPage(viewPageNum);
					if (selectedFiles.size() > 0) {
						Intent shareIntent = FileActions.createShareIntent(
								selectedFiles, mActivity);
						startActivity(Intent.createChooser(shareIntent,
								"Share via"));
						// Commented out pending solution for
						// shareactionprovider/splitactionbar bug
						// mShareActionProvider.setShareIntent(FileActions
						// .createShareIntent(selectedFiles, mActivity));
						// mSelectionMode.invalidate();
					} else {
						Toast.makeText(mActivity, "Error. No items selected.",
								Toast.LENGTH_SHORT).show();
					}
					onDeselectAll();
					break;
				case R.id.action_rename:
					mActivity.setPage(viewPageNum);
					Toast.makeText(mActivity, "Rename isn't implemented yet",
							Toast.LENGTH_SHORT).show();
					break;
				case R.id.action_details:
					mActivity.setPage(viewPageNum);
					Intent fileIntent = new Intent(mActivity, FileView.class);
					fileIntent.putExtra("absPath", selectedFiles.get(0)
							.getPath());
					startActivity(fileIntent);
					break;
				case R.id.action_zip:
					mActivity.setPage(viewPageNum);
					mActivity.showDialog(mActivity.DIALOG_ZIP);
					// Problem here. Zipfiles needs to run after dialog returns.
					// FileActions.zipFiles(selectedFiles, getCurrentDir(),
					// mActivity.getNewZipName(), mActivity);
					break;

				case R.id.action_cut:
					mActivity.setPage(viewPageNum);
					if (selectedFiles.size() > 0) {
						FileActions.cutFiles(selectedFiles, mActivity);
						mSelectionMode.invalidate(); // After cutting, the menu
														// should be rebuilt to
														// include paste
					} else {
						Toast.makeText(mActivity, "Error. No items selected.",
								Toast.LENGTH_SHORT).show();
					}
					break;

				case R.id.action_copy:
					mActivity.setPage(viewPageNum);
					if (selectedFiles.size() > 0) {
						FileActions.copyFiles(selectedFiles, mActivity);
						mSelectionMode.invalidate(); // After copying, the menu
														// should be rebuilt to
														// include paste
					} else {
						Toast.makeText(mActivity, "Error. No items selected.",
								Toast.LENGTH_SHORT).show();
					}
					break;

				case R.id.action_confirm_delete:
					// new
					// AlertDialog.Builder(mActivity).setIcon(android.R.drawable.ic_dialog_alert).setMessage("Quit the application?")
					// .setPositiveButton("Yes", new
					// DialogInterface.OnClickListener() {
					//
					// @Override
					// public void onClick(DialogInterface dialog, int which) {
					// mActivity.setPage(viewPageNum);
					// new Trasher(mActivity).execute(selectedFiles);
					// // Toast.makeText(mActivity,
					// // "Deleted " + getSelectedCount() + " items",
					// // Toast.LENGTH_SHORT).show();
					// onDeselectAll();
					// }
					//
					// }).setNegativeButton("No", null).show();

					mActivity.setPage(viewPageNum);
					new Trasher(mActivity).execute(selectedFiles);
					// Toast.makeText(mActivity,
					// "Deleted " + getSelectedCount() + " items",
					// Toast.LENGTH_SHORT).show();
					onDeselectAll();

					break;

				default:
					break;
				}
			}
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			// Clear this before onDeselectAll() to prevent onDeselectAll() from
			// trying to close the
			// contextual mode again.
			mSelectionMode = null;
			if (mClosedByUser) {
				// Clear selection, only when the contextual mode is explicitly
				// closed by the user.
				//
				// We close the contextual mode when the fragment becomes
				// temporary invisible
				// (i.e. mIsVisible == false) too, in which case we want to keep
				// the selection.
				onDeselectAll();
			}
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		// FileListItem f = mListAdapter.getItem(position);
		boolean toggled = false;
		if (!mListAdapter.isSelected((FileListItem) view)) {
			toggleSelection((FileListItem) view);
			toggled = true;
			updateSelectionMode();
		}
		return true;
	}

	private void toggleSelection(FileListItem itemView) {
		itemView.invalidate();
		mListAdapter.toggleSelected(itemView);
	}

	public boolean atRoot() {
		return mPath.equals(root);
	}

	public void upOneLevel() {
		if (mPath != root)
			changeDir(new File(mPath).getParent());
	}

	public boolean backOneLevel() {
		if (mHistory.empty()) {
			return false;
		} else {
			mPauseNavSelect = true;
			changeDir(mHistory.pop());
			this.getListView().setSelectionFromTop(mHistoryScroll.pop(),
					mHistoryScrollTop.pop());
			return true;
		}
	}

	public void createNewFolder(String foldername) {
		if (!TextUtils.isEmpty(foldername)) {
			File file = FileUtils.getFile(mPath, foldername);
			if (file.mkdirs()) {

				// Change into new directory:
				changeDir(file.getAbsolutePath());
			} else {
				Toast.makeText(mActivity, R.string.error_creating_new_folder,
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	public void zipInit(String newZipName) {

		// Convert selected items into a list of files
		Object[] selectedFilePaths = mListAdapter.getSelectedSet().toArray();
		ArrayList<File> selectedFiles = new ArrayList<File>();
		for (Object s : selectedFilePaths) {
			selectedFiles.add(new File((String) s));
		}

		FileActions.zipFiles(selectedFiles, getCurrentDir(), newZipName,
				mActivity);
		onDeselectAll();
		refresh();
	}
}