package com.example.utente.logmyposition;

import android.content.Context;
import android.location.LocationManager;
import android.location.Location;
import android.os.SystemClock;

import java.util.Date;

/**
 * Created by utente on 18/08/2015.
 * Grasie a https://mobiarch.wordpress.com/2012/07/17/testing-with-mock-location-data-in-android/
 */
public class MockLocationProvider {
    String providerName;
    Context ctx;

    public MockLocationProvider(String name, Context ctx) {
        this.providerName = name;
        this.ctx = ctx;

        LocationManager lm = (LocationManager) ctx.getSystemService(
                Context.LOCATION_SERVICE);
        lm.addTestProvider(providerName, false, false, false, false, true,
                true, true, 0, 5);
        lm.setTestProviderEnabled(providerName, true);
    }

    public void pushLocation(double lat, double lon) {
        LocationManager lm = (LocationManager) ctx.getSystemService(
                Context.LOCATION_SERVICE);

        Location mockLocation = new Location(providerName);
        mockLocation.setLatitude(lat);
        mockLocation.setLongitude(lon);
        mockLocation.setAltitude(120);
        mockLocation.setTime(System.currentTimeMillis());

        mockLocation.setProvider(LocationManager.GPS_PROVIDER);

        mockLocation.setAccuracy(8.0f);
        mockLocation.setSpeed(15.0f);
        mockLocation.setBearing(25.0f);
        mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

        /*
        location.setLatitude(latitude);
location.setLongitude(longitude);
location.setBearing(bearing);
location.setSpeed(speed);
location.setAltitude(altitude);
location.setTime(new Date().getTime());
location.setProvider(LocationManager.GPS_PROVIDER);
location.setAccuracy(1);
         */

        lm.setTestProviderLocation(providerName, mockLocation);
    }

    public void shutdown() {
        LocationManager lm = (LocationManager) ctx.getSystemService(
                Context.LOCATION_SERVICE);
        lm.removeTestProvider(providerName);
    }
}