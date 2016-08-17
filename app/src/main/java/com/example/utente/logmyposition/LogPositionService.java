package com.example.utente.logmyposition;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.acra.ACRA;

import java.util.Iterator;
import java.util.Locale;
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
    Notification myNotication ;
    private int NOTIFICATION = R.string.local_service_started;

    private boolean servizioAvviato=false;

    private LocationManager locationManager;
    private LocationProvider locationProvider;
    private LogPositionLocationListener locationListener;

    // I singleton in Android possono essere deallocati se l'activity è chiusa dal S.O.
    ApplicationSettings applicationSettings = ApplicationSettings.getInstance();

// fatto con un timertask (eseguiCompito)
// TODO: emetto la notifica dei punti salvati ogni 10. Da fare meglio es emettere una notifica ogni x secondi
//    private int notificationLag=0; // Indica ogni quanto devi punti emettere una notifica
//    private final int notificationLagConst=9;
    // Receiver per aggiornare la parte dell'interfaccia di competenza del servizio (es. notifica)
    // Filtro AggiornaInterfaccia
    private BroadcastReceiver mMessageFromListenerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Aggiorno l'interfaccia

//            if (notificationLag++ >= notificationLagConst) {
//                notificationLag=0;
//              //  showNotification();
//            }

        }
    };

    // Receiver per aggiornare i parametri se sono stati cambiati dalle preferenze
    // Filtro AggiornaParametri
    private BroadcastReceiver mMessageFromListenerReceiverPreferencesChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Aggiorno i parametri se sono stati modificati durante l'esecusione del servizio

            locationManager.removeUpdates(locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    applicationSettings.getMinTimeLocationUpdate(),
                    applicationSettings.getMinDistanceLocationUpdate(),
                    locationListener);
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
            case GpsStatus.GPS_EVENT_STARTED : s="GPS_EVENT_STARTED"; applicationSettings.setGPSAvailable(true); break;
            case GpsStatus.GPS_EVENT_STOPPED : s="GPS_EVENT_STOPPED"; applicationSettings.setGPSAvailable(false); break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS : s="GPS_EVENT_SATELLITE_STATUS";
                applicationSettings.setSatelliti(calcolaNumeroSatelliti());
                break;
        }
        Log.e(getClass().getSimpleName(), "Evento GPS Cambiato:"+s);

        aggiornaMainActivity();
        // TODO :Verificare se è questo che fa mostrare sempre la notifica e non ogni 30 secondi
        if (event != GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
            showNotification();
        }
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

        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());
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

        // Aggiorno l'interfaccia se è attiva
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageFromListenerReceiver,
                new IntentFilter("AggiornaInterfaccia"));

        // Aggiorno i parametri dipendenti dalle preferenze
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageFromListenerReceiverPreferencesChanged,
                new IntentFilter("AggiornaParametri"));

//        // TODEL X TESTARE ACRA
//        String s=null;
//        s.toString();
//        ///////////////////////

        return START_STICKY;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(getClass().getSimpleName(), "Distrutto");

        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageFromListenerReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageFromListenerReceiverPreferencesChanged);

        // Salvo lo stato dell'applicazione
        mNM.cancel(NOTIFICATION);
        servizioAvviato = false;
        applicationSettings.setStatoServizio(false);
        applicationSettings.savePreferences(getApplicationContext());

        // Deregistro il listener per la posizione
        locationManager.removeUpdates(locationListener);
        locationManager.removeGpsStatusListener(this);

        // Rimuovo il timer
        timer.cancel();
        timer = null;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());

        applicationSettings.savePreferences(getApplicationContext());
        Log.e("Servizio", "Poca memoria");
    }

    @Override
    public void onTrimMemory(int level){
        super.onTrimMemory(level);

        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    @Override
    public void onTaskRemoved(Intent intent){
        super.onTaskRemoved(intent);

        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());

    }



    private void eseguiCompito() {
        // Log.e(getClass().getSimpleName(), "running");
        showNotification();
        if (BuildConfig.DEBUG  && ApplicationSettings.MOCKLOCATION){
            double la=Math.random()*60;
            double lo=Math.random()*20;
            mock.pushLocation(la,lo);
            Log.e(getClass().getSimpleName(),"Mocking..."+
            String.format(Locale.ITALY,"%f",la)+ " - "+ String.format(Locale.ITALY,"%f",lo)
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

        String sTicker="Punti salvati: " + applicationSettings.getPuntiSalvati();
        //text+=" "+sTicker+"\n";

        text+=(applicationSettings.isGPSAvailable()?" (GPS Abilitato)":" (GPS Disabilitato)");

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainTabbedActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setAutoCancel(false);
        builder.setTicker(sTicker);
        builder.setSubText(sTicker);
        builder.setShowWhen(true);
        builder.setContentTitle(getText(R.string.local_service_label));
        builder.setContentText(text);

        // Log.e(Thread.currentThread().getStackTrace()[2].getMethodName()+" - 2",phrase);
//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.compass_rose_md);
//        builder.setLargeIcon(bitmap);
        builder.setSmallIcon(R.drawable.ic_main_w);
        builder.setContentIntent(contentIntent);
        builder.setOngoing(true);
        // builder.setSubText("Nuovo punto registrato");   //API level 16

        //builder.addAction(R.drawable.ic_account_balance_black_18dp,"Condividi", pendingIntentBtn2);

        builder.setOnlyAlertOnce(false);

        builder.setStyle(new Notification.BigTextStyle(builder).bigText(text));

        myNotication= builder.build();

        // myNotication.flags |= Notification.FLAG_INSISTENT;
        myNotication.flags |= Notification.VISIBILITY_PUBLIC;

        mNM.notify(NOTIFICATION, myNotication);
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
        int used=0;

        final Iterator<GpsSatellite> it = gs.getSatellites().iterator();
        while (it.hasNext()) {
            GpsSatellite gpsSatellite=it.next();
            // it.next();
            if(gpsSatellite.getSnr()>0.0f){
                used++;
            }
            i++;
        }
        //applicationSettings.setSatelliti(i,gs.getMaxSatellites());
        applicationSettings.setSatelliti(used, i);

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

        // TODO: Costante cablata per la temporizzazione
        timer.schedule(timerTask, 0, 30000);
    }

    private void avviaServizio(){
        if (BuildConfig.DEBUG && ApplicationSettings.MOCKLOCATION){
            mock = new MockLocationProvider(
                    LocationManager.GPS_PROVIDER, getApplicationContext());
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.addGpsStatusListener(this);

        locationProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);

        locationListener = new LogPositionLocationListener(getApplicationContext());
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                applicationSettings.getMinTimeLocationUpdate(),
                applicationSettings.getMinDistanceLocationUpdate()
                , locationListener);

        servizioAvviato=true;
        applicationSettings.setStatoServizio(true);
        applicationSettings.setGPSAvailable(locationManager.isProviderEnabled("gps"));

        impostaAttivitaTemporizzata();

        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();

        aggiornaMainActivity();
    }
}
