package com.example.utente.logmyposition.fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebHistoryItem;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.utente.logmyposition.ApplicationSettings;
import com.example.utente.logmyposition.R;
import com.example.utente.logmyposition.util.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;

public class GpsSatellitesStatusFragment extends Fragment implements GpsStatus.Listener{

    private Context mContext=null;
    private Activity mActivity=null;
    private View rootView=null;
    private ListView listViewSatellites=null;

    private LocationManager locationManager;
    private MyLocationListener locationListener;
    private GpsStatusAdapter gpsStatusAdapter;
    private ApplicationSettings applicationSettings = ApplicationSettings.getInstance();

    private GpsInfo[] mGpsInfo;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_gps_satellites_status, container, false);

        if (mActivity==null){
            mActivity=getActivity();
            mContext=getActivity().getApplicationContext();
        }

        mGpsInfo=new GpsInfo[255];

        // Inizializzo le info sui satelliti
        for (int i=0;i<255;i++){
            mGpsInfo[i]=new GpsInfo();
        }

        locationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
        locationManager.addGpsStatusListener(this);

        LocationProvider locationProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
        locationListener = new MyLocationListener();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                0,
                0.0f,
                locationListener);

        listViewSatellites = (ListView)rootView.findViewById(R.id.listViewStatus);
        gpsStatusAdapter= new GpsStatusAdapter(mContext,mGpsInfo);
        listViewSatellites.setAdapter(gpsStatusAdapter);

        this.rootView=rootView;

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        locationManager.removeUpdates(locationListener);
        locationManager.removeGpsStatusListener(this);
    }

    @Override
    public void onGpsStatusChanged(int event) {
        String s="";
        switch (event){
            case GpsStatus.GPS_EVENT_FIRST_FIX : s="GPS_EVENT_FIRST_FIX"; break;
            case GpsStatus.GPS_EVENT_STARTED : s="GPS_EVENT_STARTED"; applicationSettings.setGPSAvailable(true); break;
            case GpsStatus.GPS_EVENT_STOPPED : s="GPS_EVENT_STOPPED"; applicationSettings.setGPSAvailable(false);
                turnOffAllUI();
                break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS : s="GPS_EVENT_SATELLITE_STATUS";
                break;
        }

        TextView textView = (TextView) rootView.findViewById(R.id.txtGpsStatus);
        textView.setText(s);

        GpsStatus gs = locationManager.getGpsStatus(null);

        final Iterator<GpsSatellite> it = gs.getSatellites().iterator();
        GpsSatellite gpsSatellite;

        textView = (TextView) rootView.findViewById(R.id.txtFixTime);
        textView.setText(String.valueOf(gs.getTimeToFirstFix()));

        int maxSat=0, currSat=0, fixSat=0;
        int[] SatType={0,0,0,0,0};
        int[] SatTypeFix={0,0,0,0,0};
        int SatTypeIndex=0;
        int satellitePRN;

        // Resetto lo stato GPS per mostrare quello attuale;
        for (int i=0;i<255;i++){
            mGpsInfo[i].resetAll();
        }

        while (it.hasNext()) {
            gpsSatellite=it.next();

            switch (GpsSatelliteType.getSatelliteType(gpsSatellite.getPrn())){
                case BEIDOU:
                    SatTypeIndex=0;
                    break;
                case GLONASS:
                    SatTypeIndex=1;
                    break;
                case  GPS:
                    SatTypeIndex=2;
                    break;
                case QZSS:
                    SatTypeIndex=3;
                    break;
                case SBAS:
                    SatTypeIndex=4;
                    break;
                default:
                    SatTypeIndex=-1;
            }

            if(gpsSatellite.usedInFix()){
                fixSat++;
            }
            maxSat++;

            satellitePRN=gpsSatellite.getPrn();

            if (gpsSatellite.getSnr()>0){
                currSat++;
            }

            if (SatTypeIndex>=0){
                SatType[SatTypeIndex]++;
                if (gpsSatellite.usedInFix()){
                    SatTypeFix[SatTypeIndex]++;
                }
            }
            mGpsInfo[satellitePRN].updateValues(gpsSatellite);
        }

///////////////////// Aggiorno l'interfaccia

        textView = (TextView) rootView.findViewById(R.id.txtSatBE);
        textView.setText(String.format("%d (%d)",SatType[0],SatTypeFix[0]));
        textView = (TextView) rootView.findViewById(R.id.txtSatGLO);
        textView.setText(String.format("%d (%d)",SatType[1],SatTypeFix[1]));
        textView = (TextView) rootView.findViewById(R.id.txtSatGPS);
        textView.setText(String.format("%d (%d)",SatType[2],SatTypeFix[2]));
        textView = (TextView) rootView.findViewById(R.id.txtSatQZ);
        textView.setText(String.format("%d (%d)",SatType[3],SatTypeFix[3]));
        textView = (TextView) rootView.findViewById(R.id.txtSatSB);
        textView.setText(String.format("%d (%d)",SatType[4],SatTypeFix[4]));

        textView = (TextView)rootView.findViewById(R.id.txtSatCurrMax);
        textView.setText(String.valueOf(maxSat)+" / "+String.valueOf(currSat)+" ("+String.valueOf(fixSat)+")");

//        int index = listViewSatellites.getFirstVisiblePosition();
//        View v = listViewSatellites.getChildAt(0);
//        int top = (v == null) ? 0 : v.getTop();

        // TODO: vedere perchè aggiornando la listview, si resetta la posizione a quella iniziale
//        gpsStatusAdapter.setNotifyOnChange(false);
        gpsStatusAdapter.notifyDataSetChanged();

//        listViewSatellites.setSelectionFromTop(index,top);
//        int k= (int)(Math.random()*10);
//
//        for (int i1=0;i1<30;i1++){
//            satelliteSNR=i1%k;
//            satellitePRN=i1;
//
//            generateAndPlaceTextView(currId+satellitePRN,currId1+satellitePRN,satelliteSNR, satellitePRN);
//
//            currId1++;
//            currId++;
//        }
    }

    private void turnOffAllUI(){
        TextView textView = (TextView) rootView.findViewById(R.id.txtSatCurrMax);
        textView.setText(R.string.NoValue);

        textView = (TextView) rootView.findViewById(R.id.txtFixTime);
        textView.setText(R.string.NoValue);

        textView = (TextView) rootView.findViewById(R.id.txtLat);
        textView.setText(R.string.NoValue);

        textView = (TextView) rootView.findViewById(R.id.txtLon);
        textView.setText(R.string.NoValue);

        textView = (TextView) rootView.findViewById(R.id.txtHigh);
        textView.setText(R.string.NoValue);

        textView = (TextView) rootView.findViewById(R.id.txtSpeed);
        textView.setText(R.string.NoValue);

        textView = (TextView) rootView.findViewById(R.id.txtOri);
        textView.setText(R.string.NoValue);

        textView = (TextView) rootView.findViewById(R.id.txtPrec);
        textView.setText(R.string.NoValue);

        textView = (TextView) rootView.findViewById(R.id.txtSatBE);
        textView.setText(R.string.NoValue);
        textView = (TextView) rootView.findViewById(R.id.txtSatGLO);
        textView.setText(R.string.NoValue);
        textView = (TextView) rootView.findViewById(R.id.txtSatGPS);
        textView.setText(R.string.NoValue);
        textView = (TextView) rootView.findViewById(R.id.txtSatQZ);
        textView.setText(R.string.NoValue);
        textView = (TextView) rootView.findViewById(R.id.txtSatSB);
        textView.setText(R.string.NoValue);


    }


    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            TextView textView = (TextView) rootView.findViewById(R.id.txtLat);
            String s=String.format(Locale.ITALY,"%+3.6f",location.getLatitude());
            textView.setText(s);

            textView = (TextView) rootView.findViewById(R.id.txtLon);
            s=String.format(Locale.ITALY,"%+3.6f",location.getLongitude());
            textView.setText(s);

            textView = (TextView) rootView.findViewById(R.id.txtHigh);
            s=String.format(Locale.ITALY,"%3.0f (m)",location.getAltitude());
            textView.setText(s);

            textView = (TextView) rootView.findViewById(R.id.txtPrec);
            s=String.format(Locale.ITALY,"%3.0f (m)",location.getAccuracy());
            textView.setText(s);

            textView = (TextView) rootView.findViewById(R.id.txtSpeed);
            s=String.format(Locale.ITALY,"%3.0f (km/h)",location.getSpeed()*3.6f);
            textView.setText(s);

            textView = (TextView) rootView.findViewById(R.id.txtOri);
            s=String.format(Locale.ITALY,"%3.0f°",location.getBearing());
            textView.setText(s);

            // location.getProvider();

        }

        @Override
        public void onProviderDisabled(String provider) {
            turnOffAllUI();
        }

        @Override
        public void onProviderEnabled(String provider) {

            // turnOffAllUI();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            String s="";
        }

    }

    private class GpsStatusAdapter extends ArrayAdapter<GpsInfo> {
        private GpsInfo[] mGpsInfo;
        private Context mContext;

        private View emptyLine=null;// Test per ottimizzare le righe vuote. Ne creo una per tutte

        @Override
        public void addAll(Collection<? extends GpsInfo> collection) {
            super.addAll(collection);
        }

        @Override
        public void addAll(GpsInfo... items) {
            super.addAll(items);
        }

        public GpsStatusAdapter(Context context, GpsInfo[] gpsInfo){
            super(context, R.layout.gps_status_item, gpsInfo);
            mGpsInfo=gpsInfo;
            mContext=context;
        }

        @Override
        public int getCount() {
//            int c=0;
//            for (int i=0;i<255;i++){
//                if (!(mGpsInfo[i]==null || mGpsInfo[i].SNR==0.0f)){
//                    c++;
//                }
//            }
//            return c;
            return super.getCount();
        }

        @Override
        public int getItemViewType(int position) {
            // Define a way to determine which layout to use, here it's just evens and odds.
            return (mGpsInfo[position]==null || mGpsInfo[position].SNR==0.0f)?1:0;
        }

        // Mi serve per mostrare le righe vuote
        @Override
        public int getViewTypeCount() {
            return 2; // Count of different layouts
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolderItem viewHolderItem;

            if (convertView==null){
                LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                viewHolderItem = new ViewHolderItem();

                // Se il tipo di vista da proporre è completo allora popolo la viewHolderItem
                if(getItemViewType(position) == 0) {
                    convertView = layoutInflater.inflate(R.layout.gps_status_item, parent, false);
                    viewHolderItem.txtPRN   = (TextView) convertView.findViewById(R.id.txtPRN);
                    viewHolderItem.txtAz    = (TextView) convertView.findViewById(R.id.txtAz);
                    viewHolderItem.txtConst = (TextView) convertView.findViewById(R.id.txtConst);
                    viewHolderItem.txtEle   = (TextView) convertView.findViewById(R.id.txtEle);
                    viewHolderItem.txtFix   = (TextView) convertView.findViewById(R.id.txtFix);
                    viewHolderItem.txtSNRStatus= (TextView) convertView.findViewById(R.id.txtSNRStatus);
                    viewHolderItem.progressBarSNR = (ProgressBar)convertView.findViewById(R.id.progressBarSNR);
                }
                // Altrimenti metto un layout vuoto
                else {
                    // Test: riuso una sola view per tutte le empty line
                    if (emptyLine==null){
                        emptyLine=layoutInflater.inflate(R.layout.gps_status_empty_line,parent, false);
                    }
                    // convertView = emptyLine;
                    convertView=layoutInflater.inflate(R.layout.gps_status_empty_line,parent, false);
                }
                convertView.setTag(viewHolderItem);
            } else{
                viewHolderItem=(ViewHolderItem)convertView.getTag();
            }

            boolean greenLine=false;    // sfondo verde per i satelliti agganciati
            // Se non ho nulla ritorno una riga nulla
            if (getItemViewType(position) == 0){
                viewHolderItem.txtPRN.setText(String.valueOf(mGpsInfo[position].PRN));
                viewHolderItem.txtAz.setText(String.format("%d°",(int)mGpsInfo[position].azimuth));
                viewHolderItem.txtConst.setText(mGpsInfo[position].SatelliteConstellation);
                viewHolderItem.txtEle.setText(String.format("%d°",(int)mGpsInfo[position].elevation));
                viewHolderItem.txtFix.setText(String.valueOf((mGpsInfo[position].usedInFix)));
                viewHolderItem.txtSNRStatus.setText(String.format(Locale.ITALY,"(%3.1f)",mGpsInfo[position].SNR));
                viewHolderItem.progressBarSNR.setProgress((int) mGpsInfo[position].SNR);
                if (mGpsInfo[position].usedInFix){
                    greenLine = true;
                }
            }

            // Righe alternate
            // TODO: le righe vanno alternate ma solo per quelle effettivamente visibili e non per quelle vuote
            // Determinare la posizione mostrata di una riga visibile
//            if ((position &1)==0) {
//                convertView.setBackgroundColor(Color.WHITE);
//            } else{
//                convertView.setBackgroundColor(Color.LTGRAY);
//            }

            if (greenLine){
                convertView.setBackgroundColor(Color.GREEN);
            } else{
                convertView.setBackgroundResource(android.R.color.transparent);
            }
            return convertView;
        }
    }

    static class ViewHolderItem {
        TextView txtConst;
        TextView txtPRN;
        TextView txtFix;
        TextView txtAz;
        TextView txtEle;
        TextView txtSNRStatus;
        ProgressBar progressBarSNR;
        //int position;
    }


    //// DEBUG
    private Random rnd = new Random();

//    private class GpsInfo{
//        public int PRN;
//        public float SNR;
//        public boolean usedInFix, hasAlmanac, hasEphemeris;
//        public float azimuth, elevation;
//        public String SatelliteConstellation;
//
//        public void resetAll(){
//            PRN=0;
//            SNR=0.0f;
//            usedInFix=false;
//            hasAlmanac=false;
//            hasEphemeris=false;
//            azimuth=0.0f;
//            elevation=0.0f;
//            SatelliteConstellation="??";
//        }
//
//        public void updateValues(GpsSatellite gpsSatellite){
//            PRN=gpsSatellite.getPrn();
//            SNR=gpsSatellite.getSnr();
//            usedInFix = gpsSatellite.usedInFix();
//            azimuth= gpsSatellite.getAzimuth();
//            elevation= gpsSatellite.getElevation();
//            hasAlmanac= gpsSatellite.hasAlmanac();
//            hasEphemeris= gpsSatellite.hasEphemeris();
//
//            switch (GpsSatelliteType.getSatelliteType(PRN)){
//                case BEIDOU:
//                    SatelliteConstellation="BEI"; break;
//                case SBAS:
//                    SatelliteConstellation="SBA"; break;
//                case GLONASS:
//                    SatelliteConstellation="GLO"; break;
//                case QZSS:
//                    SatelliteConstellation="QZS"; break;
//                case GPS:
//                    SatelliteConstellation="GPS"; break;
//                default:
//                    SatelliteConstellation="na";
//            }
//        }
//
//
//        GpsInfo(){
//            resetAll();
//
//            /////// DEBUG
////            usedInFix=rnd.nextBoolean();
////            azimuth=rnd.nextInt(360);
////            elevation=rnd.nextInt(90);
////            PRN=rnd.nextInt(236);
////
////            if (rnd.nextBoolean()) {
////                SNR = rnd.nextInt(10);
////            }
//
//        }
//    }

//    enum GpsSatelliteTypeValue {
//        GPS,SBAS,NA,GLONASS, QZSS, BEIDOU
//    }

//    private static class GpsSatelliteType{
//        public static GpsSatelliteTypeValue getSatelliteType(int PRN){
//            /*
//            1–32: GPS
//            33–54: Various SBAS systems (EGNOS, WAAS, SDCM, GAGAN, MSAS) – some IDs still unused
//            55–64: not used (might be assigned to further SBAS systems)
//            65–88: GLONASS
//            89–96: GLONASS (future extensions?)
//            97–192: not used
//            193–195: QZSS
//            196–200: QZSS (future extensions?)
//            201–235: Beidou
//             */
//            if (PRN>=1 && PRN <=32){
//                return GpsSatelliteTypeValue.GPS;
//            }
//
//            if (PRN>=33 && PRN <=54){
//                return GpsSatelliteTypeValue.SBAS;
//            }
//
//            if ((PRN>=55 && PRN <=64) || (PRN>=97 && PRN <=192)){
//                return GpsSatelliteTypeValue.NA;
//            }
//
//            if ((PRN>=65 && PRN <=96)){
//                return GpsSatelliteTypeValue.GLONASS;
//            }
//
//            if ((PRN>=193&& PRN <=200)){
//                return GpsSatelliteTypeValue.QZSS;
//            }
//
//            if ((PRN>=201 && PRN <=235)){
//                return GpsSatelliteTypeValue.BEIDOU;
//            }
//
//            return GpsSatelliteTypeValue.NA;
//        }
//    }
}
