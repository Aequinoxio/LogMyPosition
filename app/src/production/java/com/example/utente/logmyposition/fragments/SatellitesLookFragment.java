package com.example.utente.logmyposition.fragments;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.utente.logmyposition.AugmentedReality.ArDisplayView;
import com.example.utente.logmyposition.AugmentedReality.OverlayView;
import com.example.utente.logmyposition.R;

import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SatellitesLookFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SatellitesLookFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SatellitesLookFragment extends Fragment implements SensorEventListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    Context mContext=null;
    Activity mActivity=null;
    private View rootView=null;
   // private View drawingView=null;
    private TextView txtAzimuth=null, txtElevation=null, txtRotation=null;

    // Sensori
    SensorManager sensorManager;
    SensorEventListener sensorEventListener;
    // New style (uso accelerometro e magnetometro invede di orientation
    Sensor accelerometer;
    Sensor magnetometer;


    private OnFragmentInteractionListener mListener;

    public SatellitesLookFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SatellitesLookFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SatellitesLookFragment newInstance(String param1, String param2) {
        SatellitesLookFragment fragment = new SatellitesLookFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView=inflater.inflate(R.layout.fragment_satellites_look, container, false);
        //drawingView=rootView.findViewById(R.id.imgDrawingView);
        txtAzimuth=(TextView) rootView.findViewById(R.id.txtAzimuthSatLook);
        txtElevation=(TextView) rootView.findViewById(R.id.txtElevationSatLook);
        txtRotation=(TextView) rootView.findViewById(R.id.txtRotazionSatLook);

        if (mActivity==null){
            mActivity=getActivity();
            mContext=getActivity().getApplicationContext();
        }

        // Sensori accelerometro e magnetometro
        sensorManager = (SensorManager) mActivity.getSystemService(Context.SENSOR_SERVICE);
        sensorEventListener=this;
        accelerometer=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer=sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

//        decommentare se i sensori sono del frame
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_UI);

        // Creo la SurfaceView per mostrare i satelliti
        FrameLayout arViewPane = (FrameLayout) rootView.findViewById(R.id.ar_view_pane);
       // ArDisplayView arDisplay = new ArDisplayView(mContext,mActivity);
       // arViewPane.addView(arDisplay);
        OverlayView arContent = new OverlayView(mContext);
        arViewPane.addView(arContent);
//        // Delego i sensori
//        sensorEventListener=arContent;
//        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
//        sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_UI);

        return rootView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (sensorEventListener!=null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    final int accumIndexMax=10;     // Calcolo media ultimi rilevamenti
    int accumIndexGravity=0;        // media mobile, mi serve un indice per indirizzare l'accumulatore
    int accumIndexGeomagnetic=0;    // media mobile, mi serve un indice per indirizzare l'accumulatore
    float[][] mGravity=new float[accumIndexMax][3];     // Valori misurati
    float[][] mGeomagnetic=new float[accumIndexMax][3]; // Valori misurati
    float[] mGravityMean=new float[3];                  // Valori medi
    float[] mGeomagneticMean=new float[3];              // valori medi
    private float currentDegree = 0f;                   // Angolo per l'animazione
    float azimuth=0f;                                   // Angolo per l'animazione
    float elevation=0f;                                 // Angolo per l'animazione
    float rotation=0f;                                  // Angolo epr l'animazione

    // Alloco solo una volta visto che altrimenti devo farlo per ogni cambiamento del sensore
    float Rot[] = new float[9];                         // Matrice di appoggio per il calcolo della rotazione
    float RotRemapped[] = new float[9];                         // Matrice di appoggio per il calcolo della rotazione
    float Inc[] = new float[9];                         // Matrice di appoggio per il calcolo dell'inclinazione
    float orientation[] = new float[3];                 // MAtrice di appoggio per il calcolo dell'orientamento
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Aggiorno la media mobile togliendo il vecchio valore ed aggiungendo quello nuovo
            mGravityMean[0]=mGravityMean[0]+(-mGravity[accumIndexGravity][0]+event.values[0])/((float)accumIndexMax);
            mGravityMean[1]=mGravityMean[1]+(-mGravity[accumIndexGravity][1]+event.values[1])/((float)accumIndexMax);
            mGravityMean[2]=mGravityMean[2]+(-mGravity[accumIndexGravity][2]+event.values[2])/((float)accumIndexMax);

            // Copio il nuovo valore
            System.arraycopy(event.values, 0, mGravity[accumIndexGravity], 0, event.values.length);

            // Aggiorno l'indice dell'accumulatore
            accumIndexGravity++;
            if(accumIndexGravity>=accumIndexMax){
                accumIndexGravity=0;
            }
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            // Aggiorno la media mobile togliendo il vecchio valore ed aggiungendo quello nuovo
            mGeomagneticMean[0]=mGeomagneticMean[0]+(-mGeomagnetic[accumIndexGeomagnetic][0]+event.values[0])/((float)accumIndexMax);
            mGeomagneticMean[1]=mGeomagneticMean[1]+(-mGeomagnetic[accumIndexGeomagnetic][1]+event.values[1])/((float)accumIndexMax);
            mGeomagneticMean[2]=mGeomagneticMean[2]+(-mGeomagnetic[accumIndexGeomagnetic][2]+event.values[2])/((float)accumIndexMax);

            System.arraycopy(event.values, 0, mGeomagnetic[accumIndexGeomagnetic], 0, event.values.length);
            accumIndexGeomagnetic++;
            if(accumIndexGeomagnetic>=accumIndexMax){
                accumIndexGeomagnetic=0;
            }
        }

        // Calcolo la rotazione dello schermo (see: http://stackoverflow.com/questions/18782829/android-sensormanager-strange-how-to-remapcoordinatesystem
        int mScreenRotation=mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int axisX=SensorManager.AXIS_X,axisY=SensorManager.AXIS_Z;

//        switch (mScreenRotation) {
//            case Surface.ROTATION_0:
//                axisX = SensorManager.AXIS_X;
//                axisY = SensorManager.AXIS_Y;
//                break;
//
//            case Surface.ROTATION_90:
//                axisX = SensorManager.AXIS_Y;
//                axisY = SensorManager.AXIS_MINUS_X;
//                break;
//
//            case Surface.ROTATION_180:
//                axisX = SensorManager.AXIS_MINUS_X;
//                axisY = SensorManager.AXIS_MINUS_Y;
//                break;
//
//            case Surface.ROTATION_270:
//                axisX = SensorManager.AXIS_MINUS_Y;
//                axisY = SensorManager.AXIS_X;
//                break;
//
//            default:
//                break;
//        }

        boolean gotRotation=SensorManager.getRotationMatrix(Rot, Inc, mGravityMean, mGeomagneticMean);

        if (gotRotation) {
            SensorManager.remapCoordinateSystem(Rot, axisX, axisY, RotRemapped);
            SensorManager.getOrientation(RotRemapped, orientation);
            // orientation[0] è l'azimuth in radianti da -pi a +pi
            // lo trasformo in gradi e lo riporto tra 0 e 360
            azimuth = (float)(Math.toDegrees(orientation[0])+360)%360;
            elevation= (float)(-Math.toDegrees(orientation[1]))%360;
            rotation= (float)(Math.toDegrees(orientation[2])+360)%360;
        }

        String s=String.format(Locale.ITALY,"%3.0f°",azimuth);
        txtAzimuth.setText(s);
        s=String.format(Locale.ITALY,"%3.0f°",elevation);
        txtElevation.setText(s);
        s=String.format(Locale.ITALY,"%3.0f°",rotation);
        txtRotation.setText(s);

//        ImageView imageView = (ImageView) rootView.findViewById(R.id.imgCompass);
//        RotateAnimation ra = new RotateAnimation(currentDegree,-azimuth,
//                Animation.RELATIVE_TO_SELF,0.5f,
//                Animation.RELATIVE_TO_SELF,0.5f);
//
//        ra.setInterpolator(new LinearInterpolator());
//        ra.setDuration(250);
//        ra.setFillAfter(true);
//        imageView.startAnimation(ra);
        currentDegree=-azimuth;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
