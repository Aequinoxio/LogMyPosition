package com.example.utente.logmyposition.util;

/**
 * Created by utente on 16/08/2016.
 */
public class KalmanFilter3D {
    KalmanFilterScalar[] kalmanFilters=new KalmanFilterScalar[3];
    float[] values=new float[3];
    /**
     * Inizializza il filtro con i valori unici per le tre dimensioni
     * @param q - process noise covariance
     * @param r - measurement noise covariance
     * @param p - estimation error covariance
     * @param initial_value - initial value

     * @param q
     * @param r
     * @param p
     * @param initial_value
     */
    public KalmanFilter3D(double q, double r, double p, double initial_value){
        for (int i=0;i<3;i++){
            kalmanFilters[i]= new KalmanFilterScalar();
            kalmanFilters[i].init(q,r,p,initial_value);
            values[i]=(float)initial_value;
        }
    }

    public float[] update(float[] measurements){
        for (int i=0;i<3;i++){
            values[i]=(float)kalmanFilters[i].update((double)measurements[i]);
        }
        return values;
    }
}
