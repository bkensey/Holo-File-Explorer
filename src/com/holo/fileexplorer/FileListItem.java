/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.holo.fileexplorer;

import com.holo.actions.FileActionSupport;
import com.holo.fileexplorer.R;

import java.io.File;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout.Alignment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;

import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import com.google.common.base.Objects;

//import com.android.email.R;
//import com.android.emailcommon.utility.TextUtilities;
//import com.google.common.base.Objects;

/**
 * This custom View is the list item for the MessageList activity, and serves
 * two purposes: 1. It's a container to store message metadata (e.g. the ids of
 * the message, mailbox, & account) 2. It handles internal clicks such as the
 * checkbox or the favorite star
 */
public class FileListItem extends View {

	// private ThreePaneLayout mLayout;
	private FilesAdapter mAdapter;
	private FileListItemCoordinates mCoordinates;
	private Context mContext;
	private FileMeta mFileMeta;

	private boolean mDownEvent;

	public static final String MESSAGE_LIST_ITEMS_CLIP_LABEL = "com.android.email.MESSAGE_LIST_ITEMS";

	public FileListItem(Context context, FileMeta fileMeta) {
		super(context);
		mContext = context;
		mFileMeta = fileMeta;
		init(context);
	}

	public FileListItem(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public FileListItem(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	// Wide mode shows name, date, time, and favorite spread out across the
	// screen
	private static final int MODE_WIDE = FileListItemCoordinates.WIDE_MODE;
	// Sentinel indicating that the view needs layout
	public static final int NEEDS_LAYOUT = -1;

	private static boolean sInit = false;
	private static final TextPaint sDefaultPaint = new TextPaint();
	private static final TextPaint sBoldPaint = new TextPaint();
	private static final TextPaint sDatePaint = new TextPaint();
	private static Bitmap sSelectedIconOn;
	private static Bitmap sSelectedIconOff;
	private static String sDetailDateDivider;
	private static String sDetailDescription;
	private static String sDetailEmptyDescription;

	// Static colors.
	private static int DETAIL_TEXT_COLOR;
	private static int DATE_TEXT_COLOR;
	private static int FILE_NAME_TEXT_COLOR;

	public String mSender;
	public SpannableStringBuilder mText;
	public CharSequence mFileDetailText;
	private String mFileDetail;
	private CharSequence mModifiedDate;
	private StaticLayout mDetailLayout;
	public boolean mRead = false;
	public boolean mHasAttachment = false;
	public boolean mHasInvite = false;
	public boolean mIsFavorite = false;
	public boolean mHasBeenRepliedTo = false;
	public boolean mHasBeenForwarded = false;
	/** {@link Paint} for account color chips. null if no chips should be drawn. */
	public Paint mColorChipPaint;

	private int mMode = -1;

	private int mViewWidth = 0;
	private int mViewHeight = 0;

	private static int sItemHeightWide;
	private static int sItemHeightNormal;
	private static int sIconDimension;

	// Note: these cannot be shared Drawables because they are selectors which
	// have state.
	private Drawable mReadSelector;
	private Drawable mUnreadSelector;
	private Drawable mWideReadSelector;
	private Drawable mWideUnreadSelector;

	private CharSequence mFormattedFileName;
	// We must initialize this to something, in case the timestamp of the
	// message is zero (which
	// should be very rare); this is otherwise set in setTimestamp
	private CharSequence mFormattedDate = "";

	private void init(Context context) {
		mContext = context;
		if (!sInit) {
			Resources r = context.getResources();
			final TypedArray a = context.getTheme().obtainStyledAttributes(
					R.styleable.AppTheme);
			sDetailDescription = r.getString(R.string.file_detail_description)
					.concat(", ");
			sDetailEmptyDescription = r
					.getString(R.string.message_is_empty_description);
			sDetailDateDivider = r
					.getString(R.string.file_list_detail_date_divider);
			sItemHeightWide = r
					.getDimensionPixelSize(R.dimen.message_list_item_height_wide);
			sItemHeightNormal = r
					.getDimensionPixelSize(R.dimen.message_list_item_height_normal);
			sIconDimension = r
					.getDimensionPixelSize(R.dimen.file_icon_dimension);

			sDefaultPaint.setTypeface(Typeface.DEFAULT);
			sDefaultPaint.setAntiAlias(true);
			sDatePaint.setTypeface(Typeface.DEFAULT);
			sDatePaint.setAntiAlias(true);
			sBoldPaint.setTypeface(Typeface.DEFAULT_BOLD);
			sBoldPaint.setAntiAlias(true);

			sSelectedIconOff = BitmapFactory.decodeResource(r, a.getResourceId(
					R.styleable.AppTheme_buttonCheckOff,
					R.drawable.btn_check_off_normal_holo_light));
			sSelectedIconOn = BitmapFactory.decodeResource(r, a.getResourceId(
					R.styleable.AppTheme_buttonCheckOn,
					R.drawable.btn_check_on_normal_holo_light));

			// sSelectedIconOff = BitmapFactory.decodeResource(r,
			// R.drawable.btn_check_off_normal_holo_light);
			// sSelectedIconOn = BitmapFactory.decodeResource(r,
			// R.drawable.btn_check_on_normal_holo_light);

			FILE_NAME_TEXT_COLOR = a.getColor(
					R.styleable.AppTheme_listItemFileNameTextColor,
					R.color.text_test);
			DETAIL_TEXT_COLOR = a.getColor(
					R.styleable.AppTheme_listItemDetailTextColor,
					R.color.text_test);
			DATE_TEXT_COLOR = a.getColor(
					R.styleable.AppTheme_listItemDateTextColor,
					R.color.text_test);

			sInit = true;

		}
		// TODO: just move thise all to a MessageListItem.bindTo(cursor) so that
		// the fields can
		// be private, and their inter-dependence when they change can be
		// abstracted away.

		// Load the public fields in the view (for later use)
		// itemView.mMessageId = cursor.getLong(COLUMN_ID);
		// itemView.mMailboxId = cursor.getLong(COLUMN_MAILBOX_KEY);
		// final long accountId = cursor.getLong(COLUMN_ACCOUNT_KEY);
		// itemView.mAccountId = accountId;
		//
		// boolean isRead = cursor.getInt(COLUMN_READ) != 0;
		// boolean readChanged = isRead != itemView.mRead;
		// itemView.mRead = isRead;
		// itemView.mIsFavorite = cursor.getInt(COLUMN_FAVORITE) != 0;
		// final int flags = cursor.getInt(COLUMN_FLAGS);
		// itemView.mHasInvite = (flags & Message.FLAG_INCOMING_MEETING_INVITE)
		// != 0;
		// itemView.mHasBeenRepliedTo = (flags & Message.FLAG_REPLIED_TO) != 0;
		// itemView.mHasBeenForwarded = (flags & Message.FLAG_FORWARDED) != 0;
		// itemView.mHasAttachment = cursor.getInt(COLUMN_ATTACHMENTS) != 0;
		// itemView.setTimestamp(cursor.getLong(COLUMN_DATE));
		// itemView.mSender = cursor.getString(COLUMN_DISPLAY_NAME);
		// itemView.setText(
		// cursor.getString(COLUMN_DETAIL), cursor.getString(COLUMN_DATE),
		// readChanged);
		// itemView.mColorChipPaint =
		// mShowColorChips ? mResourceHelper.getAccountColorPaint(accountId) :
		// null;
		//
		// if (mQuery != null && itemView.mDate != null) {
		// itemView.mDate =
		// TextUtilities.highlightTermsInText(cursor.getString(COLUMN_DATE),
		// mQuery);
		// }

		setText(false);
		mSender = mFileMeta.mName;
		LayoutInflater mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mInflater.inflate(R.layout.file_list_item_normal, null);
	}

	public String getIdentifer() {
		// TODO Auto-generated method stub
		return mFileMeta.mPath;
	}

	public void reInit(FileMeta fileMeta) {
		mFileMeta = fileMeta;
		init(mContext);
	}

	/**
	 * Invalidate all drawing caches associated with drawing message list items.
	 * This is an expensive operation, and should be done rarely, such as when
	 * system font size changes occurs.
	 */
	public static void resetDrawingCaches() {
		FileListItemCoordinates.resetCaches();
		sInit = false;
	}

	/**
	 * Sets message detail and date safely, ensuring the cache is invalidated.
	 */
	public void setText(boolean forceUpdate) {
		boolean changed = false;
		if (!Objects.equal(mFileDetailText, mFileMeta.mDetail)) {
			mFileDetailText = mFileMeta.mDetail;
			changed = true;
			populateContentDescription();
		}

		if (!Objects.equal(mFileDetailText, mFileMeta.mPath)) {
			// mFileDetail = mFileMeta.mPath;
			if (mFileMeta.mModifiedDate != null) {
				// "parent directory" listview item will have a date of null, so
				// do this to avoid nullPointerExceptions
				mModifiedDate = android.text.format.DateFormat.format(
						"MMM d, yyyy h:mmaa", mFileMeta.mModifiedDate);
			}
			// mModifiedDate = dateFormat.format(mFileMeta.mModifiedDate);
			changed = true;
		}

		if (forceUpdate || changed
				|| (mFileDetailText == null && mFileDetailText == null) /*
																		 * first
																		 * time
																		 */) {
			SpannableStringBuilder ssb = new SpannableStringBuilder();
			boolean hasDetail = false;
			if (!TextUtils.isEmpty(mFileDetailText)) {
				SpannableString ss = new SpannableString(mFileDetailText);
				ss.setSpan(new StyleSpan(Typeface.BOLD), 0, ss.length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				ssb.append(ss);
				hasDetail = true;
			}
			// Temporary(?) switch from file location detail to date modified
			// if (!TextUtils.isEmpty(mFileDetail)) {
			// if (hasDetail) {
			// ssb.append(sDetailDateDivider);
			// }
			// ssb.append(mFileDetail);
			// }
			if (!TextUtils.isEmpty(mModifiedDate)) {
				if (hasDetail) {
					ssb.append(" " + sDetailDateDivider + " ");
				}
				ssb.append(mModifiedDate);
			}
			mText = ssb;
			requestLayout();
		}
	}

	long mTimeFormatted = 0;

	public void setTimestamp(long timestamp) {
		if (mTimeFormatted != timestamp) {
			mFormattedDate = DateUtils.getRelativeTimeSpanString(mContext,
					timestamp).toString();
			mTimeFormatted = timestamp;
		}
	}

	/**
	 * Determine the mode of this view (WIDE or NORMAL)
	 * 
	 * @param width
	 *            The width of the view
	 * @return The mode of the view
	 */
	private int getViewMode(int width) {
		return FileListItemCoordinates.getMode(mContext, width);
	}

	private Drawable mCurentBackground = null; // Only used by
												// updateBackground()

	private void updateBackground() {
		final Drawable newBackground;
		boolean isMultiPane = FileListItemCoordinates.isMultiPane(mContext);
		if (false) { // mRead
			// if (isMultiPane && mLayout.isLeftPaneVisible()) {
			// if (mWideReadSelector == null) {
			// mWideReadSelector = getContext().getResources()
			// .getDrawable(R.drawable.conversation_wide_read_selector);
			// }
			// newBackground = mWideReadSelector;
			// } else {
			if (mReadSelector == null) {
				mReadSelector = getContext().getResources().getDrawable(
						R.drawable.item_selector);
			}
			newBackground = mReadSelector;
			// }
		} else {
			// if (isMultiPane && mLayout.isLeftPaneVisible()) {
			// if (mWideUnreadSelector == null) {
			// mWideUnreadSelector = getContext().getResources().getDrawable(
			// R.drawable.conversation_wide_unread_selector);
			// }
			// newBackground = mWideUnreadSelector;
			// } else {
			if (mUnreadSelector == null) {
				mUnreadSelector = getContext().getResources().getDrawable(
						R.drawable.item_selector);
			}
			newBackground = mUnreadSelector;
			// }
		}
		if (newBackground != mCurentBackground) {
			// setBackgroundDrawable is a heavy operation. Only call it when
			// really needed.
			setBackgroundDrawable(newBackground);
			mCurentBackground = newBackground;
		}
	}

	private void calculateDetailText() {
		if (mText == null || mText.length() == 0) {
			return;
		}
		boolean hasDetail = false;
		int dateStart = 0;
		if (!TextUtils.isEmpty(mFileDetailText)) {
			int detailColor = getFontColor(DETAIL_TEXT_COLOR);
			mText.setSpan(new ForegroundColorSpan(detailColor), 0,
					mFileDetailText.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			dateStart = mFileDetailText.length() + 1;
		}
		if (!TextUtils.isEmpty(mFileDetailText)) {
			int dateColor = getFontColor(DATE_TEXT_COLOR);
			mText.setSpan(new ForegroundColorSpan(dateColor), dateStart,
					mText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
	}

	private void calculateDrawingData() {
		sDefaultPaint.setTextSize(mCoordinates.detailFontSize);
		calculateDetailText();
		// mText
		mDetailLayout = new StaticLayout(mText, sDefaultPaint,
				mCoordinates.detailWidth, Alignment.ALIGN_NORMAL, 1, 0, false /* includePad */);
		if (mCoordinates.detailLineCount < mDetailLayout.getLineCount()) {
			// TODO: ellipsize.
			int end = mDetailLayout
					.getLineEnd(mCoordinates.detailLineCount - 1);
			mDetailLayout = new StaticLayout(mText.subSequence(0, end),
					sDefaultPaint, mCoordinates.detailWidth,
					Alignment.ALIGN_NORMAL, 1, 0, true);
		}

		// Now, format the name for its width
		TextPaint namePaint = mRead ? sDefaultPaint : sBoldPaint;
		// And get the ellipsized string for the calculated width
		if (TextUtils.isEmpty(mSender)) {
			mFormattedFileName = "";
		} else {
			int nameWidth = mCoordinates.nameWidth;
			namePaint.setTextSize(mCoordinates.nameFontSize);
			namePaint.setColor(getFontColor(FILE_NAME_TEXT_COLOR));
			mFormattedFileName = TextUtils.ellipsize(mSender, namePaint,
					nameWidth, TruncateAt.END);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (widthMeasureSpec != 0 || mViewWidth == 0) {
			mViewWidth = MeasureSpec.getSize(widthMeasureSpec);
			int mode = getViewMode(mViewWidth);
			if (mode != mMode) {
				mMode = mode;
			}
			mViewHeight = measureHeight(heightMeasureSpec, mMode);
		}
		setMeasuredDimension(mViewWidth, mViewHeight);
	}

	/**
	 * Determine the height of this view
	 * 
	 * @param measureSpec
	 *            A measureSpec packed into an int
	 * @param mode
	 *            The current mode of this view
	 * @return The height of the view, honoring constraints from measureSpec
	 */
	private int measureHeight(int measureSpec, int mode) {
		int result = 0;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		if (specMode == MeasureSpec.EXACTLY) {
			// We were told how big to be
			result = specSize;
		} else {
			// Measure the text
			if (mMode == MODE_WIDE) {
				result = sItemHeightWide;
			} else {
				result = sItemHeightNormal;
			}
			if (specMode == MeasureSpec.AT_MOST) {
				// Respect AT_MOST value if that was what is called for by
				// measureSpec
				result = Math.min(result, specSize);
			}
		}
		return result;
	}

	@Override
	public void draw(Canvas canvas) {
		// Update the background, before View.draw() draws it.
		setSelected(mAdapter.isSelected(this));
		updateBackground();
		super.draw(canvas);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		mCoordinates = FileListItemCoordinates.forWidth(mContext, mViewWidth);
		calculateDrawingData();
	}

	private int getFontColor(int defaultColor) {
		// return isActivated() ? ACTIVATED_TEXT_COLOR : defaultColor;
		return defaultColor;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// Draw the color chip indicating the mailbox this belongs to
		// if (mColorChipPaint != null) {
		// canvas.drawRect(mCoordinates.chipX, mCoordinates.chipY,
		// mCoordinates.chipX + mCoordinates.chipWidth, mCoordinates.chipY
		// + mCoordinates.chipHeight, mColorChipPaint);
		// }

		// Draw the icon
		Drawable icon = FileActionSupport.getIcon(mContext, new File(
				mFileMeta.mPath));
		if (icon != null) {
			icon.setBounds(mCoordinates.iconX, mCoordinates.iconY,
					mCoordinates.iconX + sIconDimension, mCoordinates.iconY
							+ sIconDimension);
			icon.draw(canvas);
		}
		// sFileIcon = BitmapFactory.decodeResource(mContext.getResources(),
		// fileId);
		// canvas.drawBitmap(iconBitmap, mCoordinates.iconX, mCoordinates.iconY,
		// null);

		// Draw the checkbox
		canvas.drawBitmap(mAdapter.isSelected(this) ? sSelectedIconOn
				: sSelectedIconOff, mCoordinates.checkmarkX,
				mCoordinates.checkmarkY, null);

		// Draw the file name
		Paint fileNamePaint = sBoldPaint; // mRead ? sDefaultPaint : sBoldPaint
		fileNamePaint.setColor(getFontColor(FILE_NAME_TEXT_COLOR));
		fileNamePaint.setTextSize(mCoordinates.nameFontSize);
		canvas.drawText(mFormattedFileName, 0, mFormattedFileName.length(),
				mCoordinates.nameX, mCoordinates.nameY
						- mCoordinates.nameAscent, fileNamePaint);

		// Detail and date.
		sDefaultPaint.setTextSize(mCoordinates.detailFontSize);
		canvas.save();
		canvas.translate(mCoordinates.detailX, mCoordinates.detailY);
		mDetailLayout.draw(canvas);
		canvas.restore();

		// Draw the date
		sDatePaint.setTextSize(mCoordinates.dateFontSize);
		sDatePaint.setColor(DATE_TEXT_COLOR);
		int dateX = mCoordinates.dateXEnd
				- (int) sDatePaint.measureText(mFormattedDate, 0,
						mFormattedDate.length());

		canvas.drawText(mFormattedDate, 0, mFormattedDate.length(), dateX,
				mCoordinates.dateY - mCoordinates.dateAscent, sDatePaint);

		// Draw the favorite icon

		// TODO: deal with the icon layouts better from the coordinate class so
		// that this logic
		// doesn't have to exist.
		// Draw the attachment and invite icons, if necessary.
		// int iconsLeft = dateX - sBadgeMargin;
		// if (mHasAttachment) {
		// iconsLeft = iconsLeft - sAttachmentIcon.getWidth();
		// canvas.drawBitmap(sAttachmentIcon, iconsLeft,
		// mCoordinates.paperclipY, null);
		// }
		// if (mHasInvite) {
		// iconsLeft -= sInviteIcon.getWidth();
		// canvas.drawBitmap(sInviteIcon, iconsLeft, mCoordinates.paperclipY,
		// null);
		// }
	}

	public static int getIcon(File file) {

		if (!file.isFile()) // dir
		{
			if (FileActionSupport.isProtected(file)) {
				return (R.drawable.file_extension_dir_sys);

			} else if (FileActionSupport.isSdCard(file)) {
				return (R.drawable.file_extension_dir_sdcard);
			} else {
				return (R.drawable.file_extension_dir);
			}
		} else // file
		{
			String fileName = file.getName();
			if (FileActionSupport.isProtected(file)) {
				return (R.drawable.file_extension_error);

			}
			if (fileName.endsWith(".apk")) {
				return (R.drawable.file_extension_generic);
			}
			if (fileName.endsWith(".zip")) {
				return (R.drawable.file_extension_zip);
			}
			// else if(FileActionSupport.isMusic(file))
			// {
			// return (R.drawable.file_extension_generic);
			// }
			// else if(FileActionSupport.isVideo(file))
			// {
			// return (R.drawable.file_extension_mpg);
			// }
			else if (FileActionSupport.isPicture(file)) {
				return (R.drawable.file_extension_jpg);
			} else {
				return (R.drawable.file_extension_generic);
			}
		}

	}

	/**
	 * Called by the adapter at bindView() time
	 * 
	 * @param adapter
	 *            the adapter that creates this view
	 * @param layout
	 *            If this is a three pane implementation, the ThreePaneLayout.
	 *            Otherwise, null.
	 */
	public void bindViewInit(FilesAdapter adapter/* , ThreePaneLayout layout */) {
		// mLayout = layout;
		mAdapter = adapter;
		requestLayout();
	}

	private static final int TOUCH_SLOP = 24;
	private static int sScaledTouchSlop = -1;

	private void initializeSlop(Context context) {
		if (sScaledTouchSlop == -1) {
			final Resources res = context.getResources();
			final Configuration config = res.getConfiguration();
			final float density = res.getDisplayMetrics().density;
			final float sizeAndDensity;
			// TODO Pre Honeycomb devices will FC on the second condition, hence
			// the addition of the first. Not quite sure how this
			// code now treats pre-Honecomb large screen devices (like the fire)
			// but it probably isn't good. Need a way to safely determine
			// screen size.
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
					&& config
							.isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_XLARGE)) {
				sizeAndDensity = density * 1.5f;
			} else {
				sizeAndDensity = density;
			}
			sScaledTouchSlop = (int) (sizeAndDensity * TOUCH_SLOP + 0.5f);
		}
	}

	/**
	 * Overriding this method allows us to "catch" clicks in the checkbox or
	 * star and process them accordingly.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		initializeSlop(getContext());

		boolean handled = false;
		int touchX = (int) event.getX();
		// int checkRight = mCoordinates.checkmarkX
		// + mCoordinates.checkmarkWidthIncludingMargins
		// + sScaledTouchSlop;

		int checkRight = mCoordinates.checkmarkX - sScaledTouchSlop;

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (touchX > checkRight) {
				mDownEvent = true;
				if (touchX > checkRight) {
					handled = true;
				}
			}
			break;

		case MotionEvent.ACTION_CANCEL:
			mDownEvent = false;
			break;

		case MotionEvent.ACTION_UP:
			if (mDownEvent) {
				if (touchX > checkRight) {
					mAdapter.toggleSelected(this);
					handled = true;
					// } else if (touchX > starLeft) {
					// mIsFavorite = !mIsFavorite;
					// mAdapter.updateFavorite(this, mIsFavorite);
					// handled = true;
				}
			}
			break;
		}

		if (handled) {
			invalidate();
		} else {
			handled = super.onTouchEvent(event);
		}

		return handled;
	}

	@Override
	public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
		event.setClassName(getClass().getName());
		event.setPackageName(getContext().getPackageName());
		event.setEnabled(true);
		event.setContentDescription(getContentDescription());
		return true;
	}

	/**
	 * Sets the content description for this item, used for accessibility.
	 */
	private void populateContentDescription() {
		if (!TextUtils.isEmpty(mFileDetailText)) {
			setContentDescription(sDetailDescription + mFileDetailText);
		} else {
			setContentDescription(sDetailEmptyDescription);
		}
	}
}
