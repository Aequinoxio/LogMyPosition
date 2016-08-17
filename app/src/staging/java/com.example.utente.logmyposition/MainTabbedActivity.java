package com.example.utente.logmyposition;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.ToxicBakery.viewpager.transforms.CubeOutTransformer;
import com.example.utente.logmyposition.fragments.GpsSatellitesStatusFragment;
import com.example.utente.logmyposition.fragments.LogMyPositionFragment;
import com.example.utente.logmyposition.fragments.SatellitesLookFragment;
import com.example.utente.logmyposition.fragments.SimpleGpsViewFragment;

public class MainTabbedActivity extends AppCompatActivity implements SatellitesLookFragment.OnFragmentInteractionListener /*implements
       ActionBar.TabListener*/ {

    ApplicationSettings applicationSettings = ApplicationSettings.getInstance();

    private ViewPager viewPager;
    //private TabsPagerAdapter mAdapter;
    private MyPagerAdapter mAdapter;
    private ActionBar actionBar;

    private final int SETTINGS_RESULTCODE=1234;

    // Tab titles
    private String[] tabs = { "Log My Position", "Gps Status Listener", "Satellite view" };

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        Debug.stopMethodTracing();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tabbed);

     //   Debug.startMethodTracing("LogMyPosition");

        // Initilization
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        // mAdapter = new TabsPagerAdapter(getSupportFragmentManager());
        mAdapter = new MyPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(mAdapter);

        viewPager.setPageTransformer(true, new CubeOutTransformer());

//        // Sostituire con la bar nella title strip
//        actionBar = getActionBar();
//        if (actionBar!=null) {
//            actionBar.setHomeButtonEnabled(false);
//            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
//
//            // Adding Tabs
//            for (String tab_name : tabs) {
//                actionBar.addTab(actionBar.newTab().setText(tab_name)
//                        .setTabListener(this));
//            }
//        }

        /**
         * on swiping the viewpager make respective tab selected
         * */
//        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
//
//            @Override
//            public void onPageSelected(int position) {
//                // on changing the page
//                // make respected tab selected
////                actionBar.setSelectedNavigationItem(position);
//            }
//
//            @Override
//            public void onPageScrolled(int arg0, float arg1, int arg2) {
//            }
//
//            @Override
//            public void onPageScrollStateChanged(int arg0) {
//            }
//        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intentSettings= new Intent(getApplicationContext(),SimpleSettingsActivity.class);
            startActivityForResult(intentSettings, SETTINGS_RESULTCODE);

            return true;
        }

        if (id == R.id.action_about) {
            String s = getString(R.string.app_name) +" - Ver. " + BuildConfig.VERSION_NAME ;
            s+="\nby "+ getString(R.string.Autore);
            s+="\n\n"+getString(R.string.descrizione);
            new AlertDialog.Builder(MainTabbedActivity.this)
                    .setTitle(R.string.action_about)
                    .setMessage(s)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    }).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==SETTINGS_RESULTCODE)
        {
            // Ricarico le preferences
            applicationSettings.loadPreferences(getApplicationContext());

            // Aggiorno il servizio sulla base delle preferenze
            Intent intent = new Intent("AggiornaParametri");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        return;
    }

    /*
        @Override
        public void onTabSelected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {

        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {

        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {

        }
    */
    public static class MyPagerAdapter extends FragmentPagerAdapter {
        private static int NUM_ITEMS = 4;

        public MyPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: // Fragment # 0 - This will show FirstFragment different title
                    return new SimpleGpsViewFragment();

                case 1: // Fragment # 0 - This will show FirstFragment
                return new GpsSatellitesStatusFragment();

                case 2: // Fragment # 0 - This will show FirstFragment different title
                    return new SatellitesLookFragment();

                case 3: // Fragment # 0 - This will show FirstFragment different title
                return new LogMyPositionFragment();

                default:
                    return new Fragment();
            }
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            String s="";
            switch (position){
                case 0:
                    s="Simple Gps view"; break;
                case 1:
                    s="Gps Status"; break;
                case 2:
                    s="Log position"; break;
                case 3:
                    s="Satellite Position"; break;
            }
            return s;
        }

    }
}
