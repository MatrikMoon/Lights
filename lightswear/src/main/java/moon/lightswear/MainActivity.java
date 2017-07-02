package moon.lightswear;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.view.View;
import android.widget.EditText;
import android.widget.ToggleButton;

import moon.shared.BaseToggleActivity;
import moon.shared.ExceptionTools;
import moon.shared.MyClientTask;

public class MainActivity extends WearableActivity implements BaseToggleActivity {

    private BoxInsetLayout mContainerView;
    private ToggleButton tb;
    private EditText debugText;

    MyClientTask m;

    //Kill networking when we go out of focus
    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyClientTask.killAll();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = findViewById(R.id.container);
        tb = findViewById(R.id.toggleButton);
        debugText = findViewById(R.id.debugText);

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
                    ExceptionTools.stackTraceToString(e);
                }
            }
        });

        tb.setLongClickable(true);
        tb.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                debugText.setVisibility(View.VISIBLE);
                debugText.setFocusable(false);
                return true;
            }
        });

        try {
            //Only start a new task if it's not already running
            if ((m == null) || !m.isConnected()) {
                m = new MyClientTask(this, "192.168.1.101", 10150);
                m.execute();
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
            ExceptionTools.stackTraceToString(e);
        }
    }

    public void debugData(final String data) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                debugText.append(data + "\n");
            }
        });
    }

    //Sets the state of the toggle button
    public void setToggle(final boolean b) {
        m.setState(b ? "ON" : "OFF");
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                debugData("CHECKED :" +  (b ? "TRUE" : "FALSE"));
                tb.setChecked(b);
            }
        });
    }

    //Gets the activity type
    public String getType() {
        return "WEARABLE";
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
