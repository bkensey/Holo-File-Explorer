package com.holo.fileexplorer;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.View.BaseSavedState;
import android.view.ViewGroup;
import android.widget.Toast;

public class ViewPagerAdapter extends FragmentPagerAdapter {

	private static final String TAG = "[ViewPagerAdapter] called: ";
	private int NUM_ITEMS = 2;
	private int[] scrollPosition = new int[NUM_ITEMS];
	private Context context;
	private Map<Integer, FileListFragment> mPageReferenceMap = new HashMap<Integer, FileListFragment>();
	private boolean debug = true;

	public ViewPagerAdapter(Context context, FragmentManager fm) {

		super(fm);
		
		this.context = context;
		for (int i = 0; i < NUM_ITEMS; i++) {
			scrollPosition[i] = 0;
		}
	}
	
	public void onActivityCreated(Bundle savedInstanceState) {
		getFragment(0).takeOver();
		// TODO: make this do something different on restart
	
	}
	
	// for Jake Wharton's viewpagerindicator.  may be nixed because of the onPageChangeListener interference
//	@Override
//	public String getTitle(int position) {
//		return "Tab #" + Integer.toString(position + 1);
//	}
	
	// for PagerTitleStrip
	@Override
    public CharSequence getPageTitle (int position) {
		return "Tab #" + Integer.toString(position + 1);
    }

	@Override
	public int getCount() {
		//if (debug) Log.i(TAG, "getCount");
		return NUM_ITEMS;
	}

	@Override
	public Fragment getItem(int position) {
		if (debug) Log.i(TAG, "getItem @ " + position);
		FileListFragment myFragment = FileListFragment.newInstance(position);
		mPageReferenceMap.put(position, myFragment);
		return myFragment;
	}
	
	@Override
    public void destroyItem(ViewGroup pager, int position, Object object) {
        super.destroyItem(pager, position, object);
        mPageReferenceMap.remove(position);
        if (debug) Log.i(TAG, "destroyItem @ " + position);
    }

    @Override
    public void finishUpdate(ViewGroup pager) {
        super.finishUpdate(pager);
    }

    @Override
    public Object instantiateItem(ViewGroup pager, int position) {
    	if (debug) Log.i(TAG, "instantiateItem @ " + position);
    	return super.instantiateItem(pager, position);
    }
    
//    public Object getItemReference (ViewGroup pager, int position) {
////    	if (mCurTransaction == null) {
////            mCurTransaction = mFragmentManager.beginTransaction();
////        }
//    	
//    	String name = makeFragmentName(pager.getId(), position);
//        Fragment fragment = fm.findFragmentByTag(name);
//    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
    	//if (debug) Log.i(TAG, "isViewFromObject result = " + super.isViewFromObject(view, object));
    	return super.isViewFromObject(view, object);
    }

//    @Override
//    public void restoreState(Parcelable state, ClassLoader loader) {
//        super.restoreState(state, loader);
//        if (debug) Log.i(TAG, "restoreState");
//    }
//
//    @Override
//    public Parcelable saveState() {
//    	if (debug) Log.i(TAG, "saveState");
//        return super.saveState();
//        
////    	Parcelable superState = super.saveState();
////    	SavedState savedState = new SavedState(superState);
////        //savedState.currentPage = mCurrentPage;
////        return savedState;
//    }

    @Override
    public void startUpdate(ViewGroup pager) {
        super.startUpdate(pager);
        if (debug) Log.i(TAG, "startUpdate");
    }
    
    public FileListFragment getFragment(int key) {
        return mPageReferenceMap.get(key);
    }
    
    public int getNumItems() {
    	return NUM_ITEMS;
    }
}