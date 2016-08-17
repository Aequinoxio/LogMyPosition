package com.example.utente.logmyposition;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by utente on 03/08/2016.
 *
 * Questa classe implementa il servizio di listening sullo stato del GPS, della posizione (Da verificare se serve)
 * e sullo stato dei sensori. Dispaccia gli eventi a tutta l'app (servizio attivo solo se l'app è in foreground)
 */
public class GpsContinuousService extends Service implements GpsStatus.Listener, LocationListener, SensorEventListener {

    private LocationManager locationManager;
    private LocationProvider locationProvider;
    private LogPositionLocationListener locationListener;

    // I singleton in Android possono essere deallocati se l'activity è chiusa dal S.O.
    ApplicationSettings applicationSettings = ApplicationSettings.getInstance();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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

    public GpsContinuousService() {
        super();
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
                applicationSettings.getMinTimeLocationUpdate(),
                applicationSettings.getMinDistanceLocationUpdate()
                , locationListener);

        applicationSettings.setStatoServizio(true);
        applicationSettings.setGPSAvailable(locationManager.isProviderEnabled("gps"));
    }

    private void aggiornaStatoApp(){
        Intent intent = new Intent("AggiornaInterfaccia");
  //      intent.putExtra("STATOSERVIZIO", servizioAvviato);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onGpsStatusChanged(int event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

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
}
