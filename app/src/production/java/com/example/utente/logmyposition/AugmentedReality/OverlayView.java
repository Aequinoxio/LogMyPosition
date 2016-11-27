package com.example.utente.logmyposition.AugmentedReality;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.view.View;

import com.example.utente.logmyposition.ApplicationSettings;
import com.example.utente.logmyposition.util.GpsInfo;
import com.example.utente.logmyposition.util.GpsSatelliteType;
import com.example.utente.logmyposition.util.LowPassFilter;

import java.util.Iterator;
import java.util.Locale;

/**
 * Created by utente on 31/07/2016.
 */

public class OverlayView extends View implements SensorEventListener, GpsStatus.Listener{

    public static final String DEBUG_TAG = "OverlayView Log";
    String accelData = "Accelerometer Data";
    String compassData = "Compass Data";
    String gyroData = "Gyro Data";
    SensorManager sensorManager;
    long lastSensorUpdateMillisAccel=0;
    long lastSensorUpdateMillisMagnet=0;
    final int smoothingValue=60;

    // TODO: per ora copio il codice. ottimizzarlo con un servizio
    private LocationManager locationManager;
    private ApplicationSettings applicationSettings = ApplicationSettings.getInstance();

    private GpsInfo[] mGpsInfo;


    // TODO: Rimuovere i listener. Da verificare in quale metodo farlo
    public OverlayView(Context context) {
        super(context);

        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor accelSensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor compassSensor = sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor gyroSensor = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

//        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI);
//        sensorManager.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_UI);

        // TODO: portarli fuori da qui e metterli nell'oggetto che lo utilizzerà. Se serve creare metodi specifici di registrazione de registrazione
        boolean isAccelAvailable = sensors.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
        boolean isCompassAvailable = sensors.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_NORMAL);
        boolean isGyroAvailable = sensors.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.addGpsStatusListener(this);

        // Inizializzazione
        mGpsInfo=new GpsInfo[255];

        // Inizializzo le info sui satelliti
        for (int i=0;i<255;i++){
            mGpsInfo[i]=new GpsInfo();
        }

    }



    // TODO: DEBUG solo per verificare se sto facendo tutto giusto  // Satellite GPS n. 14
//    float satellitesBearing=81;
//    float satellitesElevation=47;
    Paint contentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.LTGRAY);

        contentPaint.setTextAlign(Paint.Align.LEFT);
        contentPaint.setTextSize(20);
        contentPaint.setColor(Color.RED);
//        canvas.drawText(accelData, canvas.getWidth()/2, canvas.getHeight()/4, contentPaint);
//        canvas.drawText(compassData, canvas.getWidth()/2, canvas.getHeight()/2, contentPaint);
//        canvas.drawText(gyroData, canvas.getWidth()/2, (canvas.getHeight()*3)/4, contentPaint);

        canvas.drawText(String.format(Locale.ITALY,"%+3.0f  %+3.0f  %+3.0f",orientationDeg[0], orientationDeg[1], orientationDeg[2]),
                40, 40, contentPaint);
//        canvas.drawText(String.format("%3.0f",Math.toDegrees(orientation[1])), canvas.getWidth()/2, canvas.getHeight()/2, contentPaint);
//        canvas.drawText(String.format("%3.0f",Math.toDegrees(orientation[2])), canvas.getWidth()/2, (canvas.getHeight()*3)/4, contentPaint);

        //// Disegno un satellite. FOV = 46 per un obiettivo di 50mm
        float verticalFOV = 46;
        float horizontalFOV = 46;

        // Disegno una linea orizzontale e verticale fissa a metà della view
        canvas.drawLine(0f, canvas.getHeight()/2, canvas.getWidth(), canvas.getHeight()/2, contentPaint);
        canvas.drawLine(canvas.getWidth()/2, 0, canvas.getWidth()/2, canvas.getHeight(), contentPaint);

        // Ciclo su tutti i satelliti
        GpsStatus gs = locationManager.getGpsStatus(null);
        final Iterator<GpsSatellite> it = gs.getSatellites().iterator();
        GpsSatellite gpsSatellite;

        contentPaint.setColor(Color.BLACK);

        int satellites=0;

        while(it.hasNext()) {
            gpsSatellite=it.next();
            satellites++;

            // Translate, but normalize for the FOV of the camera -- basically, pixels per degree, times degrees == pixels
            float dx = (float) ((canvas.getWidth() / horizontalFOV) * (orientationDeg[0] - gpsSatellite.getAzimuth()));
            float dy = -(float) ((canvas.getHeight() / verticalFOV) * (orientationDeg[1] - gpsSatellite.getElevation()));

            // wait to translate the dx so the horizon doesn't get pushed off
            canvas.translate(0.0f - dx, 0.0f - dy);

            // draw our point -- we've rotated and translated this to the right spot already
            canvas.drawCircle(canvas.getWidth() / 2, canvas.getHeight() / 2, 8.0f, contentPaint);
            canvas.drawText(String.format(Locale.ITALY,"%d(%+3.0f  %+3.0f)",
                    gpsSatellite.getPrn() ,gpsSatellite.getAzimuth(), gpsSatellite.getElevation()),
                    10+canvas.getWidth() / 2, canvas.getHeight() / 2,
                    contentPaint);
//            canvas.drawText(String.format(Locale.ITALY,"%d", gpsSatellite.getPrn()),
//                    10, 0, contentPaint);

            // Reset canvas position
            canvas.translate(0.0f + dx, 0.0f + dy);

        }

        canvas.drawText(String.format(Locale.ITALY,"Satelliti: %3d",satellites),
                40, 20, contentPaint);


        // use roll for screen rotation
        //canvas.translate(0.0f+ canvas.getWidth()/2, 0.0f+canvas.getHeight()/2);
        canvas.rotate(0.0f - orientationDeg[2]);
        // make our line big enough to draw regardless of rotation and translation
        canvas.drawLine(0f - canvas.getHeight(), canvas.getHeight()/2, canvas.getWidth()+canvas.getHeight(), canvas.getHeight()/2, contentPaint);

//        // now translate the dx
//        canvas.translate(0.0f-dx, 0.0f);
//
    }


    // compute rotation matrix
    float rotation[] = new float[9];
    float identity[] = new float[9];
    float[] lastAccelerometer=new float[3];
    float[] lastCompass=new float[3];
    float orientation[] = new float[3];
    float orientationDeg[] = new float[3];
    float cameraRotation[] = new float[9];


    /*
    Lowpass filter seen @ http://phrogz.net/js/framerate-independent-low-pass-filter.html
        var smoothed   = 0;        // or some likely initial value
        var smoothing  = 10;       // or whatever is desired
        var lastUpdate = new Date;
        function smoothedValue( newValue ){
          var now = new Date;
          var elapsedTime = now - lastUpdate;
          smoothed += elapsedTime * ( newValue - smoothed ) / smoothing;
          lastUpdate = now;
          return smoothed;
        }
     */

    LowPassFilter lowPassFilterAccel=new LowPassFilter(), lowPassFilterMagnet=new LowPassFilter();

    @Override
    public void onSensorChanged(SensorEvent event) {

        long nowMillis=System.currentTimeMillis();

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//            lastAccelerometer[0]+=(nowMillis-lastSensorUpdateMillisAccel)*(event.values[0]-lastAccelerometer[0])/((float)smoothingValue);
//            lastAccelerometer[1]+=(nowMillis-lastSensorUpdateMillisAccel)*(event.values[1]-lastAccelerometer[1])/((float)smoothingValue);
//            lastAccelerometer[2]+=(nowMillis-lastSensorUpdateMillisAccel)*(event.values[2]-lastAccelerometer[2])/((float)smoothingValue);
//
//            lastSensorUpdateMillisAccel=nowMillis;
            lowPassFilterAccel.addVector(event.values);
            lastAccelerometer=lowPassFilterAccel.getVector();
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
//            lastCompass[0]+=(nowMillis-lastSensorUpdateMillisMagnet)*(event.values[0]-lastCompass[0])/((float)smoothingValue);
//            lastCompass[1]+=(nowMillis-lastSensorUpdateMillisMagnet)*(event.values[1]-lastCompass[1])/((float)smoothingValue);
//            lastCompass[2]+=(nowMillis-lastSensorUpdateMillisMagnet)*(event.values[2]-lastCompass[2])/((float)smoothingValue);
//
//            lastSensorUpdateMillisMagnet=nowMillis;

            lowPassFilterMagnet.addVector(event.values);
            lastCompass=lowPassFilterMagnet.getVector();
        }

        boolean gotRotation = SensorManager.getRotationMatrix(rotation,identity, lastAccelerometer, lastCompass);
        if (gotRotation) {
            // remap such that the camera is pointing straight down the Y axis
            SensorManager.remapCoordinateSystem(rotation, SensorManager.AXIS_X,
                    SensorManager.AXIS_Z, cameraRotation);

            // orientation vector
            SensorManager.getOrientation(cameraRotation, orientation);
        }

        orientationDeg[0]= (float)(Math.toDegrees(orientation[0]))%360;
        orientationDeg[1]= (float)(-Math.toDegrees(orientation[1]))%360;
        orientationDeg[2]= (float)(Math.toDegrees(orientation[2]))%360;


//        StringBuilder msg = new StringBuilder(event.sensor.getName()).append(" ");
//        for(float value: event.values)
//        {
//            msg.append("[").append(value).append("]");
//        }
//
//        switch(event.sensor.getType())
//        {
//            case Sensor.TYPE_ACCELEROMETER:
//                accelData = msg.toString();
//                break;
//            case Sensor.TYPE_GYROSCOPE:
//                gyroData = msg.toString();
//                break;
//            case Sensor.TYPE_MAGNETIC_FIELD:
//                compassData = msg.toString();
//                break;
//        }

        this.invalidate();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onGpsStatusChanged(int event) {
        String s = "";
        switch (event) {
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                s = "GPS_EVENT_FIRST_FIX";
                break;
            case GpsStatus.GPS_EVENT_STARTED:
                s = "GPS_EVENT_STARTED";
                applicationSettings.setGPSAvailable(true);
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                s = "GPS_EVENT_STOPPED";
                applicationSettings.setGPSAvailable(false);
//                turnOffAllUI();
                break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                s = "GPS_EVENT_SATELLITE_STATUS";
                break;
        }

//        TextView textView = (TextView) rootView.findViewById(R.id.txtGpsStatus);
//        textView.setText(s);

        GpsStatus gs = locationManager.getGpsStatus(null);

        final Iterator<GpsSatellite> it = gs.getSatellites().iterator();
        GpsSatellite gpsSatellite;

//        textView = (TextView) rootView.findViewById(R.id.txtFixTime);
//        textView.setText(String.valueOf(gs.getTimeToFirstFix()));

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
    }
}