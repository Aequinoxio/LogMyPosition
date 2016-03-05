package com.example.utente.logmyposition;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.NetworkOnMainThreadException;
import android.preference.PreferenceManager;
import android.util.Log;

import org.acra.ACRA;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Created by utente on 14/08/2015.
 *
 * Applico il pattern Singleton thread safe con double check
 * Conservo qui tutte le variabili che mi servono nel prosieguo dell'applicazione
 */
public class ApplicationSettings {
    private static ApplicationSettings MYSELF=null ;
    private static Context context;

    private final  String PREFNAME="LogMyPosition";
    private final  String STATOSERVIZIO="STATOSERVIZIO";
    private final  String STATOFILESALVATAGGIO="STATOFILESALVATAGGIO";

    public static final boolean MOCKLOCATION=false;

    /*
    // Id per trattare la notifica
    private static final int NOTIFICATION = R.string.local_service_started;
*/

    private  SharedPreferences sharedPreferences=null;
    private  SharedPreferences.Editor editor=null;

    private  boolean servizioLogAttivato=false;
    private  boolean GPSAvailable =false;
    private  File fileSalvataggio=null;
    private  long minTimeLocationUpdate=1000;        // 1 minuto per default tra un aggiornamento e l'altro
    private  float minDistanceLocationUpdate = 10.0f; // 10 metri per default tra un aggiornamento e l'altro

    private  int maxSatelliti=0;
    private  int numSatelliti=0;

    private  long puntiSalvati=0;

    // Imposto comunque una UUID
    private  UUID sessione=generaSessione();

    /**
     * Costruttore privato per il pattern Singleton
     */
    private ApplicationSettings(){
        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis() + " -> " + Thread.currentThread().getStackTrace()[2].getClassName().replace(".", "_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    /**
     * Imposto il distruttore per cercare di capire come mai viene distrutta la variabile FILE
     */
    protected void finalize(){
        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis() + " -> " + Thread.currentThread().getStackTrace()[2].getClassName().replace(".", "_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    /**
     * imposta il numero dei satelliti rilevati
     * @param num
     */
    public  void setSatelliti(int num){
        setSatelliti(num, maxSatelliti);
    }

    /**
     * Genera una sessione randomica
     * @return UUID generato
     */
    public UUID generaSessione(){
        sessione=UUID.randomUUID();
        return sessione;
    }

    /**
     * Ritorna la sessione generata
     * @return
     */
    public UUID getSessione(){return sessione;}

    /**
     * Resetta il numero di punti salvati
     */
    public void resetPuntiSalvati(){puntiSalvati=0;}

    /**
     * Incrementa di uno il numero di punti salvati e li ritorna
     * @return
     */
    public long incrementaPuntiSalvati(){puntiSalvati++; return puntiSalvati;}

    /**
     * Ritorna il numero di punti salvati
     * @return
     */
    public long getPuntiSalvati(){return puntiSalvati;}

    /**
     * imposta il numero dei satelliti rilevati ed il massimo rilevato
     * @param num
     * @param max
     */
    public void setSatelliti (int num, int max){
        maxSatelliti=max;
        numSatelliti=num;
    }

    /**
     * Ritorna il numero dei satelliti rilevati
     * @return
     */
    public int getSatelliti(){return numSatelliti;}

    /**
     * ritorna il massimo del numero dei satelliti rilevati
     * @return
     */
    public int getMaxSatelliti(){return maxSatelliti;}

    public boolean isGPSAvailable(){return GPSAvailable ;}
    public void setGPSAvailable(boolean stato){GPSAvailable =stato;}

    public boolean isServiceEnabled(){return servizioLogAttivato;}

    public void setStatoServizio(boolean stato){
        servizioLogAttivato=stato;
    }

    public long getMinTimeLocationUpdate(){return minTimeLocationUpdate;}

    public float getMinDistanceLocationUpdate(){return minDistanceLocationUpdate;}

    public void savePreferences(Context context){

        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis(), "ApplicationSettings.savePreferences");
        ACRA.getErrorReporter().putCustomData("ApplicationSettings - sharedPreferences", (sharedPreferences==null)?"null":sharedPreferences.toString());

        sharedPreferences = context.getSharedPreferences(PREFNAME,0);
        editor=sharedPreferences.edit();

        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("ApplicationSettings - editor", (editor == null) ? "null" : editor.toString());
        ACRA.getErrorReporter().putCustomData("ApplicationSettings - fileSalvataggio", (fileSalvataggio == null) ? "null" : fileSalvataggio.toString());

        synchronized (editor) {
           // editor.putBoolean(STATOSERVIZIO, servizioLogAttivato);
            editor.putString(STATOFILESALVATAGGIO, fileSalvataggio.getAbsolutePath());
            editor.commit();
        }
    }

    public void loadPreferences(Context context){
        sharedPreferences = context.getSharedPreferences(PREFNAME, 0);
        Map<String, ?> values = null;
        String filePath=null;

        try {
            values = sharedPreferences.getAll();
        } catch (NullPointerException npe){
            Log.e("loadPreferences", "Preferenze non esistenti");
        }

//        // Prelevo i dati solo se ci sono nelle shared prefs
//        if (values!=null && values.size()!=0) {
//            //servizioLogAttivato = (Boolean) values.get(STATOSERVIZIO);
//            filePath = (String) values.get(STATOFILESALVATAGGIO);
//        }

        // Leggo le shared_prefs impostate dall'activity SettingsActivity.java
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        // TODO: Costanti di default cablate
        minDistanceLocationUpdate=Float.valueOf (sp.getString("sync_space","10"));
        minTimeLocationUpdate=Long.valueOf (sp.getString("sync_frequency","30"));
    }

    public File getFileSalvataggio(){
    /* TODO: Workaround per evitare un nullpointer exception quanto adnroid, in sovraccarico e con poca memoria,
        dealloca e reinizializza il singleton(!!!)
     */
        if (fileSalvataggio==null)
            setFileSalvataggio();

        return fileSalvataggio;
    }

    public void setFileSalvataggio(){
        Date date= new Date();
        SimpleDateFormat ft = new SimpleDateFormat ("yyyyMMdd", Locale.getDefault());

        // TODO: Costante estensione cablata
        String filenameGiornaliero=ft.format(date)+".txt";
        // TODO: rendere il file parametrico per es. a livello giornaliero o far scegliere all'utente
        // fileSalvataggio = new File(context.getFilesDir(), "pippo.txt");
        // Creo una dir sulla SD
        File sd = new File(Environment.getExternalStorageDirectory() + "/LogMyPosition");

        // Provo a creare la directory sulla sd
        boolean successCreaDir = true;
        if (!sd.exists()) {
            successCreaDir = sd.mkdir();
        }

        // Se non riesco a creare la directory metto tutto nella subdir dell'App
        if (!successCreaDir)
            fileSalvataggio = new File(context.getFilesDir(), filenameGiornaliero);
        else
            fileSalvataggio = new File(sd,filenameGiornaliero );

        if (!fileSalvataggio.exists()) {
            try {
                fileSalvataggio.createNewFile();
            } catch (IOException ioe) {
                Log.e("Errore", "Creazione file - " + fileSalvataggio.getAbsolutePath() + " - non riuscita");
            }
        }
        fileSalvataggio.setReadable(true,false);
    }

    public static void initInstance(){
        if (MYSELF==null){
            synchronized (ApplicationSettings.class){
                if (MYSELF==null){
                    MYSELF=new ApplicationSettings();
                }
            }
        }

        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    public static ApplicationSettings getInstance(){
//        if (MYSELF==null){
//            synchronized (ApplicationSettings.class){
//                if (MYSELF==null){
//                    MYSELF=new ApplicationSettings();
//                }
//            }
//        }
        return MYSELF;
    }
}
