package com.example.utente.logmyposition;

/**
 * Created by utente on 08/08/2016.
 */
public interface SensorListenerCallback {
    // TODO: specificare i parametri necessari nelle callback, per ora sono vuoti

    // Callback per avere l'aggiornamento della rotazione non rimappata e quindi in coordinate assolute terrestri
    public void registerOrientationUnremappedCallback();

    // Callback per avere l'aggiornamento della rotazione rimappata e quindi in coordinate del cellulare
    public void registerOrientationRemappedCallback();

    // Deregistra le callback
    public void unregisterCallback();
}
