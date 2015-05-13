package com.example.alex.myapplication;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;




public class MainActivity extends IOIOActivity {
    private String LOG_FILENAME ;
    final int MPH = 0;
    final int KPH = 1;

    /* airspeed indicator adjust to zero */
    private float calibrate = 1.68f;
    private float sFactor = 5.0f;
    private float cFactor = 1.0f;
    private float raw_voltage = 0;

    /* for average airspeed calculation */
    private float sample_sum = 0.0f;
    private int sample_count = 0;
    private int sample_size = 3;
    private float avg_voltage = 0.0f;


    /* define ui elements   */
    private ToggleButton button_;
    int speed_unit = MPH;
    TextView units;
    TextView speed;
    ProgressBar progressBar;
    TextView fvoltage_Text;
    TextView cfactor_Text;
    TextView avgfactor_Text;

    //xx
    //final Context context = this;
    private EditText result;

    //define barometric sensor
    private SensorManager mSensorManager = null;
    private String barometric_altitude = "";

    // define location manager (gps)
    LocationManager locationManager = null;
    private String gpsSpeed = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set an inital log filename.. may not bee needed.
        LOG_FILENAME = "iolog_"+String.valueOf(java.lang.System.currentTimeMillis()).substring(5,10)+".txt";

        //get sensor manager instance. (for barometer)
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //for gps
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);





        button_ = (ToggleButton) findViewById(R.id.button);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        units = (TextView) findViewById(R.id.text_units);
        speed = (TextView) findViewById(R.id.text_speed);
        fvoltage_Text = (TextView) findViewById(R.id.voltage);
        cfactor_Text = (TextView) findViewById(R.id.cFactor);
        avgfactor_Text = (TextView) findViewById(R.id.avgFactor);

        //xx
        result = (EditText) findViewById(R.id.editText);


        Button button_kph = (Button) findViewById(R.id.button_kph);
        button_kph.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                units.setText("KPH");
                speed.setText("000");
                progressBar.setProgress(30);
                speed_unit = KPH;
                cFactor = 1.0f;
                //turn on async task --------------
                //ConnectTask task = new ConnectTask();
                //task.execute();


            }
        });

        Button button_mph = (Button) findViewById(R.id.button_mph);
        button_mph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                units.setText("MPH");
                speed.setText("555");
                progressBar.setProgress(50);
                speed_unit = MPH;
                cFactor = 0.6213f;
                //TCP-----------------
                /*
                if(TcpClient.isConnected)
                {
                    mTcpClient.stopClient();
                }
                */

            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.calibrate:
                toast("Calibrating meeter to zero");
                calibrate = raw_voltage;

                return true;
            case R.id.action_settings:
                toast("settings happened");
                showDialog();

                return true;

            case R.id.cfactor_up:
                toast("sfactor up ");
                sFactor = sFactor + 2;
                return true;

            case R.id.cfactor_down:
                toast("sfactor down ");
                sFactor = sFactor - 2;
                return true;

            case R.id.avgfactor_up:
                toast("avgfactor up ");
                sample_size = sample_size + 2;
                return true;

            case R.id.avgfactor_down:
                toast("avgfactor down ");
                sample_size = sample_size - 2;
                return true;


            default:
                return super.onOptionsItemSelected(item);
        }
    }
    //---------------------

    /**
     * This is the thread on which all the IOIO activity happens. It will be run
     * every time the application is resumed and aborted when it is paused. The
     * method setup() will be called right after a connection with the IOIO has
     * been established (which might happen several times!). Then, loop() will
     * be called repetitively until the IOIO gets disconnected.
     */
    class Looper extends BaseIOIOLooper {
        /**
         * The on-board LED.
         */
        private DigitalOutput led_;

        /**
         * The analog input pin 40
         */
        private AnalogInput pin40_;

        /**
         * Called every time a connection with IOIO has been established.
         * Typically used to open pins.
         *
         * @throws ioio.lib.api.exception.ConnectionLostException When IOIO connection is lost.
         *
         */
        @Override
        protected void setup() throws ConnectionLostException {
            showVersions(ioio_, "IOIO connected!");
            led_ = ioio_.openDigitalOutput(0, true);
            pin40_ = ioio_.openAnalogInput(40);

            enableUi(true);
        }

        /**
         * Called repetitively while the IOIO is connected.
         *
         * @throws ConnectionLostException When IOIO connection is lost.
         * @throws InterruptedException    When the IOIO thread has been interrupted.
         * @see ioio.lib.util.IOIOLooper#loop()
         */
        @Override
        public void loop() throws ConnectionLostException, InterruptedException {
            led_.write(!button_.isChecked());

            //gat AIS voltage from sensor
            raw_voltage = pin40_.getVoltage();

            //simple low pass filter by averaging x count samples.
            sample_sum += raw_voltage;
            sample_count += 1;

            if (sample_count >= sample_size) {
                avg_voltage = sample_sum / sample_count;
                sample_count = 0;
                sample_sum = 0;
            }


            String avgVoltageString = Float.toString(avg_voltage);
            //the voltage sensor precision is .003, so trim the last 4 digits of the .01234567 float
            if (avgVoltageString.length()> 5 ) {
                avgVoltageString = avgVoltageString.substring(0, avgVoltageString.length() - 4);
            }
            final String voltage_string = avgVoltageString;

            /** calculate speed :
             *  convert voltage to pascals
             *  convert pascals to m/s
             */

            final int avg_speed_value = (int) mpsToKph(pressureToAirspeed(voltsToPressure(avg_voltage)));
            Log.i("avgSpeed", " volts" + avg_voltage);
            Log.i("avgSpeed", " voltsToPressure" + voltsToPressure(avg_voltage));
            Log.i("avgSpeed", " PressureToAirspeed" + pressureToAirspeed(voltsToPressure(avg_voltage)));
            Log.i("avgSpeed", " #" + avg_speed_value);


            String pad = "";
            if (avg_speed_value < 100) pad = "0";
            if (avg_speed_value < 10)  pad = "00";
            if (avg_speed_value < 0)   pad = "";

            final String speed_string = pad + Integer.toString(avg_speed_value);
            final String sfactor_string = Integer.toString((int) sFactor) + "x";
            final String avgfactor_string = Integer.toString( sample_size) + "~";


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    speed.setText(speed_string);
                    progressBar.setProgress(avg_speed_value);
                    fvoltage_Text.setText(voltage_string);
                    cfactor_Text.setText(sfactor_string);
                    avgfactor_Text.setText(avgfactor_string + " /" + gpsSpeed);
                }
            });

            Thread.sleep(200);

            if (button_.isChecked() ) {
                if (sample_count == 0)  {
                           logToFile(barometric_altitude,voltage_string,gpsSpeed); //log only when averages are done
                         }
            }
            else {
                //keep reset the logging filename until it is needed.. (yes it is ugly.. but simple)
                LOG_FILENAME = "iolog_"+String.valueOf(java.lang.System.currentTimeMillis()).substring(5,10)+".txt";
            }
            //pin40_.close();
        }

        /**
         * Called when the IOIO is disconnected.
         *
         * @see ioio.lib.util.IOIOLooper#disconnected()
         */
        @Override
        public void disconnected() {
            enableUi(false);
            toast("IOIO disconnected");
        }

        /**
         * Called when the IOIO is connected, but has an incompatible firmware version.
         *
         * @see ioio.lib.util.IOIOLooper#incompatible(ioio.lib.api.IOIO)
         */
        @Override
        public void incompatible() {
            showVersions(ioio_, "Incompatible firmware version!");
        }
    }

    /**
     * A method to create our IOIO thread.
     */
    @Override
    protected IOIOLooper createIOIOLooper() {
        return new Looper();
    }

    private void showVersions(IOIO ioio, String title) {
        toast(String.format("%s\n" +
                        "IOIOLib: %s\n" +
                        "Application firmware: %s\n" +
                        "Bootloader firmware: %s\n" +
                        "Hardware: %s",
                title,
                ioio.getImplVersion(IOIO.VersionType.IOIOLIB_VER),
                ioio.getImplVersion(IOIO.VersionType.APP_FIRMWARE_VER),
                ioio.getImplVersion(IOIO.VersionType.BOOTLOADER_VER),
                ioio.getImplVersion(IOIO.VersionType.HARDWARE_VER)));
    }

    private void toast(final String message) {
        final Context context = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int numConnected_ = 0;

    private void enableUi(final boolean enable) {
        // This is slightly trickier than expected to support a multi-IOIO use-case.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (enable) {
                    if (numConnected_++ == 0) {
                        button_.setEnabled(true);
                        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL);

                    }
                } else {
                    if (--numConnected_ == 0) {
                        button_.setEnabled(false);
                        mSensorManager.unregisterListener(mSensorListener);

                    }
                }
            }
        });


    }


    //-------
    void showDialog() {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(
                R.string.hello_world);
        newFragment.show(getFragmentManager(), "dialog");
    }

    public void doPositiveClick() {
        // Do stuff here.
        toast("Positive click");
    }

    public void doNegativeClick() {
        // Do stuff here.
        toast("Negative click");
    }


    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int title) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("title", title);
            frag.setArguments(args);
            return frag;
        }

        @TargetApi(21)
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int title = getArguments().getInt("title");

        /* /xx
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.settings_dialog, null);
        final EditText userInput = (EditText) promptsView.findViewById(R.id.editText);
       */


            return new AlertDialog.Builder(getActivity())
                    .setView(R.layout.settings_dialog)
                    .setIcon(R.drawable.search)
                    .setTitle(title)
                    .setPositiveButton(R.string.fire,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((MainActivity) getActivity()).doPositiveClick();
                                    //xx
                                    //result.setText(userInput.getText());
                                }
                            }
                    )
                    .setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((MainActivity) getActivity()).doNegativeClick();
                                }
                            }
                    )
                    .create();
        }
    }

    //-------

    private void logToFile(String... text) {
        // Start with current timestamp
        String outtext = String.valueOf(java.lang.System.currentTimeMillis()).substring(5); //milliseconds since midnight
        //concatenate all the log values sent on text separated by ","
        for (String s: text){
            outtext+= " , " + s;
        }

        writeToFile(outtext + System.getProperty("line.separator") );
    }

    //write someting to disk
    private void writeToFile(String content) {
        File file;
        FileOutputStream outputStream;
        try {
            file = new File(Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), LOG_FILENAME);

            outputStream = new FileOutputStream(file, true);
            outputStream.write(content.getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private SensorEventListener mSensorListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // when accuracy changed, this method will be called.
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // when pressure value is changed, this method will be called.
            float pressure_value = 0.0f;
            float height = 0.0f;

            // if you use this listener as listener of only one sensor (ex, Pressure), then you don't need to check sensor type.
            if (Sensor.TYPE_PRESSURE == event.sensor.getType()) {
                pressure_value = event.values[0];
                height = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure_value);
                barometric_altitude = String.valueOf(height);
            }
        }
    };

    private LocationListener mLocationListener = new LocationListener(){
        @Override
        public void onLocationChanged(Location location) {
            location.getLatitude();
            gpsSpeed = String.valueOf(location.getSpeed());

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }


    };

    /* MPXV7002 signal (volts) to kPa (KiloPascals
     * As per IC datasheet
     * assuming a 2/3 resistor bridge to cap max voltage to 3.3v
     * return value in pascals

       */
    private double voltsToPressure(float volts){

           return 1.5 * (volts - calibrate) *1000;
       }

    /* Convert Pascals to airspeed in m/s

    */
    private double pressureToAirspeed(double pressure){
          final double AIR_DENSITY_SEA_LEVEL = 1.255;
          return Math.sqrt( (2*pressure)/ AIR_DENSITY_SEA_LEVEL);

      }

    /* meters per second to kilomentesr per hour */
    private double mpsToKph(double mps) {
        return mps * 3.6 ;
    }

}

