package com.example.utente.logmyposition.AugmentedReality;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;

import com.example.utente.logmyposition.ApplicationSettings;

import java.util.Iterator;
import java.util.Locale;

import com.example.utente.logmyposition.util.GpsInfo;
import com.example.utente.logmyposition.util.GpsSatelliteType;
import com.example.utente.logmyposition.util.KalmanFilter3D;
import com.example.utente.logmyposition.util.LowPassFilter;

/**
 * Created by utente on 31/07/2016.
 */

public class OverlayView extends View implements SensorEventListener, GpsStatus.Listener, LocationListener{

    public static final String DEBUG_TAG = "OverlayView Log";
    String GPSStatus = "";


    // TODO: per ora copio il codice. Poi ottimizzarlo con un servizio
    private LocationManager locationManager;
    private SensorManager   sensorManager;
    Sensor accelSensor;
    Sensor compassSensor;
    Sensor gyroSensor;
    private ApplicationSettings applicationSettings = ApplicationSettings.getInstance();

    private String[] mCardinalPoints ={"N","NE","E","SE","S","SW","W","NW"};

    private GpsInfo[] mGpsInfo;


    // TODO: Rimuovere i listener. Da verificare in quale metodo farlo
    public OverlayView(Context context) {
        super(context);

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // TODO: portarli fuori da qui e metterli nell'oggetto che lo utilizzerà. Se serve creare metodi specifici di registrazione deregistrazione
        boolean isAccelAvailable = sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
        boolean isCompassAvailable = sensorManager.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_NORMAL);
        boolean isGyroAvailable = sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.addGpsStatusListener(this);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                0,
                0.0f,
                this);

        // Inizializzazione
        mGpsInfo=new GpsInfo[255];

        // Inizializzo le info sui satelliti
        for (int i=0;i<255;i++){
            mGpsInfo[i]=new GpsInfo();
        }

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    Paint contentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Rect textBounds = new Rect(); // Mi serve per l'allineamento verticale

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Disegno un satellite. FOV = 46 per un obiettivo di 50mm
        float FOV=applicationSettings.getFOV();
        float verticalFOV = FOV/2;
        float horizontalFOV = FOV/2;

        // Calcolo la risoluzione in gradi: rapporto tra pixel e gradi di vista
        // Per ora considero uguali orizzontale e verticale
        float degRes=canvas.getWidth()/horizontalFOV;

        canvas.drawColor(Color.BLACK);

        contentPaint.setTextAlign(Paint.Align.LEFT);
        contentPaint.setTextSize(20);
        contentPaint.setColor(Color.RED);
        contentPaint.setStrokeWidth(4);

// DEBUG
//        canvas.drawText(String.format(Locale.ITALY,"%+3.0f  %+3.0f  %+3.0f - canvas (%d %d)",
//                orientationDeg[0], orientationDeg[1], orientationDeg[2], canvas.getWidth(), canvas.getHeight()),
//                40, 40, contentPaint);
//        canvas.drawText(String.format("%3.0f",Math.toDegrees(orientation[1])), canvas.getWidth()/2, canvas.getHeight()/2, contentPaint);
//        canvas.drawText(String.format("%3.0f",Math.toDegrees(orientation[2])), canvas.getWidth()/2, (canvas.getHeight()*3)/4, contentPaint);


        // Disegno una linea orizzontale e verticale fissa a metà della view
        canvas.drawLine(0f, (int)(canvas.getHeight()/1.5), canvas.getWidth(), (int)(canvas.getHeight()/1.5), contentPaint);
        canvas.drawLine(canvas.getWidth()/2, 0, canvas.getWidth()/2, canvas.getHeight(), contentPaint);

        // Ciclo su tutti i satelliti
//        GpsStatus gs = locationManager.getGpsStatus(null);
//        final Iterator<GpsSatellite> it = gs.getSatellites().iterator();
//        GpsSatellite gpsSatellite;

        contentPaint.setColor(Color.WHITE);

        int satellites=0;

        float minH;
        float maxH;
        float minV;
        float maxV;
        float dx,dy;

        float azimuth, elevation;
        minH=((orientationDeg[0]-horizontalFOV)+360)%360;
        maxH=((orientationDeg[0]+horizontalFOV)+360)%360;

        minV=((orientationDeg[1]-verticalFOV))%360;
        maxV=((orientationDeg[1]+verticalFOV))%360;

        for (int i=0;i<255;i++){
            //while(it.hasNext()) {
            //gpsSatellite=it.next();

            // Se i dati non sono aggiornati passo al satellite successivo
            if (!mGpsInfo[i].dataUpdated) continue;

            satellites++;


            azimuth=mGpsInfo[i].azimuth;
            elevation=mGpsInfo[i].elevation;

            // DEBUG
//            canvas.drawText(String.format(Locale.ITALY,"degRes %3.3f - minH %3.3f - manH %3.3f - minV %3.3f - maxV %3.3f",
//                    degRes,minH,maxH,minV,maxV),
//                    40, 80, contentPaint);

//            // DEBUG
//            orientationDeg[0]=180.0f;
//            orientation[0]=(float)Math.PI;

            // Calcolo se il satellite è all'interno dell'area di vista
            // Se non sono all'esterno dell'intervallo orizzontale allora proseguo
            if (((minH>maxH)&&((azimuth>minH)||(azimuth<maxH))) ||
                    ((minH<maxH)&&(azimuth>minH)&&(azimuth<maxH))){
                // Se non sono all'esterno dell'intervallo verticale allora disegno il punto
                if ((elevation>minV)&&(elevation<maxV)){

                    dx=azimuth - orientationDeg[0];
                    // Tengo conto di quando si passa d 0 a 360. C'è un salto che porta a non visualizzare correttamente il satellite
                    if ((minH>maxH)){
                        if (azimuth > minH && orientationDeg[0]<minH){
                            dx-=360;
                        }
                        if (azimuth < maxH && orientationDeg[0]>maxH){
                            dx+=360;
                        }
                    }

                    dx = (canvas.getWidth()/2) + (degRes*dx);
                    dy = (int)(canvas.getHeight()/1.5)+ (degRes*(orientationDeg[1]-elevation));

//                    // DEBUG
//                    if (gpsSatellite.getAzimuth()==0) {
//                        canvas.drawText(String.format(Locale.ITALY, "dx %3.3f - dy %3.3f",
//                                dx, dy),
//                                40, 60, contentPaint);
//                    }

                    // Disegno il satellite: verde se è usato per il fix, altrimenti bianco
                    if (mGpsInfo[i].usedInFix){
                        contentPaint.setColor(Color.GREEN);
                    } else{
                        contentPaint.setColor(Color.WHITE);
                    }
                    canvas.drawCircle(dx, dy, 8.0f, contentPaint);
                    canvas.drawText(String.format(Locale.ITALY,"%d(%+3.0f  %+3.0f)",
                            mGpsInfo[i].PRN ,azimuth, elevation),
                            10+dx, dy,
                            contentPaint);
                }
            }
        }


        // Disegno il ruler orizzontale
        contentPaint.setColor(Color.GREEN);
        contentPaint.setTextAlign(Paint.Align.CENTER);
//        setTextSizeForWidth(contentPaint,30,"NW");// NW è solo per avere un riferimeneto a due caratteri
        for (int i=0;i<24;i++){
//            minH=((orientationDeg[0]-horizontalFOV)+360)%360;
//            maxH=((orientationDeg[0]+horizontalFOV)+360)%360;

            azimuth=15*i;

// TODO: Codice ripetuto, metterlo in una funzione
            if (((minH>maxH)&&((azimuth>minH)||(azimuth<maxH))) ||
                    ((minH<maxH)&&(azimuth>minH)&&(azimuth<maxH))){
                dx=azimuth - orientationDeg[0];
                if ((minH>maxH)){
                    if (azimuth > minH && orientationDeg[0]<minH){
                        dx-=360;
                    }
                    if (azimuth < maxH && orientationDeg[0]>maxH){
                        dx+=360;
                    }
                }

                dx = (canvas.getWidth()/2) + (degRes*dx);
                if (((int)azimuth)%45 == 0){
                    canvas.drawLine(dx,(int)(canvas.getHeight()/1.5)+20,dx,(int)(canvas.getHeight()/1.5),contentPaint);
                    canvas.drawText(String.format(Locale.ITALY,"%s",mCardinalPoints[i/3]),   // 24 / 3 = 8 punti cardinali  -- 24 * 15 = 360 gradi
                            dx,(int)(canvas.getHeight()/1.5)+40, contentPaint);
                } else{
                    canvas.drawLine(dx,(int)(canvas.getHeight()/1.5)+10,dx,(int)(canvas.getHeight()/1.5),contentPaint);
                    canvas.drawText(String.format(Locale.ITALY,"%3.0f°",azimuth),
                            dx,(int)(canvas.getHeight()/1.5)+30, contentPaint);

                }
            }
        }


        // Disegno il ruler VERTICALE
// TODO: Codice ripetuto, metterlo in una funzione
        contentPaint.setColor(Color.GREEN);
        contentPaint.setTextAlign(Paint.Align.CENTER);
        // TODO: migliorare, per ora mi serve la stringa più ampia in verticale per le dimensioni dell'area rettangolare che può contenerlo
        contentPaint.getTextBounds("-30", 0, "-30".length(), textBounds);

//        setTextSizeForWidth(contentPaint,30,"NW");// NW è solo per avere un riferimeneto a due caratteri
        for (int i=-6;i<7;i++){
//            minH=((orientationDeg[0]-horizontalFOV)+360)%360;
//            maxH=((orientationDeg[0]+horizontalFOV)+360)%360;

            elevation=15*i;
            dy = (int)(canvas.getHeight()/1.5)+ (degRes*(orientationDeg[1]-elevation));

            canvas.drawLine(canvas.getWidth()/2,dy,canvas.getWidth()/2-20,dy,contentPaint);
            canvas.drawText(String.format(Locale.ITALY,"%3.0f",elevation),   // 12 * 15 = 180 gradi
                    canvas.getWidth()/2-50,dy-textBounds.exactCenterY(), contentPaint);
        }

        contentPaint.setColor(Color.WHITE);
        contentPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(String.format(Locale.ITALY,"Satelliti: %3d",satellites),
                0, 20, contentPaint);
        canvas.drawText(String.format(Locale.ITALY,"A:%3.0f°  E:%3.0f°  R:%3.0f°",orientationDeg[0],orientationDeg[1],orientationDeg[2]),
                0, 40, contentPaint);

        contentPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(String.format(Locale.ITALY,"GPS: %s",GPSStatus),
                canvas.getWidth()-10, 20, contentPaint);

        // Disegno l'orizzonte
        // use roll for screen rotation
        canvas.translate(canvas.getWidth()/2, (int)(canvas.getHeight()/1.5));
        canvas.rotate(-orientationDeg[2]);
        // Disegno la linea d'orizzonte
        canvas.drawLine(-canvas.getWidth(), 0, canvas.getWidth(), 0, contentPaint);

    }


    /**
     * Sets the text size for a Paint object so a given string of text will be a
     * given width.
     *
     * @param paint
     *            the Paint to set the text size for
     * @param desiredWidth
     *            the desired width
     * @param text
     *            the text that should be that width
     */
    static Rect bounds= new Rect();
    private static void setTextSizeForWidth(Paint paint, float desiredWidth,
                                            String text) {

        // Pick a reasonably large value for the test. Larger values produce
        // more accurate results, but may cause problems with hardware
        // acceleration. But there are workarounds for that, too; refer to
        // http://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
        final float testTextSize = 48f;

        // Get the bounds of the text, using our testTextSize.
        paint.setTextSize(testTextSize);

        paint.getTextBounds(text, 0, text.length(), bounds);

        // Calculate the desired size as a proportion of our testTextSize.
        float desiredTextSize = testTextSize * desiredWidth / bounds.width();

        // Set the paint for that size.
        paint.setTextSize(desiredTextSize);
    }

    // compute rotation matrix
    float rotation[] = new float[9];
    float identity[] = new float[9];
    float[] lastAccelerometer=new float[3];
    float[] lastCompass=new float[3];
    float orientation[] = new float[3];
    float orientationDeg[] = new float[3];
    float cameraRotation[] = new float[9];

    //LowPassFilter lowPassFilterAccel=new LowPassFilter(), lowPassFilterMagnet=new LowPassFilter();
    LowPassFilter lowPassFilterOrientation=new LowPassFilter();

    KalmanFilter3D kalmanFilter3DAccel = new KalmanFilter3D(0.015f, 16, 1, 0.0001f);
    KalmanFilter3D kalmanFilter3DCompass = new KalmanFilter3D(0.015f, 16, 1, 0.0001f);

    @Override
    public void onSensorChanged(SensorEvent event) {

        long nowMillis=System.currentTimeMillis();

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //lastAccelerometer=lowPassFilterAccel.lowPass(event.values.clone());
            if (applicationSettings.isFilterKalman()) {
                // TODO: impostare nelle references l'uso del filtro di kalman
                lastAccelerometer= kalmanFilter3DAccel.update(event.values);
            } else {
                System.arraycopy(event.values,0,lastAccelerometer,0,event.values.length);
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            //lastCompass=lowPassFilterMagnet.lowPass(event.values.clone());
            if (applicationSettings.isFilterKalman()){
                // TODO: impostare nelle references l'uso del filtro di kalman
                lastCompass= kalmanFilter3DCompass.update(event.values);
            }else{
                System.arraycopy(event.values,0,lastCompass,0,event.values.length);
            }
        }

        boolean gotRotation = SensorManager.getRotationMatrix(rotation,identity, lastAccelerometer, lastCompass);
        if (gotRotation) {
            // remap such that the camera is pointing straight down the Y axis
            SensorManager.remapCoordinateSystem(rotation, SensorManager.AXIS_X,
                    SensorManager.AXIS_Z, cameraRotation);

            // orientation vector
            SensorManager.getOrientation(cameraRotation, orientation);
            orientation=lowPassFilterOrientation.lowPass(orientation.clone());
            //orientation=kalmanFilter3D.update(orientation.clone());

            // Riporto i valori tra 0 e 2pi
            orientation[0]= (float)(orientation[0]+ 2*Math.PI);  // Riporto tra 0 e 2pi
            orientation[1]= -orientation[1];                     // Riporto tra 0 - orizzontale, pi/2 - verticale superiore -pi/2 - vertivale inferiore
            orientation[2]= (float)(orientation[2]+ 2*Math.PI);  // Riporto tra 0 3 360

//            // Riporto tra 0 e 2pi
//            if (orientation[0]>2*Math.PI) orientation[0]-=2*Math.PI;
//            if (orientation[1]>2*Math.PI) orientation[1]-=2*Math.PI;
//            if (orientation[2]>2*Math.PI) orientation[2]-=2*Math.PI;

            // Filtro i valori finali

            // Riporto in gradi e tra 0 e 360°
            orientationDeg[0]= (float)(Math.toDegrees(orientation[0]))%360;  // Riporto tra 0 e 360
            orientationDeg[1]= (float)(Math.toDegrees(orientation[1]))%360;  // Riporto tra 0 - orizzontale, 90 - verticale superiore -90 - vertivale inferiore
            orientationDeg[2]= (float)(Math.toDegrees(orientation[2]))%360;  // Riporto tra 0 3 360
        }

        this.invalidate();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // TODO: Capire perchè non parte il GPS. lo stato è stopped a meno di riavviare il gps. Non accade negli altri fragment
    // Potrebbe dipendere dal fatto che occorre implementare il location listener
    @Override
    public void onGpsStatusChanged(int event) {
        switch (event) {
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                GPSStatus = "GPS_EVENT_FIRST_FIX";
                break;
            case GpsStatus.GPS_EVENT_STARTED:
                GPSStatus= "GPS_EVENT_STARTED";
                applicationSettings.setGPSAvailable(true);
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                GPSStatus= "GPS_EVENT_STOPPED";
                applicationSettings.setGPSAvailable(false);
//                turnOffAllUI();
                break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                GPSStatus= "GPS_EVENT_SATELLITE_STATUS";
                break;
        }

        GpsStatus gs = locationManager.getGpsStatus(null);

        final Iterator<GpsSatellite> it = gs.getSatellites().iterator();
        GpsSatellite gpsSatellite;

        int maxSat = 0, currSat = 0, fixSat = 0;
        int[] SatType = {0, 0, 0, 0, 0};
        int[] SatTypeFix = {0, 0, 0, 0, 0};
        int SatTypeIndex = 0;
        int satellitePRN;

        // Resetto lo stato GPS per mostrare quello attuale;
        for (int i = 0; i < 255; i++) {
            mGpsInfo[i].resetAll();
        }

        while (it.hasNext()) {
            gpsSatellite = it.next();

            switch (GpsSatelliteType.getSatelliteType(gpsSatellite.getPrn())) {
                case BEIDOU:
                    SatTypeIndex = 0;
                    break;
                case GLONASS:
                    SatTypeIndex = 1;
                    break;
                case GPS:
                    SatTypeIndex = 2;
                    break;
                case QZSS:
                    SatTypeIndex = 3;
                    break;
                case SBAS:
                    SatTypeIndex = 4;
                    break;
                default:
                    SatTypeIndex = -1;
            }

            if (gpsSatellite.usedInFix()) {
                fixSat++;
            }
            maxSat++;

            satellitePRN = gpsSatellite.getPrn();

            if (gpsSatellite.getSnr() > 0) {
                currSat++;
            }

            if (SatTypeIndex >= 0) {
                SatType[SatTypeIndex]++;
                if (gpsSatellite.usedInFix()) {
                    SatTypeFix[SatTypeIndex]++;
                }
            }
            mGpsInfo[satellitePRN].updateValues(gpsSatellite);
        }
        this.invalidate();
    }
}