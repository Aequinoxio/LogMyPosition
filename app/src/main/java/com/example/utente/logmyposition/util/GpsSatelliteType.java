package com.example.utente.logmyposition.util;

/**
 * Created by utente on 01/08/2016.
 */
public class GpsSatelliteType {
    public static GpsSatelliteTypeValue getSatelliteType(int PRN){
            /*
            1–32: GPS
            33–54: Various SBAS systems (EGNOS, WAAS, SDCM, GAGAN, MSAS) – some IDs still unused
            55–64: not used (might be assigned to further SBAS systems)
            65–88: GLONASS
            89–96: GLONASS (future extensions?)
            97–192: not used
            193–195: QZSS
            196–200: QZSS (future extensions?)
            201–235: Beidou
             */
        if (PRN>=1 && PRN <=32){
            return GpsSatelliteTypeValue.GPS;
        }

        if (PRN>=33 && PRN <=54){
            return GpsSatelliteTypeValue.SBAS;
        }

        if ((PRN>=55 && PRN <=64) || (PRN>=97 && PRN <=192)){
            return GpsSatelliteTypeValue.NA;
        }

        if ((PRN>=65 && PRN <=96)){
            return GpsSatelliteTypeValue.GLONASS;
        }

        if ((PRN>=193&& PRN <=200)){
            return GpsSatelliteTypeValue.QZSS;
        }

        if ((PRN>=201 && PRN <=235)){
            return GpsSatelliteTypeValue.BEIDOU;
        }

        return GpsSatelliteTypeValue.NA;
    }

}
