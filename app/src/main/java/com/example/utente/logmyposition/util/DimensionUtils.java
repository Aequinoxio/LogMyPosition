package com.example.utente.logmyposition.util;

import android.view.View;

/**
 * Created by utente on 11/04/2016.
 *
 * Thanx to http://stackoverflow.com/questions/4605527/converting-pixels-to-dp
 *
 */
class DimensionUtils {
    private static boolean isInitialised = false;
    private static float pixelsPerOneDp;

    private static void initialise(View view) {
        pixelsPerOneDp = view.getResources().getDisplayMetrics().densityDpi / 160f;
        isInitialised = true;
    }

    public static float pxToDp(View view, float px) {
        if (!isInitialised) {
            initialise(view);
        }

        return px / pixelsPerOneDp;
    }

    public static float dpToPx(View view, float dp) {
        if (!isInitialised) {
            initialise(view);
        }

        return dp * pixelsPerOneDp;
    }

}
