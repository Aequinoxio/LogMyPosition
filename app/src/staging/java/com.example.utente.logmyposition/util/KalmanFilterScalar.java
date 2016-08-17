package com.example.utente.logmyposition.util;

/**
 * Created by utente on 16/08/2016.
 */
public class KalmanFilterScalar {
    // Stato del filtro di kalman
    private double q; //process noise covariance
    private double r; //measurement noise covariance
    private double x; //value
    private double p; //estimation error covariance
    private double k; //kalman gain

    /**
     * Inizializza il filtro
     * @param q - process noise covariance
     * @param r - measurement noise covariance
     * @param p - estimation error covariance
     * @param initial_value - initial value
     */
    public void init(double q, double r, double p, double initial_value){

        this.q = q;
        this.r = r;
        this.p = p;
        this.x = initial_value;
    }


    /**
     * Aggiorna il filtro con la nuova misura
     * @param measurement - Nuova misura
     * @return            - Valore filtrato
     */
    public double update(double measurement){
        //prediction update
        //omit x = x
        p = p + q;

        //measurement update
        k = p / (p + r);
        x = x + k * (measurement - x);
        p = (1 - k) * p;

        return x;
    }
}
