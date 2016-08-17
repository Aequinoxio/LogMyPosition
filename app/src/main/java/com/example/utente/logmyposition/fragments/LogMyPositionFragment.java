package com.example.utente.logmyposition.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.utente.logmyposition.ApplicationSettings;
import com.example.utente.logmyposition.ApplicationUtility;
import com.example.utente.logmyposition.GpsSatellitesStatusActivity;
import com.example.utente.logmyposition.LogPositionLocationListener;
import com.example.utente.logmyposition.LogPositionService;
import com.example.utente.logmyposition.R;
import com.example.utente.logmyposition.ShowLogFileContentlistView;
import com.example.utente.logmyposition.SimpleSettingsActivity;

import org.acra.ACRA;

import java.io.File;
import java.util.Locale;

public class LogMyPositionFragment extends Fragment {

    private double lat=0,lon=0,alt=0;
    private float  vel=0,dir=0,acc=0;
    private long   tempo=0;

    private Context mContext=null;
    private Activity mActivity=null;
    private View rootView=null;

    // Mi servono pe rimpostare i listeners
    private Button btnStartStopService;

    private TextView txtLogFileName;

    private final int SETTINGS_RESULTCODE=1234;

    private LocationManager locationManager=null;

    // Mantengo il collegamneto con il servizio attivato
    private LogPositionService logPositionService=null;
    private boolean mIsBound;

    // N.B. Se android ha bisono di memporia dealloca il singleton
    private ApplicationSettings applicationSettings = ApplicationSettings.getInstance();


    // handler for received Intents for the "AggiornoMainActivity" event
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            lat=intent.getDoubleExtra("latitudine",0);
            lon=intent.getDoubleExtra("longitudine",0);
            alt=intent.getDoubleExtra("altitudine", 0);

            vel=intent.getFloatExtra("velocita", 0);
            dir=intent.getFloatExtra("direzione",0);
            acc=intent.getFloatExtra("accuratezza", 0);

            tempo=intent.getLongExtra("tempo", 0);

            aggiornaValoriStatoGPS();

            aggiornaInterfaccia();

            Log.e("MainActivityFr receiver", "Messaggio ricevuto");
        }
    };

    // handler for received Intents for the "AggiornaInterfaccia" event
    private BroadcastReceiver mMessageFromServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Aggiorno l'interfaccia
            aggiornaInterfaccia();
        }
    };

//    /**
//     * Aspetto qui che la sotto attività termini
//     *
//     * @param requestCode
//     * @param resultCode
//     * @param data
//     */
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data)
//    {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if(requestCode==SETTINGS_RESULTCODE)
//        {
//            // Ricarico le preferences
//            applicationSettings.loadPreferences(mContext);
//
//            // Aggiorno il servizio sulla base delle preferenze
//            Intent intent = new Intent("AggiornaParametri");
//            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
//        }
//    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        if (mActivity==null){
            mActivity=getActivity();
            mContext=getActivity().getApplicationContext();
        }
        //       Log.e("***********DEBUG?", BuildConfig.DEBUG ? "SI" : "NO");

        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());

        locationManager = (LocationManager) mActivity.getSystemService(Activity.LOCATION_SERVICE);

//        aggiornaInterfaccia();

        this.rootView = rootView;

        impostaClickListeners();

        return rootView;
    }

    /**
     * Onclick listener nell' XML funziona solo per le activity. Imposto qui i listener locali che mi servono
     */
    private void impostaClickListeners(){
        btnStartStopService = (Button) rootView.findViewById(R.id.btnStartStopService);
        btnStartStopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStopLogService(rootView);
            }
        });

        txtLogFileName =(TextView) rootView.findViewById(R.id.txtLogFileName);
        txtLogFileName .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogFile(rootView);
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        aggiornaInterfaccia();
    }

    //    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            Intent intentSettings= new Intent(mContext,SimpleSettingsActivity.class);
//            startActivityForResult(intentSettings, SETTINGS_RESULTCODE);
//
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    /**
     * Listener per mostrare il log file
     * @param v
     */
    private void showLogFile(View v){
       // Intent intent = new Intent(mContext, ShowLogFileContent.class);
        Intent intent = new Intent(mContext, ShowLogFileContentlistView.class);

        startActivity(intent);
    }

//    private void startGPSService(){
//        mActivity.startService(new Intent(mContext, LogPositionService.class));
//    }

//    /**
//     * Avvia l'attività per mostrare lo stato dei satelliti
//     * @param v
//     */
//    public void startGpsSatActivity(View v){
//        Intent intent = new Intent(mContext,GpsSatellitesStatusActivity.class);
//        startActivity(intent);
//    }


    /**
     * Listener per avviare e fermare il servizio. Collegato al togglebutton btnStartStopService
     * @param v
     */
    public void startStopLogService(View v) {
        ToggleButton serviceStartButton = (ToggleButton) rootView.findViewById(R.id.btnStartStopService);
        ProgressBar progressBar = (RatingBar) rootView.findViewById(R.id.progressBarServiceRunning);
        TextView textView = (TextView) rootView.findViewById(R.id.txtStatoLogging);

        // Verifico lo stato pre attivazione del servizio
        boolean servizioAvviato = applicationSettings.isServiceEnabled();

        // Avvio il servizio
        if (!servizioAvviato) {
            // Ad ogni nuovo avvio resetto i punti e genero un nuovo ID di sessione
            // Utile per tenere traccia dei vari segmenti
            applicationSettings.generaSessione();
            applicationSettings.resetPuntiSalvati();
            getActivity().startService(new Intent(mContext, LogPositionService.class));
        } else{
            getActivity().stopService(new Intent(mContext, LogPositionService.class));
        }

        servizioAvviato=!servizioAvviato;

        progressBar.setVisibility(ApplicationUtility.getServiceStatusProgressVisibility(mContext, servizioAvviato));

        // Lo stato di attivazione lo imposta il servizio stesso
        serviceStartButton.setText(ApplicationUtility.getServiceStatusButtonLabel(mContext, servizioAvviato));
        serviceStartButton.setChecked(servizioAvviato);

        textView.setText(ApplicationUtility.getServiceStatusTextLabel(mContext, servizioAvviato));
    }

    public void goInBackground(View v) {
        applicationSettings.savePreferences(mContext);
        getActivity().finish();
    }

//    @Override
//    public void onTrimMemory(int level){
//        super.onTrimMemory(level);
//
//        // Salvo alcune variabili per debug
//        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
//                Thread.currentThread().getStackTrace()[2].getMethodName());
//    }

    @Override
    public void onLowMemory(){
        super.onLowMemory();

        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());

    }

//    @Override
//    public void onRestart(){
//        super.onRestart();
//
//        // Salvo alcune variabili per debug
//        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
//                Thread.currentThread().getStackTrace()[2].getMethodName());
//
//    }

    @Override
    public void onStart(){
        super.onStart();
        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());

    }

    @Override
    public void onStop(){
        super.onStop();
        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());

    }

    @Override
    public void onResume(){
        super.onResume();

        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());

        aggiornaInterfaccia();

        // Un po' sporca ma efficace
        // Recupero l'ultima posizione memorizzata accedendo a variabili protected
        // Di norma mi affido ad un broadcast receiver
        if (applicationSettings.isServiceEnabled()) {
            lat = LogPositionLocationListener.lat;
            lon = LogPositionLocationListener.lon;
            alt = LogPositionLocationListener.alt;
            acc = LogPositionLocationListener.acc;
            dir = LogPositionLocationListener.dir;
            tempo = LogPositionLocationListener.tempo;
            vel = LogPositionLocationListener.vel;

            aggiornaValoriStatoGPS();
        }
        Log.e(getClass().getSimpleName(), "Ripreso");


        LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver,
                new IntentFilter("AggiornoMainActivity"));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageFromServiceReceiver,
                new IntentFilter("AggiornaInterfaccia"));

        /*
        // TODO: da eliminare se uso le variabili statiche nel position listener?
        // Recupero i dati freschi
        Intent intent = new Intent("MandamiIDati");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        */
    }

    @Override
    public void onPause(){
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageFromServiceReceiver);
        super.onPause();

        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());

        applicationSettings.savePreferences(mContext);
        Log.e(this.getClass().getSimpleName(), "In pausa");
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());

//        // TODO: verificare se è compatibile con i metodi startService e stopService
//        doUnbindService();

        Log.e(this.getClass().getSimpleName(),"Distrutta");
    }

    private void aggiornaInterfaccia() {
        applicationSettings.loadPreferences(mContext);

        TextView textView = (TextView) rootView.findViewById(R.id.txtStatoLogging);
        textView.setText(ApplicationUtility.getServiceStatusTextLabel(mContext, applicationSettings.isServiceEnabled()));

        textView = (TextView) rootView.findViewById(R.id.valNumSat);
        textView.setText(
                String.format(Locale.ITALY,"%d / %d", applicationSettings.getSatelliti(),applicationSettings.getMaxSatelliti())
        );

        textView=(TextView)rootView.findViewById(R.id.txtStatoGPS);
        textView.setText(ApplicationUtility.getServiceStatusTextLabel(mContext, locationManager.isProviderEnabled("gps")));

        ToggleButton button = (ToggleButton) rootView.findViewById(R.id.btnStartStopService);
        button.setText(ApplicationUtility.getServiceStatusButtonLabel(mContext, applicationSettings.isServiceEnabled()));
        button.setChecked(applicationSettings.isServiceEnabled());

        RatingBar progressBar = (RatingBar) rootView.findViewById(R.id.progressBarServiceRunning);
        if (applicationSettings.isServiceEnabled()) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(1);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }

        File f = applicationSettings.getFileSalvataggio();
        if (f.exists()){
            textView = (TextView) rootView.findViewById(R.id.txtLogFileName);
            textView.setText(f.getAbsolutePath());

            // TODO: Costanti di unità di misura cablate. Farle scegliere all'utente
            textView = (TextView) rootView.findViewById(R.id.valFileSize);
            textView.setText(Long.toString(f.length())+" bytes");

//            textView.setText( Html.fromHtml("<a href=\"content://" + f.getAbsolutePath() + "\">" + f.getAbsolutePath() + "</a>"));
//            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }

        // TODO: solo per testare le settings preferences class
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);

        String s = sp.getString("sync_frequency",Long.toString(applicationSettings.getMinTimeLocationUpdate()));
        textView = (TextView) rootView.findViewById(R.id.valPollingTime);
        textView.setText(s);

        s = sp.getString("sync_space",String.format(Locale.ITALY,"%.0f",applicationSettings.getMinDistanceLocationUpdate()));
        textView = (TextView) rootView.findViewById(R.id.valPollingSpace);
        textView.setText(s);

//        s=Integer.toString(ApplicationSettings.getSatelliti());
//        textView = (TextView) rootView.findViewById(R.id.valNumSat);
//        textView.setText(s);

        s=Long.toString(applicationSettings.getPuntiSalvati());
        textView = (TextView) rootView.findViewById(R.id.valPuntiSalvati);
        textView.setText(s);

        textView = (TextView)rootView.findViewById(R.id.txtSessionUUID);
        textView.setText(applicationSettings.getSessione().toString());
    }

    private void aggiornaValoriStatoGPS(){
        TextView textView;
        textView = (TextView) rootView.findViewById(R.id.valAltitudine);
        textView.setText(String.format(Locale.ITALY,"%.0f",alt));
        textView = (TextView) rootView.findViewById(R.id.valLatitudine);
        textView.setText(String.format(Locale.ITALY,"%f",lat));
        textView = (TextView) rootView.findViewById(R.id.valLongitudine);
        textView.setText(String.format(Locale.ITALY,"%f",lon));

        textView = (TextView) rootView.findViewById(R.id.valBussola);
        textView.setText(String.format(Locale.ITALY,"%.0f",dir));
        textView = (TextView) rootView.findViewById(R.id.valVelocita);
        textView.setText(String.format(Locale.ITALY,"%.0f",vel));
        textView = (TextView) rootView.findViewById(R.id.valPrecisione);
        textView.setText(String.format(Locale.ITALY,"%.0f",acc));
        textView = (TextView) rootView.findViewById(R.id.valTempo);
        textView.setText(String.format(Locale.ITALY,"%d",tempo));
    }

    ///////////////// Gestione comunicazione con il servizio
    // vedi file:///C:/Program%20Files%20%28x86%29/Android/android-sdk/docs/reference/android/app/Service.html
    // TODO: da sistemare
//    private ServiceConnection mConnection = new ServiceConnection() {
//        public void onServiceConnected(ComponentName className, IBinder service) {
//            // This is called when the connection with the service has been
//            // established, giving us the service object we can use to
//            // interact with the service.  Because we have bound to a explicit
//            // service that we know is running in our own process, we can
//            // cast its IBinder to a concrete class and directly access it.
//            logPositionService = ((LogPositionService.LocalBinder)service).getService();
//
//            // Tell the user about this for our demo.
//            Toast.makeText(mContext, R.string.local_service_connected,
//                    Toast.LENGTH_SHORT).show();
//        }
//
//        public void onServiceDisconnected(ComponentName className) {
//            // This is called when the connection with the service has been
//            // unexpectedly disconnected -- that is, its process crashed.
//            // Because it is running in our same process, we should never
//            // see this happen.
//            logPositionService = null;
//            Toast.makeText(mContext, R.string.local_service_disconnected,
//                    Toast.LENGTH_SHORT).show();
//        }
//    };
//
//    void doBindService() {
//        // Establish a connection with the service.  We use an explicit
//        // class name because we want a specific service implementation that
//        // we know will be running in our own process (and thus won't be
//        // supporting component replacement by other applications).
//        bindService(new Intent(this,
//                LogPositionService.class), mConnection, Context.BIND_AUTO_CREATE);
//        mIsBound = true;
//    }
//
//    void doUnbindService() {
//        if (mIsBound) {
//            // Detach our existing connection.
//            unbindService(mConnection);
//            mIsBound = false;
//        }
//    }
/*
    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }
*/
}
