package com.example.utente.logmyposition;

import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;

import org.acra.ACRA;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class ShowLogFileContent extends AppCompatActivity {
    TableLayout tabella=null;
    TableRow riga=null;
    TextView valore=null;

    ApplicationSettings applicationSettings = ApplicationSettings.getInstance();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Salvo alcune variabili per debug
        ACRA.getErrorReporter().putCustomData("Event at " + System.currentTimeMillis()+ " -> "+ Thread.currentThread().getStackTrace()[2].getClassName().replace(".","_"),
                Thread.currentThread().getStackTrace()[2].getMethodName());

        setContentView(R.layout.activity_show_log_file_content);
        mostraLogFile();
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_show_log_file_content, menu);
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
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    /**
     * Carica il file e lo mostra nella textView
     *
     */
    private void mostraLogFile(){
        File dataFile = applicationSettings.getFileSalvataggio();
        byte[] buffer = new byte[1024];
        StringBuilder sb = new StringBuilder();
        String linea=null;

        tabella = (TableLayout)findViewById(R.id.TABLELAYOUT_ID);

        //TextView textView=(TextView) findViewById(R.id.TEXT_STATUS_ID);;

        FileInputStream fileInputStream= null;
        BufferedReader bufferedReader;

        // TODO: Costanti cablate
        linea="UUID_Sessione;Contatore;Data locale;Tempo_GPS;Latitudine;Longitudine;Altitudine;Velocità;Orientamento;Accuratezza;Polling spazio (m);Polling tempo (s)";
        aggiungiATabella(linea,1);

        try {
            bufferedReader = new BufferedReader(new FileReader(dataFile)) ;
            int i=0;
            while ((linea=bufferedReader.readLine())!=null){
                // Utili per una semplice textView
                //linea+="\n";
                //linea=linea.replace(";","\t | \t");
                sb.append(linea);
                aggiungiATabella(linea, i++);
            }
        } catch (FileNotFoundException fnfe) {
            // Non dovrebbe mai essere lanciata a meno di cose strane durante l'esecuzione del servizio es. cancellazione file
            // tramite gestione file e da root
            fnfe.printStackTrace();
        } catch (IOException ioe){
            ioe.printStackTrace();
        }

        // textView.setText(sb.toString());

        tabella.requestLayout();

    }

    private void aggiungiATabella(String valori, int numeroRiga){
        // TODO: Costante separatore cablata
        StringTokenizer st=new StringTokenizer(valori,";");
        String s ;
        riga=new TableRow(this);

        while (st.hasMoreTokens()){
            s=st.nextToken();
//            riga.setLayoutParams(new TableRow.LayoutParams(
//                    TableRow.LayoutParams.MATCH_PARENT,
//                    TableRow.LayoutParams.WRAP_CONTENT));
            // Imposto le righe a colori alterni
            // TODO: Rendere parametrico da far scegliere all'utente
            if ((numeroRiga & 0x01)==0)
                riga.setBackgroundColor(Color.WHITE);
            else
                riga.setBackgroundColor(Color.LTGRAY);

            valore = new TextView(this);
            valore.setTextColor(Color.BLUE);
            valore.setBackgroundResource(R.drawable.cell_shape);
            valore.setGravity(Gravity.CENTER_HORIZONTAL);
            valore.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            valore.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            valore.setPadding(5, 5, 5, 5);
            valore.setText(s);
            riga.addView(valore);


        }
        tabella.addView(riga,new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
    }
}

