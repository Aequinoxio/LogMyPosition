package com.example.utente.logmyposition;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.example.utente.logmyposition.fragments.GpsSatellitesStatusFragment;
import com.example.utente.logmyposition.fragments.LogMyPositionFragment;

/**
 * Created by utente on 14/07/2016.
 * See: http://www.androidhive.info/2013/10/android-tab-layout-with-swipeable-views-1/
 */
public class TabsPagerAdapter extends FragmentPagerAdapter {

    public TabsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int index) {

        switch (index) {
            case 0:
                // Top Rated fragment activity
                return new GpsSatellitesStatusFragment();
            case 1:
                // Games fragment activity
                return new LogMyPositionFragment();
            case 2:
                // Movies fragment activity
                //return new MoviesFragment();
        }

        return null;
    }

    @Override
    public int getCount() {
        // get item count - equal to number of tabs
        return 3;
    }

}