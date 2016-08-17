package com.example.utente.logmyposition.util;

import android.location.GpsSatellite;

/**
 * Created by utente on 01/08/2016.
 */
public class GpsInfo{
    public int PRN;
    public float SNR;
    public boolean usedInFix, hasAlmanac, hasEphemeris;
    public float azimuth, elevation;
    public String SatelliteConstellation;
    public boolean dataUpdated;

    public void resetAll(){
        PRN=0;
        SNR=0.0f;
        usedInFix=false;
        hasAlmanac=false;
        hasEphemeris=false;
        azimuth=0.0f;
        elevation=0.0f;
        SatelliteConstellation="??";
        dataUpdated=false;
    }

    public void updateValues(GpsSatellite gpsSatellite){
        PRN=gpsSatellite.getPrn();
        SNR=gpsSatellite.getSnr();
        usedInFix = gpsSatellite.usedInFix();
        azimuth= gpsSatellite.getAzimuth();
        elevation= gpsSatellite.getElevation();
        hasAlmanac= gpsSatellite.hasAlmanac();
        hasEphemeris= gpsSatellite.hasEphemeris();
        dataUpdated=true;

        switch (GpsSatelliteType.getSatelliteType(PRN)){
            case BEIDOU:
                SatelliteConstellation="BEI"; break;
            case SBAS:
                SatelliteConstellation="SBA"; break;
            case GLONASS:
                SatelliteConstellation="GLO"; break;
            case QZSS:
                SatelliteConstellation="QZS"; break;
            case GPS:
                SatelliteConstellation="GPS"; break;
            default:
                SatelliteConstellation="na";
        }
    }


    public GpsInfo(){
        resetAll();

        /////// DEBUG
//            usedInFix=rnd.nextBoolean();
//            azimuth=rnd.nextInt(360);
//            elevation=rnd.nextInt(90);
//            PRN=rnd.nextInt(236);
//
//            if (rnd.nextBoolean()) {
//                SNR = rnd.nextInt(10);
//            }

    }
}