package com.example.utente.logmyposition;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.content.res.Resources;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by utente on 15/08/2015.
 *
 * extends Application
 */
public class LogPositionLocationListener implements LocationListener {

    // Provo a limitare i passaggi per i broadcast intent permettendo di leggere direttamente queste variabili
    protected static double lat=0,lon=0,alt=0;
    protected static float  vel=0,dir=0,acc=0;
    protected static long   tempo=0;

    private Context context=null;  // Provo a salvare il contesto quando creo l'oggetto

    LogPositionLocationListener(Context context){
        this.context=context;
    }
    @Override
    public void onLocationChanged(Location location) {

        Log.e(getClass().getSimpleName(), "posizione cambiata");

        impostaVariabiliInterne(location);

        salvaDati(location);

        aggiornaMainActivity(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.e(getClass().getSimpleName(),"Cambio di stato");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.e(getClass().getSimpleName(), provider+" - Provider abilitato");
        ApplicationSettings.setGPSAvailable(true);

        // Aggiorno tutte le componenti impostate su questo intent filter
        Intent intent=new Intent("AggiornaInterfaccia");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.e(getClass().getSimpleName(),provider+" - Provider disabilitato");
        ApplicationSettings.setGPSAvailable(false);

        // Aggiorno tutte le componenti impostate su questo intent filter
        Intent intent=new Intent("AggiornaInterfaccia");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }


    /**
     * Aggiorno la main activity
     *
     * @param location
     */
    private void aggiornaMainActivity(Location location){
        // Informo la main activity
        Intent intent = new Intent("AggiornoMainActivity");
        // Dati
        intent.putExtra("latitudine",location.getLatitude());
        intent.putExtra("longitudine",location.getLongitude());
        intent.putExtra("altitudine",location.getAltitude());

        intent.putExtra("tempo",location.getTime());

        intent.putExtra("velocita",location.getSpeed());
        intent.putExtra("direzione", location.getBearing());
        intent.putExtra("accuratezza", location.getAccuracy());

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
    /**
     *
     * @param location
     *
     * Salvo la posizione.
     * Prerequisito aver impostato in ApplicationSettings il file
     *
     */
    private void salvaDati(Location location) {
        // TODO: usare un file temp e salvare in formato GPX

        // TODO: metodo grezzo da migliorare
        // Tracciato
        /*
            UUID_Sessione;contatore; data locale;tempo_GPS;latitudine;longitudine;altitudine;velocità;orientamento; accuratezza
         */
        // Salvo la data corrente
        Date date= new Date();
        SimpleDateFormat ft = new SimpleDateFormat ("EEE yyyy.MM.dd - HH:mm:ss ZZZZ", Locale.getDefault());

        File dataFile = ApplicationSettings.getfileSalvataggio();
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(dataFile.getAbsolutePath(), true);
        } catch (FileNotFoundException fnfe) {
            // Non dovrebbe mai essere lanciata a meno di cose strane durante l'esecuzione del servizio es. cancellazione file
            // tramite gestione file e da root
            fnfe.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();

        // UUID sessione
        sb.append(ApplicationSettings.getSessione().toString());
        sb.append(";");

        // Contatore punti salvati
        sb.append(ApplicationSettings.getPuntiSalvati());
        sb.append(";");

        // Data attuale
        sb.append(ft.format(date));
        sb.append(";");

        sb.append(location.getTime());
        sb.append(";");

        sb.append(location.getLatitude());
        sb.append(";");

        sb.append(location.getLongitude());
        sb.append(";");

        // Se location non ha il valore ritorna cominque con 0.0
        sb.append(location.getAltitude());
        sb.append(";");

        // Se location non ha il valore ritorna cominque con 0.0
        sb.append(location.getSpeed());
        sb.append(";");

        // Se location non ha il valore ritorna cominque con 0.0
        sb.append(location.getBearing());
        sb.append(";");

        // Se location non ha il valore ritorna cominque con 0.0
        sb.append(location.getAccuracy());
        sb.append("\n");

        try {
            fileOutputStream.write(sb.toString().getBytes());
            ApplicationSettings.incrementaPuntiSalvati();
        } catch (NullPointerException | IOException e) {
            // Non dovrei mai arrivarci a meno di cose strane sul telefono
            e.printStackTrace();
        } finally {

        }
    }

    /**
     * Imposta le variabili interne all'ultima posizione nota
     * TODO: da migliorare
     * @param location
     */
    private void impostaVariabiliInterne(Location location){
        lat=location.getLatitude();
        lon=location.getLongitude();
        alt=location.getAltitude();

        vel=location.getSpeed();
        dir=location.getBearing();
        acc=location.getAccuracy();
        tempo=location.getTime();
    }
}

