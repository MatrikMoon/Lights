package moon.lightswear;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.ToggleButton;

import moon.shared.BaseToggleActivity;
import moon.shared.MyClientTask;

public class MainActivity extends WearableActivity implements BaseToggleActivity {

    private BoxInsetLayout mContainerView;
    private ToggleButton tb;

    //m = new MyClientTask("192.168.1.126", 9875);
    MyClientTask m;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("ONDESTROY", "ONDESTROY");
        //m.cancel(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("ONPAUSE", "ONPAUSE");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("ONRESUME", "ONRESUME");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        tb = (ToggleButton) findViewById(R.id.toggleButton);

        tb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (m.getState().equals("OFF")) {
                        m.send("ON");
                        m.setState("ON");
                    }
                    else if (m.getState().equals("ON")) {
                        m.send("OFF");
                        m.setState("OFF");
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        try {

            Log.i("M NULL", (m == null ? "TRUE" : "FALSE"));
            if (m != null) {
                Log.i("M NOT RUNNING", (m.getStatus() != AsyncTask.Status.RUNNING ? "TRUE" : "FALSE"));
                Log.i("M NOT CONNECTED", (!m.isConnected() ? "TRUE" : "FALSE"));
            }

            //Only start a new task if it's not already running
            if ((m == null) || (m.getStatus() != AsyncTask.Status.RUNNING) || !m.isConnected()) {
                m = new MyClientTask(this, "192.168.1.101", 10150);
                m.execute();
                Log.i("NEW TASK", "CREATED");
            }

            //Set status of toggle on resume/create
            if (m.getState().equals("ON")) {
                setToggle(true);
            }
            else {
                setToggle(false);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Sets the state of the toggle button
    public void setToggle(final boolean b) {
        m.setState(b ? "ON" : "OFF");
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.i("CHECKED", (b ? "TRUE" : "FALSE"));
                tb.setChecked(b);
            }
        });
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    @SuppressWarnings("deprecation")
    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
        } else {
            mContainerView.setBackground(null);
        }
    }
}
