package com.example.utente.logmyposition.fragments;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.utente.logmyposition.ApplicationSettings;
import com.example.utente.logmyposition.R;

import org.w3c.dom.Text;

import java.util.Iterator;
import java.util.Locale;
import java.util.Random;

/**
 * Created by utente on 17/07/2016.
 */
public class SimpleGpsViewFragment extends Fragment implements GpsStatus.Listener{

    Context mContext=null;
    Activity mActivity=null;
    View    rootView=null;

    boolean posizioneAggiornata=false;

    LocationManager locationManager;
    MyLocationListener locationListener;
    LocationProvider locationProvider;

    SensorManager sensorManager;
    SensorEventListener sensorEventListener;

    // New style (uso accelerometro e magnetometro invede di orientation
    Sensor accelerometer;
    Sensor magnetometer;

    // Unità di misura per la velocità (km/h o m/s)
    private boolean showKMH=true;

    ApplicationSettings applicationSettings = ApplicationSettings.getInstance();
    private Button btnShare;
    private TextView txtSpeed;
    private TextView txtSpeedUnit;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //super.onCreate(savedInstanceState);
        final View rootView = inflater.inflate(R.layout.fragment_simple_gps_view, container, false);

        if (mActivity==null){
            mActivity=getActivity();
            mContext=getActivity().getApplicationContext();
        }

        // Registra i sensori
        // GPS
        locationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
        locationManager.addGpsStatusListener(this);

        locationProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
        locationListener = new MyLocationListener();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                0,
                0.0f,
                locationListener);

        ListView listView = (ListView)rootView.findViewById(R.id.listViewStatus);

        // Imposta i listener per i bottoni e textarea
        btnShare = (Button) rootView.findViewById(R.id.btnShare);
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharePosition();
            }
        });

        txtSpeed = (TextView) rootView.findViewById(R.id.txtSpeed);
        txtSpeed.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showKMH=!showKMH;   // Switch tra m/s e km/h

                Toast t = Toast.makeText(rootView.getContext(),showKMH?"Velocità in km/h":"Velocità in m/s",Toast.LENGTH_LONG);
                t.show();

                // Refresh
                updateLabels();
                return true;
            }
        });
//        txtSpeed.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showKMH=!showKMH;   // Switch tra m/s e km/h
//
//                // Refresh
//                txtSpeed.invalidate();
//                txtSpeedUnit.invalidate();
//            }
//        });

        // salvo la textview per ottimizzare i riferimenti neu metodi di update
        txtSpeedUnit = (TextView) rootView.findViewById(R.id.txtSpeedUnit);

        // Sensori accelerometro e magnetometro
        sensorManager = (SensorManager) mActivity.getSystemService(Context.SENSOR_SERVICE);
        sensorEventListener=new MySensorListener();
        accelerometer=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer=sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_UI);

        this.rootView=rootView;
        return rootView;
    }

    private void updateLabels(){
        if (showKMH){
            txtSpeedUnit.setText("km/h");
        } else {
            txtSpeedUnit.setText("m/s");
        }

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

        if (sensorEventListener!=null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }

    @Override
    public void onGpsStatusChanged(int event) {
        String s="";
        switch (event){
            case GpsStatus.GPS_EVENT_FIRST_FIX :    s="GPS_EVENT_FIRST_FIX"; break;
            case GpsStatus.GPS_EVENT_STARTED :      s="GPS_EVENT_STARTED"; applicationSettings.setGPSAvailable(true); break;
            case GpsStatus.GPS_EVENT_STOPPED :      s="GPS_EVENT_STOPPED"; applicationSettings.setGPSAvailable(false);
                posizioneAggiornata=false;
                turnOffAllUI();
                break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS : s="GPS_EVENT_SATELLITE_STATUS";
                break;
        }

        GpsStatus gs = locationManager.getGpsStatus(null);

        final Iterator<GpsSatellite> it = gs.getSatellites().iterator();
        GpsSatellite gpsSatellite;

        int maxSat=0, currSat=0, fixSat=0;
//        int[] SatType={0,0,0,0,0};
//        int[] SatTypeFix={0,0,0,0,0};
//        int SatTypeIndex=0;
//        int satellitePRN;

        while (it.hasNext()) {
            gpsSatellite=it.next();

            if(gpsSatellite.usedInFix()){
                fixSat++;
            }
            maxSat++;

//            satellitePRN=gpsSatellite.getPrn();

            if (gpsSatellite.getSnr()>0){
                currSat++;
            }

//            if (SatTypeIndex>=0){
//                SatType[SatTypeIndex]++;
//                if (gpsSatellite.usedInFix()){
//                    SatTypeFix[SatTypeIndex]++;
//                }
//            }
        }
    }

    private void turnOffAllUI(){
        TextView textView ;

        textView = (TextView) rootView.findViewById(R.id.txtLat);
        textView.setText(R.string.NoValue);

        textView = (TextView) rootView.findViewById(R.id.txtLon);
        textView.setText(R.string.NoValue);

        textView = (TextView) rootView.findViewById(R.id.txtHigh);
        textView.setText(R.string.NoValue);

        textView = (TextView) rootView.findViewById(R.id.txtSpeed);
        textView.setText(R.string.ZeroValue);

        textView = (TextView) rootView.findViewById(R.id.txtSpeedDecimals);
        textView.setText(R.string.ZeroValue);


        textView = (TextView) rootView.findViewById(R.id.txtOri);
        textView.setText(R.string.NoValue);

        textView = (TextView) rootView.findViewById(R.id.txtPrec);
        textView.setText(R.string.NoValue);
    }

//"http://maps.google.com/maps?q="+String.format(Locale.ITALY,"%f,%f",));
    private void sharePosition(){

        Location location= locationManager.getLastKnownLocation(locationProvider.getName());
        if (!posizioneAggiornata || location==null || !locationManager.isProviderEnabled(locationProvider.getName())){
            Toast t = Toast.makeText(rootView.getContext(),getString(R.string.PosizioneNonConosciuta), Toast.LENGTH_LONG);
            t.show();
            return;
        }
        String googleMapsPosition=String.format(Locale.ENGLISH,"http://maps.google.com/maps?q=%+3.6f,%3.6f",location.getLatitude(),location.getLongitude());

        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType(getString(R.string.ShareMimeType));
       // sharingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);

        sharingIntent.putExtra(Intent.EXTRA_TEXT, googleMapsPosition);

        startActivityForResult(Intent.createChooser(sharingIntent, getString(R.string.CondividiCon)), ApplicationSettings.SHARE_PICKER);
    }


    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            posizioneAggiornata=true;
            TextView textView;
            textView = (TextView) rootView.findViewById(R.id.txtLat);
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

            float speed= (showKMH)?location.getSpeed()*3.6f:location.getSpeed();  // Velocità in km/h o m/s

            int   speedDecimals = (int)Math.floor((double)10*(speed-((int)speed)));
            textView = (TextView) rootView.findViewById(R.id.txtSpeed);
            s=String.format(Locale.ITALY,"%3.0f",Math.floor(speed));
            textView.setText(s);

            textView = (TextView) rootView.findViewById(R.id.txtSpeedDecimals);
            s=String.format(Locale.ITALY,"%1d",speedDecimals);
            textView.setText(s);

            textView = (TextView) rootView.findViewById(R.id.txtOri);
            s=String.format(Locale.ITALY,"%3.0f°",location.getBearing());
            textView.setText(s);

            // location.getProvider();
        }

        @Override
        public void onProviderDisabled(String provider) {
            turnOffAllUI();
            posizioneAggiornata=false;
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

    // record the compass picture angle turned
    private class MySensorListener implements SensorEventListener {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        final int accumIndexMax=10;     // Calcolo media ultimi rilevamenti
        int accumIndexGravity=0;        // media mobile, mi serve un indice per indirizzare l'accumulatore
        int accumIndexGeomagnetic=0;    // media mobile, mi serve un indice per indirizzare l'accumulatore
        float[][] mGravity=new float[accumIndexMax][3];     // Valori misurati
        float[][] mGeomagnetic=new float[accumIndexMax][3]; // Valori misurati
        float[] mGravityMean=new float[3];                  // Valori medi
        float[] mGeomagneticMean=new float[3];              // valori medi
        private float currentDegree = 0f;                   // Angolo per l'animazione
        float azimuth=0f;                                   // Angolo per l'animazione

        // Alloco solo una volta visto che altrimenti devo farlo per ogni cambiamento del sensore
        float Rot[] = new float[9];                         // Matrice di appoggio per il calcolo della rotazione
        float Inc[] = new float[9];                         // Matrice di appoggio per il calcolo dell'inclinazione
        float orientation[] = new float[3];                 // MAtrice di appoggio per il calcolo dell'orientamento

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                // Aggiorno la media mobile togliendo il vecchio valore ed aggiungendo quello nuovo
                mGravityMean[0]=mGravityMean[0]+(-mGravity[accumIndexGravity][0]+event.values[0])/((float)accumIndexMax);
                mGravityMean[1]=mGravityMean[1]+(-mGravity[accumIndexGravity][1]+event.values[1])/((float)accumIndexMax);
                mGravityMean[2]=mGravityMean[2]+(-mGravity[accumIndexGravity][2]+event.values[2])/((float)accumIndexMax);

                // Copio il nuovo valore
                System.arraycopy(event.values, 0, mGravity[accumIndexGravity], 0, event.values.length);

                // Aggiorno l'indice dell'accumulatore
                accumIndexGravity++;
                if(accumIndexGravity>=accumIndexMax){
                    accumIndexGravity=0;
                }
            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                // Aggiorno la media mobile togliendo il vecchio valore ed aggiungendo quello nuovo
                mGeomagneticMean[0]=mGeomagneticMean[0]+(-mGeomagnetic[accumIndexGeomagnetic][0]+event.values[0])/((float)accumIndexMax);
                mGeomagneticMean[1]=mGeomagneticMean[1]+(-mGeomagnetic[accumIndexGeomagnetic][1]+event.values[1])/((float)accumIndexMax);
                mGeomagneticMean[2]=mGeomagneticMean[2]+(-mGeomagnetic[accumIndexGeomagnetic][2]+event.values[2])/((float)accumIndexMax);

                System.arraycopy(event.values, 0, mGeomagnetic[accumIndexGeomagnetic], 0, event.values.length);
                accumIndexGeomagnetic++;
                if(accumIndexGeomagnetic>=accumIndexMax){
                    accumIndexGeomagnetic=0;
                }
            }

            if (SensorManager.getRotationMatrix(Rot, Inc, mGravityMean, mGeomagneticMean)) {
                SensorManager.getOrientation(Rot, orientation);
                // orientation[0] è l'azimuth in radianti da -pi a +pi
                // lo trasformo in gradi e lo riporto tra 0 e 360
                azimuth = (float)(Math.toDegrees(orientation[0])+360)%360;
            }

            // float azimut=Math.round(event.values[0]); // OLD STYLE
            TextView textView = (TextView) rootView.findViewById(R.id.txtCompass);
            String s=String.format(Locale.ITALY,"%3.0f°",azimuth);
            textView.setText(s);

            ImageView imageView = (ImageView) rootView.findViewById(R.id.imgCompass);
            RotateAnimation ra = new RotateAnimation(currentDegree,-azimuth,
                    Animation.RELATIVE_TO_SELF,0.5f,
                    Animation.RELATIVE_TO_SELF,0.5f);

            ra.setInterpolator(new LinearInterpolator());
            ra.setDuration(250);
            ra.setFillAfter(true);
            imageView.startAnimation(ra);
            currentDegree=-azimuth;

            //updateNotificationIcon();
        }

//        // TODO: Test per aggiornare in tempo reale l'orientamento dell'icona
//        private NotificationManager mNM;
//        private Notification myNotication ;
//        private int NOTIFICATION = R.string.local_service_started;
//        private void updateNotificationIcon(){
//            mNM = (NotificationManager) mActivity.getSystemService(mContext.NOTIFICATION_SERVICE);
//            Matrix matrix = new Matrix();
//            matrix.postRotate(azimuth);
//            Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.compass_rose_md);
//            Bitmap bitmapRotated= Bitmap.createBitmap(
//                    bitmap,
//                    0,0,
//                    bitmap.getWidth(),bitmap.getHeight(),
//                    matrix,true
//            );
//
//            Notification.Builder builder = new Notification.Builder(getContext());
//            builder.setLargeIcon(bitmapRotated);
//
//            myNotication= builder.build();
//
//            // myNotication.flags |= Notification.FLAG_INSISTENT;
//            myNotication.flags |= Notification.VISIBILITY_PUBLIC;
//
//            mNM.notify(NOTIFICATION, myNotication);
//
//        }
    }
}
