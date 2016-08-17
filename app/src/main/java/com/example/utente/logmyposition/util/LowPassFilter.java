package com.example.utente.logmyposition.util;

import com.example.utente.logmyposition.ApplicationSettings;

/**
 * Created by utente on 01/08/2016.
 */
public class LowPassFilter {
//    private final int accumIndexMax=30;     // Calcolo media ultimi rilevamenti
//    private int accumIndex;                 // media mobile, mi serve un indice per indirizzare l'accumulatore
//    private float[][] mValuesMeasured;      // Valori misurati
    private float[] mValuesMean;            // Valori medi
    private CircularMovingAverage circularMovingAverage, circularMovingAverage2, circularMovingAverage3;

    private long latestSampleTime, currentSampleTime;

    static final float ALPHA = 0.15f;

    ApplicationSettings applicationSettings = ApplicationSettings.getInstance();

    public LowPassFilter(){
//        mValuesMeasured=new float[accumIndexMax][3];
        mValuesMean=new float[3];
//        accumIndex=0;
        circularMovingAverage =new CircularMovingAverage();
        circularMovingAverage2 =new CircularMovingAverage(); // Media della media
        circularMovingAverage3 =new CircularMovingAverage(); // Media della media
        currentSampleTime=System.currentTimeMillis();
        latestSampleTime=currentSampleTime--; // Inizializzo con un millisecondo di differenza
    }

    public float[] lowPass( float[] input) {
        //setSampleInterval();
        currentSampleTime=System.currentTimeMillis();

        switch (applicationSettings.getLowPassFilterType()){
            // Nessun filtro
//            case 0:
//
//                break;
            case 1:

                mValuesMean= circularMovingAverage.addValuesToMovingAverage(input);
                break;
            case 2:
                mValuesMean= circularMovingAverage.addValuesToSmootedMedia(input,ALPHA);
                //calculateMean(input, ALPHA);
                break;
            case 3:

                calculateMeanFrameRate(input);
                break;

            // Copio l'input in caso non sappia quale filtro applicare per avere l'uscita del filtro sempre definita
            default:
                System.arraycopy(input,0,mValuesMean,0,input.length);
        }

        latestSampleTime=currentSampleTime;
        return mValuesMean;
    }

    private void setSampleInterval(){
        latestSampleTime=currentSampleTime;             // Salvo il valore precedente
        currentSampleTime=System.currentTimeMillis();   // ed aggiorno ai millisecondi attuali
    }

    // Metodo 1
    private void calculateMean(float[] values){
        mValuesMean= circularMovingAverage.addValuesToMovingAverage(values);
    }

//    // Metodo 2
//    private void calculateMean(float[] values, float alpha){
//        //if (mValuesMean==null) mValuesMean=new float[3];
//
//        mValuesMean[0]=mValuesMean[0]+alpha*(-mValuesMean[0]+ values[0]);
//        mValuesMean[1]=mValuesMean[1]+alpha*(-mValuesMean[1]+ values[1]);
//        mValuesMean[2]=mValuesMean[2]+alpha*(-mValuesMean[2]+ values[2]);
//
//    }

//    // Metodo 3
//    private void lowPassFilterNoFramerate(float[] input){
//        for ( int i=0; i<input.length; i++ ) {
//            mValuesMean[i] = mValuesMean[i] + ALPHA * (input[i] - mValuesMean[i]);
//        }
//    }


//    var smoothed   = 0;        // or some likely initial value
//    var smoothing  = 10;       // or whatever is desired
//    var lastUpdate = new Date;
//    function smoothedValue( newValue ){
//        var now = new Date;
//        var elapsedTime = now - lastUpdate;
//        smoothed += elapsedTime * ( newValue - smoothed ) / smoothing;
//        lastUpdate = now;
//        return smoothed;
//    }
//            lastAccelerometer[0]+=(nowMillis-lastSensorUpdateMillisAccel)*(event.values[0]-lastAccelerometer[0])/((float)smoothingValue);
//            lastAccelerometer[1]+=(nowMillis-lastSensorUpdateMillisAccel)*(event.values[1]-lastAccelerometer[1])/((float)smoothingValue);
//            lastAccelerometer[2]+=(nowMillis-lastSensorUpdateMillisAccel)*(event.values[2]-lastAccelerometer[2])/((float)smoothingValue);

    // Metodo 4
    // TODO: BUGFIX normalizzare l'elapsed
    int smootingValue=10;
    private void calculateMeanFrameRate(float[]values){
        if (mValuesMean==null) mValuesMean=new float[3];
        long elapsed = currentSampleTime-latestSampleTime;
        mValuesMean[0]+=elapsed*(values[0]-mValuesMean[0])/(float)smootingValue;
        mValuesMean[1]+=elapsed*(values[1]-mValuesMean[1])/(float)smootingValue;
        mValuesMean[2]+=elapsed*(values[2]-mValuesMean[2])/(float)smootingValue;
    }
}
