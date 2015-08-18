package com.example.utente.logmyposition;

import android.content.Context;
import android.view.View;

/**
 * Created by utente on 15/08/2015.
 */
public class ApplicationUtility {

    /**
     *
     * @param context
     * @param stato
     * @return
     *
     * Ritorno la stringa corritpondente allo stato per impostare la label
     */
    public static String getServiceStatusTextLabel(Context context,boolean stato){
        return stato?context.getString(R.string.txtStatoLoggingRunning):context.getString(R.string.txtStatoLoggingStopped);
    }

    /**
     *
     * @param context
     * @param stato
     * @return
     *
     * Ritorno la stringa opposta allo stato per impostare il toggle button
     */
    public static String getServiceStatusButtonLabel(Context context,boolean stato){
        return stato?context.getString(R.string.stopServiceButtonText):context.getString(R.string.startServiceButtonText);
    }

    /**
     *
     * @param context
     * @param stato
     * @return
     *
     * Ritorno lo stato di visibilità
     */
    public static int getServiceStatusProgressVisibility(Context context,boolean stato){
        return stato ? View.VISIBLE:View.INVISIBLE;
    }
}
