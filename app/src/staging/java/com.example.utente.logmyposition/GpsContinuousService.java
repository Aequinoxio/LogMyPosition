package com.example.utente.logmyposition;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.example.utente.logmyposition.ApplicationSettings;
import com.example.utente.logmyposition.LogPositionLocationListener;
import com.example.utente.logmyposition.util.GpsInfo;
import com.example.utente.logmyposition.util.LowPassFilter;

/**
 * Created by utente on 03/08/2016.
 *
 * Questa classe implementa il servizio di listening sullo stato del GPS, della posizione (Da verificare se serve)
 * e sullo stato dei sensori. Dispaccia gli eventi a tutta l'app (servizio attivo solo se l'app è in foreground)
 * Singleton
 */
public class GpsContinuousService extends Service
        implements GpsStatus.Listener, LocationListener, SensorEventListener,
                    GPSListenerCallback /*, SensorListenerCallback */ {

    private final static GpsContinuousService gpsContinuousService=new GpsContinuousService();

    // GPS
    private LocationManager locationManager;
    private LocationProvider locationProvider;
    private LogPositionLocationListener locationListener;
    private GpsInfo[] mGpsInfo;

    // Sensori
    SensorManager sensorManager;
    //SensorEventListener sensorEventListener;

    // Disponibilità sensori
    boolean isAccelAvailable ;

    boolean isCompassAvailable;
    boolean isGyroAvailable ;
    // compute rotation matrix
    float rotation[] = new float[9];

    float identity[] = new float[9];
    float[] lastAccelerometer=new float[3];
    float[] lastCompass=new float[3];
    float orientation[] = new float[3];
    float orientationDeg[] = new float[3];
    float cameraRotation[] = new float[9];


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private LowPassFilter lowPassFilterAccel=new LowPassFilter(), lowPassFilterMagnet=new LowPassFilter();

    public GpsContinuousService getInstance(){
        return gpsContinuousService;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            lastAccelerometer=lowPassFilterAccel.lowPass(event.values.clone());
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            lastCompass=lowPassFilterMagnet.lowPass(event.values.clone());
        }

        boolean gotRotation = SensorManager.getRotationMatrix(rotation,identity, lastAccelerometer, lastCompass);
        if (gotRotation) {

            // remap such that the camera is pointing straight down the Y axis
            SensorManager.remapCoordinateSystem(rotation, SensorManager.AXIS_X,
                    SensorManager.AXIS_Z, cameraRotation);

            // orientation vector
            SensorManager.getOrientation(cameraRotation, orientation);
        }

        orientationDeg[0]= (float)(Math.toDegrees(orientation[0])+360)%360;  // Riporto tra 0 e 360
        orientationDeg[1]= (float)(-Math.toDegrees(orientation[1]))%360;     // Riporto tra 0 - orizzontale, 90 - verticale superiore -90 - vertivale inferiore
        orientationDeg[2]= (float)(Math.toDegrees(orientation[2])+360)%360;  // Riporto tra 0 3 360
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onGpsStatusChanged(int event) {

    }

    // I singleton in Android possono essere deallocati se l'activity è chiusa dal S.O.
    ApplicationSettings applicationSettings = ApplicationSettings.getInstance();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        avviaServizio();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private GpsContinuousService() {
        super();

        // Inizializzazione
        mGpsInfo=new GpsInfo[255];

        // Inizializzo le info sui satelliti
        for (int i=0;i<255;i++){
            mGpsInfo[i]=new GpsInfo();
            mGpsInfo[i].resetAll();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeGpsStatusListener(this);
        sensorManager.unregisterListener(this);

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void avviaServizio(){
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.addGpsStatusListener(this);

        locationProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);

        locationListener = new LogPositionLocationListener(getApplicationContext());
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                0,
                0.0f,
                locationListener);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // TODO: Creare metodi specifici di registrazione de registrazione
        isAccelAvailable    = sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
        isCompassAvailable  = sensorManager.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_NORMAL);
        isGyroAvailable     = sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

//    private void aggiornaStatoApp(){
//        Intent intent = new Intent("AggiornaInterfaccia");
//        //intent.putExtra("STATOSERVIZIO", servizioAvviato);
//        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
//    }

}
