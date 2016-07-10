package com.example.utente.logmyposition;

import android.content.Context;
import android.content.Intent;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.acra.ACRA;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
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

    private ApplicationSettings applicationSettings = ApplicationSettings.getInstance();

    LogPositionLocationListener(Context context){
        this.context=context;
    }

    @Override
    public void onLocationChanged(Location location) {

        Log.e(getClass().getSimpleName(), "posizione cambiata");

        impostaVariabiliInterne(location);

        salvaDati(location);

        aggiornaMainActivity(location);

        // Aggiorno il servizio
        // Aggiorno tutte le componenti impostate su questo intent filter
        Intent intent=new Intent("AggiornaInterfaccia");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.e(getClass().getSimpleName(), "Cambio di stato");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.e(getClass().getSimpleName(), provider+" - Provider abilitato");
        aggiornaStato(true);
//        ApplicationSettings.setGPSAvailable(true);
//
//        // Aggiorno tutte le componenti impostate su questo intent filter
//        Intent intent=new Intent("AggiornaInterfaccia");
//        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.e(getClass().getSimpleName(),provider+" - Provider disabilitato");
        aggiornaStato(false);
//        ApplicationSettings.setGPSAvailable(false);
//
//        // Aggiorno tutte le componenti impostate su questo intent filter
//        Intent intent=new Intent("AggiornaInterfaccia");
//        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Aggiorna lo stato del GPS nell'applicazione ed invia i broadcast per aggiornare il resto dell'interfaccia
     * @param stato
     */
    private void aggiornaStato(boolean stato){
        applicationSettings.setGPSAvailable(false);

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

//        if (!applicationSettings.isSaveDataEnabled()){
//            return;
//        }

        // Salvo la data corrente
        Date date= new Date();
        SimpleDateFormat ft = new SimpleDateFormat ("EEE yyyy.MM.dd - HH:mm:ss ZZZZ", Locale.getDefault());

        File dataFile = applicationSettings.getFileSalvataggio();

        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());
        ACRA.getErrorReporter().putCustomData("LogPositionLocationListener - dataFile", (dataFile == null) ? "null" : dataFile.toString());

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
        sb.append(applicationSettings.getSessione().toString());
        sb.append(";");

        // Contatore punti salvati
        sb.append(applicationSettings.getPuntiSalvati());
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

        // Se location non ha il valore ritorna comunque con 0.0
        sb.append(location.getAltitude());
        sb.append(";");

        // Se location non ha il valore ritorna comunque con 0.0
        sb.append(location.getSpeed());
        sb.append(";");

        // Se location non ha il valore ritorna comunque con 0.0
        sb.append(location.getBearing());
        sb.append(";");

        // Se location non ha il valore ritorna comunque con 0.0
        sb.append(location.getAccuracy());
        sb.append(";");

        // Salvo la precisione spazia e e temporale valida al momento della registrazione
        sb.append(applicationSettings.getMinDistanceLocationUpdate());
        sb.append(";");
        sb.append(applicationSettings.getMinTimeLocationUpdate());

        sb.append("\n");

        try {
            fileOutputStream.write(sb.toString().getBytes());
            fileOutputStream.close();
            applicationSettings.incrementaPuntiSalvati();
        } catch (NullPointerException e) {
            // Non dovrei mai arrivarci a meno di cose strane fatte sul telefono come root
            e.printStackTrace();
        } catch (IOException ioe){
            // Non dovrei mai arrivarci a meno di cose strane fatte sul telefono come root
            ioe.printStackTrace();
        }
        finally {

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

