package com.example.alex.myapplication;
/**
 Make_NMEA_sentence.java
 Author: Alex Ferrer  04/05/2015
 Copyright(c) 2015 ferrerdallas@gmail.com Licence: GPL 2.0 or later
 Based on PI_XTCPgps.py version 0.1 beta 1

 **/

public class NMEAHelper {

    private String getCksum(String sentence) {
// calculates the checksum for NMEA sentences
// return as a string
        int i = 0;
        int cksum = 0;
        int senlen = sentence.length();
        while (i < senlen) {
            // cksum xor (ascii number of each character in the sentence)
            char c = (char) sentence.charAt(i) ;  //get the char at x position
            int  cc = (int) c;
            cksum = cksum ^ cc ; // cksum^ord(sentence[i:i+1])
            i = i + 1;
        }

        String acksum = Integer.toHexString(cksum);  //hex(cksum)[2:]  (no need to [2:], java return only the hex value)
        return String.valueOf(acksum);
    }


    private String getGPRMC() {
//  construct the nmea gprmc sentence
        String n_time = "";
        String n_lat = "";
        String n_lon = "";
        String n_speed = "";
        String n_heading = "";
        String n_date = "";
        String n_magvar = "";


        String gprmc =
                "GPRMC" + ","
                        + n_time + ","
                        + "A" + ","
                        + n_lat + ","
                        + n_lon + ","
                        + n_speed + ","
                        + n_heading + ","
                        + n_date + ","
                        + n_magvar;

        // append check sum and inital $
        String cks = getCksum(gprmc);
        gprmc = "$" + gprmc + "*" + cks + "\r\n";
        return gprmc;
    }

    private String getGPGGA() {
        //construct the nmea gpgga sentence
        String n_time = "";
        String n_lat = "";
        String n_lon = "";
        String n_alt = "";

        String gpgga =
                "GPGGA" + ","
                        + n_time + ","
                        + n_lat + ","
                        + n_lon + ",1,04,0.0,"
                        + n_alt + ",M,,,,";
        //append check sum and inital $
        String cks = getCksum(gpgga);
        gpgga = "$" + gpgga + "*" + cks + "\r\n";
        return gpgga;
    }

    private String getGPGSA() {
        return "$GPGSA,A,3,13,20,31,,,,,,,,,,02.2,02.2,*1e\r\n";
    }

    private String getLXWP0()    {

        String n_logger = "";
        String n_ias = "";
        String n_baroalt = "";
        String n_vario = "";
        String n_dummy = "";
        String n_heading = "";
        String n_winddir = "";
        String n_windspd = "";

        String lxwp0 = "LXWP0" + ","
                + n_logger + ","
                + n_ias + ","
                + n_baroalt + ","
                + n_vario + ","
                + n_dummy + ","
                + n_heading + ","
                + n_winddir + ","
                + n_windspd;
        String cks = getCksum(lxwp0);
        lxwp0 = "$" + lxwp0 + "*" + cks + "\r\n";
        return lxwp0;
    }
    
}