package moon.lightsphone;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

import moon.shared.BaseToggleActivity;
import moon.shared.ExceptionTools;
import moon.shared.MyClientTask;

public class MainActivity extends AppCompatActivity implements BaseToggleActivity {

    private ToggleButton tb;
    private EditText debugText;
    private Button rebootButton;

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

        tb = (ToggleButton) findViewById(R.id.toggleButton);
        debugText = (EditText) findViewById(R.id.debugText);
        rebootButton = (Button) findViewById(R.id.rebootButton);

        rebootButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                m.send("REBOOT");
            }
        });

        //Sigh, let's get this out of the way
        if (!watchService.isRunning()) {
            watchService w = new watchService(this);
            startService(new Intent(this, w.getClass()));
        }
        else {
            m = watchService.getMCT();
            watchService.getInstance().setParent(this);

        }

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
                rebootButton.setVisibility(View.VISIBLE);
                return true;
            }
        });

        try {
            //Only start a new task if it's not already running
            if ((m == null) || !m.isConnected()) {
                m = new MyClientTask(this, "192.168.1.101", 10150);
                m.execute();
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
                Log.i("CHECKED", (b ? "TRUE" : "FALSE"));
                tb.setChecked(b);
            }
        });
    }

    //Gets the activity type
    public String getType() {
        return "PHONE";
    }
}
