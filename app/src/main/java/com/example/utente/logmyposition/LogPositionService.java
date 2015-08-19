package com.example.utente.logmyposition;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Debug;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by utente on 14/08/2015.
 */
public class LogPositionService extends Service implements GpsStatus.Listener {
    // DEBUGMODE
    private MockLocationProvider mock;

    private Timer timer;

    private NotificationManager mNM;
    private int NOTIFICATION = R.string.local_service_started;

    private boolean servizioAvviato=false;

    private LocationManager locationManager;
    private LocationProvider locationProvider;
    private LogPositionLocationListener locationListener;

    // Receiver per aggiornare la parte dell'interfacci di competenza del servizio (es. notifica)
    private BroadcastReceiver mMessageFromListenerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Aggiorno l'interfaccia
            showNotification();
        }
    };

    /**
     * Implementato per realizzare la classe GpsStatus Listener
     *
     * @param event
     *
     * Tipi di evento:
     *
     * GPS_EVENT_STARTED
     * GPS_EVENT_STOPPED
     * GPS_EVENT_FIRST_FIX
     * GPS_EVENT_SATELLITE_STATUS
     *
     */
    @Override
    public void onGpsStatusChanged(int event){
        String s="";
        switch (event){
            case GpsStatus.GPS_EVENT_FIRST_FIX : s="GPS_EVENT_FIRST_FIX"; break;
            case GpsStatus.GPS_EVENT_STARTED : s="GPS_EVENT_STARTED"; ApplicationSettings.setGPSAvailable(true); break;
            case GpsStatus.GPS_EVENT_STOPPED : s="GPS_EVENT_STOPPED"; ApplicationSettings.setGPSAvailable(false); break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS : s="GPS_EVENT_SATELLITE_STATUS";
                calcolaNumeroSatelliti();
                break;
        }
        Log.e(getClass().getSimpleName(), "Evento GPS Cambiato:"+s);

        aggiornaMainActivity();
        showNotification();
    }
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     * <p/>
     * From Andoird sdk documentation
     */
    public class LocalBinder extends Binder {
        LogPositionService getService() {
            return LogPositionService.this;
        }
    }


    /**
     * Dalla documentazione android sdk
     */
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /**
     * Dalla documentazione android sdk
     *
     * @param intent
     * @return
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(getClass().getSimpleName(), "Creato");

        // Imposto una notifica se avvio il servizio
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    /**
     * Dalla documentazione di android sdk
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(getClass().getSimpleName(), "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.

        avviaServizio();

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageFromListenerReceiver,
                new IntentFilter("AggiornaInterfaccia"));

        return START_STICKY;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(getClass().getSimpleName(), "Distrutto");

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageFromListenerReceiver);

        // Salvo lo stato dell'applicazione
       // if (servizioAvviato=true) {
            // Cancel the persistent notification.
            //mNM.cancel(ApplicationSettings.getNotificationID());
            mNM.cancel(NOTIFICATION);
            servizioAvviato = false;
            ApplicationSettings.setStatoServizio(false);
            ApplicationSettings.savePreferences(getApplicationContext());

            // Deregistro il listener per la posizione
            locationManager.removeUpdates(locationListener);

            // Rimuovo il timer
            timer.cancel();
            timer = null;
       // }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ApplicationSettings.savePreferences(getApplicationContext());
        Log.e("Servizio", "Poca memoria");
    }

    private void eseguiCompito() {
        Log.e(getClass().getSimpleName(), "running");
        if (BuildConfig.DEBUG){
            double la=Math.random()*60;
            double lo=Math.random()*20;
            mock.pushLocation(la,lo);
            Log.e(getClass().getSimpleName(),"Mocking..."+
            String.format("%f",la)+ " - "+ String.format("%f",lo)
            );
        }
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        String text = getString(
                servizioAvviato?
                R.string.local_service_started : R.string.local_service_disconnected
        );

        text+=(ApplicationSettings.isGPSAvailable()?" (GPS Abilitato)":" (GPS Disabilitato)");

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_main_w, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.local_service_label),
                text, contentIntent);

        // Send the notification.
        //mNM.notify(ApplicationSettings.getNotificationID(), notification);
        mNM.notify(NOTIFICATION, notification);
    }

    private void aggiornaMainActivity(){
        Intent intent = new Intent("AggiornaInterfaccia");
        intent.putExtra("STATOSERVIZIO", servizioAvviato);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private int calcolaNumeroSatelliti(){
        // Set the no. of available satellites.
        final GpsStatus gs = this.locationManager.getGpsStatus(null);
        int i = 0;

        final Iterator<GpsSatellite> it = gs.getSatellites().iterator();
        while (it.hasNext()) {
            it.next();
            i ++;
        }
        ApplicationSettings.setSatelliti(i);
        return i;
    }
    private void impostaAttivitaTemporizzata(){
        // avvio il timer
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                eseguiCompito();
            }
        };
        timer = new Timer();
        timer.schedule(timerTask, 0, 5000);
    }

    private void avviaServizio(){
        if (BuildConfig.DEBUG){
            mock = new MockLocationProvider(
                    LocationManager.GPS_PROVIDER, getApplicationContext());
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);


        locationListener = new LogPositionLocationListener(getApplicationContext());
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                ApplicationSettings.getMinTimeLocationUpdate(),
                ApplicationSettings.getMinDistanceLocationUpdate()
                , locationListener);

        servizioAvviato=true;
        ApplicationSettings.setStatoServizio(true);
        ApplicationSettings.setGPSAvailable(locationManager.isProviderEnabled("gps"));

        // Imposta attività temporizzata (per DEBUG
        impostaAttivitaTemporizzata();
        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
/*
        // Se è tutto ok attivo il provider
        if ((locationProvider != null) && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))) {
            locationListener = new LogPositionLocationListener(getApplicationContext());
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    ApplicationSettings.getMinTimeLocationUpdate(),
                    ApplicationSettings.getMinDistanceLocationUpdate()
                    , locationListener);
            servizioAvviato=true;
            ApplicationSettings.setStatoServizio(true);


            // Imposta attività temporizzata (per DEBUG
            impostaAttivitaTemporizzata();
            // Display a notification about us starting.  We put an icon in the status bar.
            showNotification();

        } else {
            locationManager=null;
            locationProvider=null;
            locationListener=null;
            // Visualizzo un messaggio come feedback in caso il GPS non sia attivo
            Toast myToast = Toast.makeText(getApplicationContext(),
                    this.getString(R.string.noGPSInfo) + "\n",
                    Toast.LENGTH_LONG);
            myToast.show();
            servizioAvviato=false;
            ApplicationSettings.setStatoServizio(false);
        }
        */

        aggiornaMainActivity();
    }
/*
    // see https://androidcookbook.com/Recipe.seam?recipeId=1229
    private void setMockLocation(double latitude, double longitude, float accuracy) {
        locationManager.addTestProvider(LocationManager.GPS_PROVIDER,
                "requiresNetwork" == "",
                "requiresSatellite" == "",
                "requiresCell" == "",
                "hasMonetaryCost" == "",
                "supportsAltitude" == "",
                "supportsSpeed" == "",
                "supportsBearing" == "",
                android.location.Criteria.POWER_LOW,
                android.location.Criteria.ACCURACY_FINE);

        Location newLocation = new Location(LocationManager.GPS_PROVIDER);

        newLocation.setLatitude(latitude);
        newLocation.setLongitude(longitude);
        newLocation.setAccuracy(accuracy);

        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

        locationManager.setTestProviderStatus(LocationManager.GPS_PROVIDER,
                LocationProvider.AVAILABLE,
                null, System.currentTimeMillis());

        locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, newLocation);
    }
    */
}
