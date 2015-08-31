package com.example.utente.logmyposition;

import android.app.Application;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

/**
 * Created by utente on 01/09/2015.
 */
@ReportsCrashes(
        mailTo = "prikeprok@gmail.com",
        customReportContent = { ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL,
                ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT, ReportField.SETTINGS_GLOBAL, ReportField.DEVICE_FEATURES,
                ReportField.SETTINGS_SECURE, ReportField.SETTINGS_SYSTEM, ReportField.SHARED_PREFERENCES, ReportField.THREAD_DETAILS
        },
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_report
        //formKey = "", // This is required for backward compatibility but not used
        //formUri = "http://www.backendofyourchoice.com/reportpath"
)
public class MyApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }
}
