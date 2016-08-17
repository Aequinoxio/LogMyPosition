package com.example.utente.logmyposition;

import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TableLayout;
import android.widget.TextView;

//import com.example.utente.logmyposition.util.DimensionUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ShowLogFileContentlistView extends ActionBarActivity {

    TextView valore=null;

    ApplicationSettings applicationSettings = ApplicationSettings.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_log_file_contentlist_view);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ListView lv= (ListView)findViewById(R.id.listview);

        // create the grid item mapping
        String[] from = new String[] {"rowid", "col_0", "col_1", "col_2", "col_3","col_4","col_5","col_6","col_7","col_8","col_9","col_10","col_11"};

//        // TODO: Cablarlo nello header e non qui
//        String[] from_header = new String[] {"UUID_Sessione", "Contatore", "Data locale", "Tempo_GPS", "Latitudine", "Longitudine", "Altitudine", "Velocità", "Orientamento", "Accuratezza", "Polling spazio (m)", "Polling tempo (s)"};

        int[] to = new int[] { R.id.item1, R.id.item2, R.id.item3, R.id.item4,R.id.item5 ,R.id.item6 ,R.id.item7 ,R.id.item8 ,R.id.item9 ,R.id.item10 ,R.id.item11,R.id.item12,R.id.item13 };

        int[] larghezze = new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0};

        /* Calcolo larghezza ottimale della griglia in base alla dimensione delle stringhe
        TODO: verificare se i metodi ritornano DP Px o altro
         */
//        Rect rectTemp = new Rect();
//        TextView textViewTemp;
//        Paint textPaintTemp ;
//        View viewTemp= (View) findViewById(R.id.horizontalScrollView2);
//        int widthTemp=0;
//
 //TODO: impostare le dimensioni per tutte le righe
//        // Intanto imposto il max alla dimensione delle label
//        for (int i=0;i<larghezze.length;i++){
//            textViewTemp = (TextView) findViewById(to[i]);
//            String sTemp = textViewTemp.getText().toString();
//            textPaintTemp = textViewTemp.getPaint();
//            //textPaintTemp.getTextBounds(sTemp,0,sTemp.length(),rectTemp);
//            //widthTemp= (int)DimensionUtils.pxToDp(viewTemp, rectTemp.width());
//            //widthTemp=rectTemp.width();
//
//            // Aggiungo un carattere "largo" alla stringa
//            float fwidthTemp=textPaintTemp.measureText(sTemp+"0");
//            // Salvo la larghezza e la imposto nell'intestazione
//            larghezze[i]=(int)fwidthTemp;
//            textViewTemp.setWidth((int)fwidthTemp);
//        }
// TODO: nel ciclo seguente verificare se la larghezza è > se si aggiornare larghezze ed al termine reimpostare la larghezza dell'intetazione
        // prepare the list of all records
        List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();

        /*
        Carica il file
         */
        File dataFile = applicationSettings.getFileSalvataggio();
        byte[] buffer = new byte[1024];
        StringBuilder sb = new StringBuilder();
        String linea=null;

        FileInputStream fileInputStream= null;
        BufferedReader bufferedReader;

//        textViewTemp=new TextView(getApplicationContext());

        int riga=0;
        try {
            bufferedReader = new BufferedReader(new FileReader(dataFile)) ;
            int i=0;
            while ((linea=bufferedReader.readLine())!=null){
                // Divido la stringa in base ai ";"
                String[] elementi = linea.split(";");
                // Utili per una semplice textView
                //linea+="\n";
                //linea=linea.replace(";","\t | \t");
                HashMap<String, String> map = new HashMap<String, String>();
                riga++;
                map.put("rowid", "" + riga);

                int maxElementi=Math.min(elementi.length, larghezze.length);

                for (int x=0; x<maxElementi; x++) {
                    map.put("col_"+x, elementi[x]);

//                    // Calcolo la più grande larghezza
//                    textViewTemp.setText(elementi[x]);
//                    textPaintTemp = textViewTemp.getPaint();
//
//                    // Aggiungo un carattere "largo" alla stringa
//                    float fwidthTemp=textPaintTemp.measureText(elementi[x]+"0");
//                    // Salvo la larghezza e la imposto nell'intestazione
//                    larghezze[x]=Math.max((int)fwidthTemp, larghezze[x]);
                }
                // Aggiungo la mappa
                fillMaps.add(map);
            }
        } catch (FileNotFoundException fnfe) {
            // Non dovrebbe mai essere lanciata a meno di cose strane durante l'esecuzione del servizio es. cancellazione file
            // tramite gestione file e da root
            fnfe.printStackTrace();
        } catch (IOException ioe){
            ioe.printStackTrace();
        }

//TODO:  propagare queste dimensioni a tutte le righe
//        for (int x=0;x<larghezze.length-1;x++){
//            textViewTemp=(TextView) findViewById(to[x+1]);
//            textViewTemp.setWidth(larghezze[x]);
//        }

        // fill in the grid_item layout
        SimpleAdapter adapter = new SimpleAdapter(this, fillMaps, R.layout.grid_item, from, to);
        lv.setAdapter(adapter);
    }
}