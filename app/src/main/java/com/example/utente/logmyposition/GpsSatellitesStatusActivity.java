package com.example.utente.logmyposition;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Camera;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;
import java.util.StringTokenizer;

public class GpsSatellitesStatusActivity extends ActionBarActivity implements GpsStatus.Listener{

    private LocationManager locationManager;
    private LocationListener locationListener;
    private GpsStatusAdapter gpsStatusAdapter;
    private ApplicationSettings applicationSettings = ApplicationSettings.getInstance();

    private GpsInfo[] mGpsInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_satellites_status);

        mGpsInfo=new GpsInfo[255];

        // Inizializzo le info sui satelliti
        for (int i=0;i<255;i++){
            mGpsInfo[i]=new GpsInfo();
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.addGpsStatusListener(this);

        LocationProvider locationProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
        locationListener = new MyLocationListener();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                0,
                1.0f,
                locationListener);

        ListView listView = (ListView)findViewById(R.id.listViewStatus);
        gpsStatusAdapter= new GpsStatusAdapter(this,mGpsInfo);
        listView.setAdapter(gpsStatusAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

        TextView textView = (TextView) findViewById(R.id.txtGpsStatus);
        textView.setText(s);

        GpsStatus gs = locationManager.getGpsStatus(null);

        final Iterator<GpsSatellite> it = gs.getSatellites().iterator();
        GpsSatellite gpsSatellite;

        textView = (TextView) findViewById(R.id.txtFixTime);
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

        textView = (TextView) findViewById(R.id.txtSatBE);
        textView.setText(String.format("%d (%d)",SatType[0],SatTypeFix[0]));
        textView = (TextView) findViewById(R.id.txtSatGLO);
        textView.setText(String.format("%d (%d)",SatType[1],SatTypeFix[1]));
        textView = (TextView) findViewById(R.id.txtSatGPS);
        textView.setText(String.format("%d (%d)",SatType[2],SatTypeFix[2]));
        textView = (TextView) findViewById(R.id.txtSatQZ);
        textView.setText(String.format("%d (%d)",SatType[3],SatTypeFix[3]));
        textView = (TextView) findViewById(R.id.txtSatSB);
        textView.setText(String.format("%d (%d)",SatType[4],SatTypeFix[4]));

        textView = (TextView)findViewById(R.id.txtSatCurrMax);
        textView.setText(String.valueOf(maxSat)+" / "+String.valueOf(currSat)+" ("+String.valueOf(fixSat)+")");

        gpsStatusAdapter.notifyDataSetChanged();
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
        TextView textView = (TextView) findViewById(R.id.txtSatCurrMax);
        textView.setText(R.string.NoValue);

        textView = (TextView) findViewById(R.id.txtFixTime);
        textView.setText(R.string.NoValue);

        textView = (TextView) findViewById(R.id.txtLat);
        textView.setText(R.string.NoValue);

        textView = (TextView) findViewById(R.id.txtLon);
        textView.setText(R.string.NoValue);

        textView = (TextView) findViewById(R.id.txtHigh);
        textView.setText(R.string.NoValue);

        textView = (TextView) findViewById(R.id.txtSpeed);
        textView.setText(R.string.NoValue);

        textView = (TextView) findViewById(R.id.txtOri);
        textView.setText(R.string.NoValue);

        textView = (TextView) findViewById(R.id.txtPrec);
        textView.setText(R.string.NoValue);

        textView = (TextView) findViewById(R.id.txtSatBE);
        textView.setText(R.string.NoValue);
        textView = (TextView) findViewById(R.id.txtSatGLO);
        textView.setText(R.string.NoValue);
        textView = (TextView) findViewById(R.id.txtSatGPS);
        textView.setText(R.string.NoValue);
        textView = (TextView) findViewById(R.id.txtSatQZ);
        textView.setText(R.string.NoValue);
        textView = (TextView) findViewById(R.id.txtSatSB);
        textView.setText(R.string.NoValue);


    }


    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            TextView textView = (TextView) findViewById(R.id.txtLat);
            String s=String.format(Locale.ITALY,"%+3.6f",location.getLatitude());
            textView.setText(s);

            textView = (TextView) findViewById(R.id.txtLon);
            s=String.format(Locale.ITALY,"%+3.6f",location.getLongitude());
            textView.setText(s);

            textView = (TextView) findViewById(R.id.txtHigh);
            s=String.format(Locale.ITALY,"%3.0f (m)",location.getAltitude());
            textView.setText(s);

            textView = (TextView) findViewById(R.id.txtPrec);
            s=String.format(Locale.ITALY,"%3.0f (m)",location.getAccuracy());
            textView.setText(s);

            textView = (TextView) findViewById(R.id.txtSpeed);
            s=String.format(Locale.ITALY,"%3.0f (km/h)",location.getSpeed()*3.6f);
            textView.setText(s);

            textView = (TextView) findViewById(R.id.txtOri);
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

        public GpsStatusAdapter(Context context, GpsInfo[] gpsInfo){
            super(context, R.layout.gps_status_item, gpsInfo);
            mGpsInfo=gpsInfo;
            mContext=context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);

            if (mGpsInfo[position]==null || mGpsInfo[position].SNR==0.0f){
                return (layoutInflater.inflate(R.layout.gps_status_empty_line,parent, false));
            }

            View rowView = layoutInflater.inflate(R.layout.gps_status_item,parent, false);

            TextView textView = (TextView) rowView.findViewById(R.id.txtPRN);
            textView.setText(String.valueOf(mGpsInfo[position].PRN));

            ProgressBar progressBar = (ProgressBar) rowView.findViewById(R.id.progressBarSNR);
            progressBar.setProgress((int) mGpsInfo[position].SNR);

            textView = (TextView) rowView.findViewById(R.id.txtSNRStatus);
            textView.setText(String.format(Locale.ITALY,"(%3.1f)",mGpsInfo[position].SNR));

            textView = (TextView) rowView.findViewById(R.id.txtConst);
            textView.setText(mGpsInfo[position].SatelliteConstellation);

            textView = (TextView) rowView.findViewById(R.id.txtAz);
            textView.setText(String.format("%d°",(int)mGpsInfo[position].azimuth));

            textView = (TextView) rowView.findViewById(R.id.txtEle);
            textView.setText(String.format("%d°",(int)mGpsInfo[position].elevation));

            textView = (TextView) rowView.findViewById(R.id.txtFix);
            textView.setText(String.valueOf((mGpsInfo[position].usedInFix)));

//            progressBar.invalidate();

            return rowView;
        }
    }

    //// DEBUG
    private Random rnd = new Random();

    private class GpsInfo{
        public int PRN;
        public float SNR;
        public boolean usedInFix, hasAlmanac, hasEphemeris;
        public float azimuth, elevation;
        public String SatelliteConstellation;

        public void resetAll(){
            PRN=0;
            SNR=0.0f;
            usedInFix=false;
            hasAlmanac=false;
            hasEphemeris=false;
            azimuth=0.0f;
            elevation=0.0f;
            SatelliteConstellation="??";
        }

        public void updateValues(GpsSatellite gpsSatellite){
            PRN=gpsSatellite.getPrn();
            SNR=gpsSatellite.getSnr();
            usedInFix = gpsSatellite.usedInFix();
            azimuth= gpsSatellite.getAzimuth();
            elevation= gpsSatellite.getElevation();
            hasAlmanac= gpsSatellite.hasAlmanac();
            hasEphemeris= gpsSatellite.hasEphemeris();

            switch (GpsSatelliteType.getSatelliteType(PRN)){
                case BEIDOU:
                    SatelliteConstellation="BE"; break;
                case SBAS:
                    SatelliteConstellation="SB"; break;
                case GLONASS:
                    SatelliteConstellation="GL"; break;
                case QZSS:
                    SatelliteConstellation="QZ"; break;
                case GPS:
                    SatelliteConstellation="GP"; break;
                default:
                    SatelliteConstellation="na";
            }
        }


        GpsInfo(){
            resetAll();
            /*
            /////// DEBUG
            usedInFix=rnd.nextBoolean();
            azimuth=rnd.nextInt(360);
            elevation=rnd.nextInt(90);
            PRN=rnd.nextInt(236);

            if (rnd.nextBoolean()) {
                SNR = rnd.nextInt(100);
            }
            */
        }
    }

    enum GpsSatelliteTypeValue {
        GPS,SBAS,NA,GLONASS, QZSS, BEIDOU
    }
    private static class GpsSatelliteType{
        public static GpsSatelliteTypeValue getSatelliteType(int PRN){
            /*
            1–32: GPS
            33–54: Various SBAS systems (EGNOS, WAAS, SDCM, GAGAN, MSAS) – some IDs still unused
            55–64: not used (might be assigned to further SBAS systems)
            65–88: GLONASS
            89–96: GLONASS (future extensions?)
            97–192: not used
            193–195: QZSS
            196–200: QZSS (future extensions?)
            201–235: Beidou
             */
            if (PRN>=1 && PRN <=32){
                return GpsSatelliteTypeValue.GPS;
            }

            if (PRN>=33 && PRN <=54){
                return GpsSatelliteTypeValue.SBAS;
            }

            if ((PRN>=55 && PRN <=64) || (PRN>=97 && PRN <=192)){
                return GpsSatelliteTypeValue.NA;
            }

            if ((PRN>=65 && PRN <=96)){
                return GpsSatelliteTypeValue.GLONASS;
            }

            if ((PRN>=193&& PRN <=200)){
                return GpsSatelliteTypeValue.QZSS;
            }

            if ((PRN>=201 && PRN <=235)){
                return GpsSatelliteTypeValue.BEIDOU;
            }

            return GpsSatelliteTypeValue.NA;
        }
    }
}
