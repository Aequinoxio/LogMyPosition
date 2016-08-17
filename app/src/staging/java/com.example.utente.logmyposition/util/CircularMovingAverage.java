package com.example.utente.logmyposition.util;

/**
 * Created by utente on 11/08/2016.
 */
/**
 * Classe per la gestione della media mobile. Considera un vetore di 3 coordinate di cui calcola la media mobile indipendentemente l'uno dall'altro
 */
public class CircularMovingAverage {
    private final int accumIndexMax=30;     // Calcolo media ultimi rilevamenti
    private int accumIndex;                 // media mobile, mi serve un indice per indirizzare l'accumulatore
    private float[][] mValuesMeasured;      // Valori misurati
    private float[] mValuesMean;            // Valori medi

    private float[][] sinusValuesMeasured;           // Valori cartesiani corrispondenti all'angolo
    private float[][] cosinusValuesMeasured;
    private float[] mSinusValuesMean;            // Valori medi
    private float[] mCosinusValuesMean;            // Valori medi

    public CircularMovingAverage(){
        mValuesMeasured=new float[accumIndexMax][3];
        mValuesMean=new float[3];

        sinusValuesMeasured =new float[accumIndexMax][3];
        mSinusValuesMean=new float[3];

        cosinusValuesMeasured=new float[accumIndexMax][3];
        mCosinusValuesMean=new float[3];

        accumIndex=0;
    }

    /**
     * Aggiunge i valori al calcolo della media mobile
     * Restituisce la media mobile includendo i valori precedenti e quelli attualmente passati per parametro
     * @param values - vettore di 3 float (x,y,z) di cui calcolare la media mobile. Per ciascuno viene calcolata
     *               la media mobile basata sulla serie storica
     * @return valori medi (xm,ym,zm) calcolati
     */
    public float[] addValuesToMovingAverage(float[] values){

        for (int i=0;i<3;i++) {
            mSinusValuesMean[i] = (float) (mSinusValuesMean[i] + (-sinusValuesMeasured[accumIndex][i] + Math.sin(values[i])) / ((float) accumIndexMax));
            sinusValuesMeasured[accumIndex][i]=(float)Math.sin(values[i]);

//        mSinusValuesMean[0]=(float)(mSinusValuesMean[0]+(-sinusValuesMeasured[accumIndex][0]+Math.sin(values[0]))/((float)accumIndexMax));
//        mSinusValuesMean[0]=(float)(mSinusValuesMean[0]+(-sinusValuesMeasured[accumIndex][0]+Math.sin(values[0]))/((float)accumIndexMax));
//        sinusValuesMeasured[accumIndex][0]=(float)Math.sin(values[0]);
//        sinusValuesMeasured[accumIndex][1]=(float)Math.sin(values[1]);
//        sinusValuesMeasured[accumIndex][2]=(float)Math.sin(values[2]);


            mCosinusValuesMean[i] = (float) (mCosinusValuesMean[i] + (-cosinusValuesMeasured[accumIndex][i] + Math.cos(values[i])) / ((float) accumIndexMax));
            cosinusValuesMeasured[accumIndex][i]=(float)Math.cos(values[i]);

            // Calcolo la media
            mValuesMean[i]=(float)Math.atan2(mSinusValuesMean[i],mCosinusValuesMean[i]);

            // Copio il nuovo valore
            mValuesMeasured[accumIndex][i]=values[i];

        }

//        mCosinusValuesMean[0]=mCosinusValuesMean[0]+(-cosinusValuesMeasured[accumIndex][0]+Math.cos(values[0])/((float)accumIndexMax);
//        mCosinusValuesMean[1]=mCosinusValuesMean[1]+(-cosinusValuesMeasured[accumIndex][1]+Math.cos(values[1])/((float)accumIndexMax);
//        mCosinusValuesMean[2]=mCosinusValuesMean[2]+(-cosinusValuesMeasured[accumIndex][2]+Math.cos(values[2])/((float)accumIndexMax);
//        cosinusValuesMeasured[accumIndex][0]=(float)Math.cos(values[0]);
//        cosinusValuesMeasured[accumIndex][1]=(float)Math.cos(values[1]);
//        cosinusValuesMeasured[accumIndex][2]=(float)Math.cos(values[2]);

//        mValuesMean[0]=(float)Math.atan2(mSinusValuesMean[0],mCosinusValuesMean[0]);
//        mValuesMean[1]=(float)Math.atan2(mSinusValuesMean[1],mCosinusValuesMean[1]);
//        mValuesMean[2]=(float)Math.atan2(mSinusValuesMean[2],mCosinusValuesMean[2]);

//        mValuesMean[0]=mValuesMean[0]+(-mValuesMeasured[accumIndex][0]+values[0])/((float)accumIndexMax);
//        mValuesMean[1]=mValuesMean[1]+(-mValuesMeasured[accumIndex][1]+values[1])/((float)accumIndexMax);
//        mValuesMean[2]=mValuesMean[2]+(-mValuesMeasured[accumIndex][2]+values[2])/((float)accumIndexMax);

//        // Copio il nuovo valore
//        mValuesMeasured[accumIndex][0]=values[0];
//        mValuesMeasured[accumIndex][1]=values[1];
//        mValuesMeasured[accumIndex][2]=values[2];

        // Aggiorno l'indice dell'accumulatore
        accumIndex++;
        if(accumIndex>=accumIndexMax){
            accumIndex=0;
        }

        return mValuesMean.clone();
    }

    /**
     * Calcolo della media "liscia". Aggiunge i valori e ritorna la media.
     * @param values
     * @param alpha - Valore per rendere la media più liscia (<1.0). Più è piccolo e maggiore è l'effetto. Un valore tipico è 0.15
     * @return
     */
    public float[] addValuesToSmootedMedia(float[] values, float alpha){
        for (int i=0;i<3;i++){
            mSinusValuesMean[i]     =(float) (mSinusValuesMean[i] + alpha*(-mSinusValuesMean[i] + Math.sin(values[i])));
            mCosinusValuesMean[i]   =(float) (mCosinusValuesMean[i] + alpha*(-mCosinusValuesMean[i] + Math.cos(values[i])));

            mValuesMean[i]          =(float)Math.atan2(mSinusValuesMean[i],mCosinusValuesMean[i]);
        }

        return mValuesMean.clone();
    }
}
