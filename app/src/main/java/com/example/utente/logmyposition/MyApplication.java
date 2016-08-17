package com.example.utente.logmyposition;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
//import org.acra.ACRAConfiguration;
import org.acra.ACRAConstants;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.log.ACRALog;
import org.acra.sender.HttpSender;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

/**
 * Created by utente on 01/09/2015.
 */
@ReportsCrashes(
//        mailTo = "prikeprok@gmail.com",
//        customReportContent = { ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL,
//                ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT, ReportField.SETTINGS_GLOBAL, ReportField.DEVICE_FEATURES,
//                ReportField.SETTINGS_SECURE, ReportField.SETTINGS_SYSTEM, ReportField.SHARED_PREFERENCES, ReportField.THREAD_DETAILS
//        },
//        mode = ReportingInteractionMode.TOAST,
//        resToastText = R.string.crash_report
/*
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "http://pippokennedy.iriscouch.com/acra-logmyposition/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "pippo",
        formUriBasicAuthPassword = "kennedy",
*/
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "https://pippokennedy.cloudant.com/acra-logmyposition/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "ndespecierislooksoressil",
        formUriBasicAuthPassword = "697ce7bf841ec8cce83899c29b8f1d804587c86d",

        customReportContent = {
                // Campi obblicatori per acralyzer
                ReportField.REPORT_ID, ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.ANDROID_VERSION,ReportField.PACKAGE_NAME,ReportField.BUILD, ReportField.STACK_TRACE,

                // Campo per sapere il cellulare su cui è installato
                ReportField.INSTALLATION_ID,

                // Campi utili per avere altre info
                ReportField.PHONE_MODEL,ReportField.CUSTOM_DATA, ReportField.LOGCAT, ReportField.SETTINGS_GLOBAL, ReportField.DEVICE_FEATURES,
                ReportField.SETTINGS_SECURE, ReportField.SETTINGS_SYSTEM, ReportField.SHARED_PREFERENCES, ReportField.THREAD_DETAILS
        },
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_report
)
public class MyApplication extends Application{
    // Comunque si faccia i Singleton in Android non sono affidabili. Se l'applicazione passa in background
    // Vengono reinizializzati
    ApplicationSettings applicationSettings ;

    // File che memorizza una UUID e verificare se l'applicazione è già stata eseguita
    private static final String INSTALLATION = "INSTALLATO";

    /**
     * Verifica se l'applicazione è già stata lanciata e comunica il first run tramite ACRA
     */
    private void checkFirstRunAndSendData(){
        Context context = getApplicationContext();

        // Test per vedere il first run
        File installation = new File(context.getFilesDir(), INSTALLATION);
        if (!installation.exists()) {
            // Provo a comunicare i dati al server. Se non ci riesco resta in first run
            ACRA.getErrorReporter().handleSilentException(new Throwable("Primo avvio applicazione"));

            try {
                FileOutputStream out = new FileOutputStream(installation);

                String id = UUID.randomUUID().toString()+"\n";
                out.write(id.getBytes());
                out.close();
            }catch (Exception e){
                throw new RuntimeException(e);
            }
            // Se arrivo qui allora ho comunicato i dati e scrivo il file
        }
    }

    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        ACRA.init(this);

        // Inizializzo il Singleton e mi salvo una istanza
        ApplicationSettings.initInstance();
        applicationSettings=ApplicationSettings.getInstance();

        // Verifico se è la prima esecuzione e provo a mandare i dati
        checkFirstRunAndSendData();
    }

//    protected void finalize(){
//        applicationSettings=ApplicationSettings.getInstance();
//    }


    @Override
    public void onTerminate(){
        super.onTerminate();
        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    @Override
    public void onLowMemory(){
        super.onLowMemory();
        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    @Override
    public void onTrimMemory(int level){
        super.onTrimMemory(level);

        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());
        String s="";
        switch (level){
            case TRIM_MEMORY_BACKGROUND :s="TRIM_MEMORY_BACKGROUND"; break;
            case TRIM_MEMORY_COMPLETE : s="TRIM_MEMORY_COMPLETE"; break;
            case TRIM_MEMORY_MODERATE : s="TRIM_MEMORY_MODERATE"; break;
            case TRIM_MEMORY_RUNNING_CRITICAL : s="TRIM_MEMORY_RUNNING_CRITICAL"; break;
            case TRIM_MEMORY_RUNNING_LOW : s="TRIM_MEMORY_RUNNING_LOW"; break;
            case TRIM_MEMORY_RUNNING_MODERATE : s="TRIM_MEMORY_RUNNING_MODERATE"; break;
            case TRIM_MEMORY_UI_HIDDEN : s="TRIM_MEMORY_UI_HIDDEN"; break;
        }

        ACRA.getErrorReporter().putCustomData("MyApplication -> TrimMemoryLevel",s);
    }

}
