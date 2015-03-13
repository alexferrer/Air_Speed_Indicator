package com.example.alex.myapplication;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;


public class MainActivity extends IOIOActivity {
    final int MPH = 0;
    final int KPH = 1;

    /* airspeed indicator adjust to zero */
    private float calibrate = 1.66f;
    private float sFactor  = 20.0f;
    private float cFactor  = 1.0f;


    private ToggleButton button_;
    int         speed_unit = MPH;
    TextView    units;
    TextView    speed;
    ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button_ = (ToggleButton) findViewById(R.id.button);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        units= (TextView) findViewById(R.id.text_units);
        speed= (TextView) findViewById(R.id.text_speed);


        Button button_kph = (Button) findViewById(R.id.button_kph);
        button_kph.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                units.setText("KPH");
                speed.setText("000");
                progressBar.setProgress(30);
                speed_unit = KPH;
                cFactor = 1.0f;

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
            case R.id.action_search:
                toast("action search happened");
                return true;
            case R.id.action_settings:
                toast("settings happened");
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
        /** The on-board LED. */
        private DigitalOutput led_;

        /** The analog input pin 40 */
        private AnalogInput  pin40_ ;

        /**
         * Called every time a connection with IOIO has been established.
         * Typically used to open pins.
         *
         * @throws ioio.lib.api.exception.ConnectionLostException
         *             When IOIO connection is lost.
         *
         * @see ioio.lib.util.IOIOLooper# setup()
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
         * @throws ConnectionLostException
         *             When IOIO connection is lost.
         * @throws InterruptedException
         * 				When the IOIO thread has been interrupted.
         *
         * @see ioio.lib.util.IOIOLooper#loop()
         */
        @Override
        public void loop() throws ConnectionLostException, InterruptedException {
            led_.write(!button_.isChecked());

            float raw = pin40_.getVoltage();

            /** calculate speed :
             *  read voltage
             *  susbtract baseline voltage
             *  convert voltage to KPH with sFactor
             *  convert to MPH if required cFactor*/

              final int voltage   = (int)( (raw - calibrate) * 100 * sFactor * cFactor) ;
            String pad = "";

            if (voltage < 10 ) pad = "0";

            final String vString = pad + Integer.toString(voltage);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    speed.setText(vString);
                    progressBar.setProgress(voltage);
                }
            });

            Thread.sleep(100);
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
     *
     * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
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
                    }
                } else {
                    if (--numConnected_ == 0) {
                        button_.setEnabled(false);
                    }
                }
            }
        });
    }
}