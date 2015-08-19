package com.example.utente.logmyposition;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.NetworkOnMainThreadException;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by utente on 14/08/2015.
 *
 * Applico il pattern Singleton thread safe con double check
 * Conservo qui tutte le variabili che mi servono nel prosieguo dell'applicazione
 */
public class ApplicationSettings {
    private static ApplicationSettings MYSELF=null ;
    private static Context context;

    private final static String PREFNAME="LogMyPosition";
    private final static String STATOSERVIZIO="STATOSERVIZIO";
    private final static String STATOFILESALVATAGGIO="STATOFILESALVATAGGIO";

    /*
    // Id per trattare la notifica
    private static final int NOTIFICATION = R.string.local_service_started;
*/

    private static SharedPreferences sharedPreferences=null;
    private static SharedPreferences.Editor editor=null;

    private static boolean servizioLogAttivato=false;
    private static boolean GPSAvailable =false;
    private static File fileSalvataggio=null;
    private static long minTimeLocationUpdate=1000;        // 1 minuto per default tra un aggiornamento e l'altro
    private static float minDistanceLocationUpdate = 10.0f; // 10 metri per default tra un aggiornamento e l'altro

    private static int maxSatelliti=0;
    private static int numSatelliti=0;

    /**
     * Costruttore privato per il pattern Singleton
     */
    private ApplicationSettings(){}

//    /**
//     * Ritorna l'ID dela notifica unico per tutta l'applicazione
//     * @return
//     */
//    public static int getNotificationID(){return NOTIFICATION;}

    /**
     * imposta il numero dei satelliti rilevati
     * @param num
     */
    public static void setSatelliti(int num){
        setSatelliti(num, maxSatelliti);
    }

    /**
     * imposta il numero dei satelliti rilevati ed il massimo rilevato
     * @param num
     * @param max
     */
    public static void setSatelliti (int num, int max){
        maxSatelliti=max;
        numSatelliti=num;
    }

    /**
     * Ritorna il numero dei satelliti rilevati
     * @return
     */
    public static int getSatelliti(){return numSatelliti;}

    /**
     * ritorna il massimo del numero dei satelliti rilevati
     * @return
     */
    public static int getMaxSatelliti(){return maxSatelliti;}

    public static boolean isGPSAvailable(){return GPSAvailable ;}
    public static void setGPSAvailable(boolean stato){GPSAvailable =stato;}

    public static boolean isServiceEnabled(){return servizioLogAttivato;}

    public static void setStatoServizio(boolean stato){
        servizioLogAttivato=stato;
    }

    public static long getMinTimeLocationUpdate(){return minTimeLocationUpdate;}

    public static float getMinDistanceLocationUpdate(){return minDistanceLocationUpdate;}

    public static void savePreferences(Context context){
        sharedPreferences = context.getSharedPreferences(PREFNAME,0);
        editor=sharedPreferences.edit();
        synchronized (editor) {
           // editor.putBoolean(STATOSERVIZIO, servizioLogAttivato);
            editor.putString(STATOFILESALVATAGGIO, fileSalvataggio.getAbsolutePath());
            editor.commit();
        }
    }

    public static void loadPreferences(Context context){
        sharedPreferences = context.getSharedPreferences(PREFNAME, 0);
        Map<String, ?> values = null;
        String filePath=null;

        try {
            values = sharedPreferences.getAll();
        } catch (NullPointerException npe){
            Log.e("loadPreferences", "preferenze non esistenti");
        }

        // Prelevo i dati solo se ci sono nelle shared prefs
        if (values!=null && values.size()!=0) {
            //servizioLogAttivato = (Boolean) values.get(STATOSERVIZIO);
            filePath = (String) values.get(STATOFILESALVATAGGIO);
        }

        // TODO: rendere il file parametrico per es. a livello giornaliero o far scegliere all'utente
        // fileSalvataggio = new File(context.getFilesDir(), "pippo.txt");
        // Creo una dir sulla SD
        File sd = new File(Environment.getExternalStorageDirectory() + "/LogMyPosition");
        boolean successCreaDir = true;
        if (!sd.exists()) {
            successCreaDir = sd.mkdir();
        }

        // Se non riesco a creare la directory metto tutto nella subdir dell'App
        if (!successCreaDir)
            fileSalvataggio = new File(context.getFilesDir(), "pippo.txt");
        else
            fileSalvataggio = new File(sd,"pippo.txt" );

        if (!fileSalvataggio.exists()) {
            try {
                fileSalvataggio.createNewFile();
            } catch (IOException ioe) {
                Log.e("Errore", "Creazione file - " + filePath + " - non riuscita");
            }
        }
        fileSalvataggio.setReadable(true,false);


        // LEggo le shared_prefs impostate dall'activity SettingsActivity.java
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        minDistanceLocationUpdate=Float.valueOf (sp.getString("sync_space","10"));
        minTimeLocationUpdate=Long.valueOf (sp.getString("sync_frequency","30"));
    }

    public static File getfileSalvataggio(){return fileSalvataggio;}

    public static ApplicationSettings getInstance(){
        if (MYSELF==null){
            synchronized (ApplicationSettings.class){
                if (MYSELF==null){
                    MYSELF=new ApplicationSettings();
                }
            }
        }
        return MYSELF;
    }
}
