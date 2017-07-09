package moon.lightsphone;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import moon.shared.BaseToggleActivity;
import moon.shared.ExceptionTools;
import moon.shared.MyClientTask;

/**
 * Created by Moon on 7/1/2017.
 * This service makes sure the phone can always relay a watch message if it needs to,
 * even when the app isn't open.
 */

public class watchService extends Service implements BaseToggleActivity {

    //MainActivity m;
    static boolean running = false;
    static watchService w;

    //FIXME: This is ugly and bad.
    static MainActivity m;

    static boolean scheduled = false;

    //We need a default constructor for AndroidManifest to register this as a service
    @SuppressWarnings("unused")
    public watchService() {
        w = this;
        running = true;
    }

    //Our own instance of MCT, if there's no foreground running
    //FIXME: Why, why, why do I always fall back to using static fields
    private static MyClientTask mct;

    @Override
    public void onCreate() {
        //Use parent class's MyClientTask
        if (m != null) {
            if (m.m == null || !m.m.isConnected()) {
                debugData("First stop");
                if (!getIsRice()) {
                    mct = new MyClientTask(m, "192.168.1.101", 10150); //Create our own mct
                }
                else {
                    mct = new MyClientTask(m, "192.168.1.126", 9875); //Create our own mct
                }
                m.m = mct; //Set the paren't instance to our own
                mct.execute(); //Run it
            }
        }
        //No parent? No problem. Let's just use our own.
        else {
            if (mct == null || !mct.isConnected()) {
                debugData("Second stop");
                if (!getIsRice()) {
                    mct = new MyClientTask(this, "192.168.1.101", 10150); //Create our own mct
                }
                else {
                    mct = new MyClientTask(this, "192.168.1.126", 9875); //Create our own mct
                }
                mct.execute();
            }
        }
    }

    public static void connectActivity(MainActivity main) {
        m = main; //Set our instance of MainActivity to the one passed in
        m.m = mct; //Give MainActivity our instance of the MCT
        mct.send("REQUEST_STATUS"); //Since the MCT in the service is already running, we'll go ahead and do another status request
    }

    //When connected to the lights, this toggle allows the widget to easily toggle the state
    public static void widgetToggle() {
        try {
            if (getState().equals("OFF")) {
                mct.send("ON");
                mct.setState("ON");
            }
            else if (getState().equals("ON")) {
                mct.send("OFF");
                mct.setState("OFF");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            //debugData(ExceptionTools.stackTraceToString(e));
        }
    }

    //Returns the current lights state, if it can
    public static String getState() {
        return (mct != null ? mct.getState() : "OFF");
    }

    public static boolean isRunning() {
        return running;
    }

    public void debugData(String data) {
        if (m != null) {
            m.debugData(data);
        }
        Log.i("DEBUG", data);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public String getType() {
        return "PHONE";
    }

    public boolean getIsRice() {
        if (m != null) {
            return m.getIsRice();
        }

        //Whoops, there's no parent. Let's handle this ourselves.
        return this.getSharedPreferences("moon.lightsphone.MainActivity", MODE_PRIVATE).getBoolean("rice", false);
    }

    @Override
    public void setToggle(boolean toggle) {
        //Call parent's function
        if (m != null) {
            m.setToggle(toggle);
        }
        ButtonWidget.setState(toggle ? "ON" : "OFF", this);
    }
}
