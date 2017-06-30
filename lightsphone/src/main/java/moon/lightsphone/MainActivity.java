package moon.lightsphone;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ToggleButton;

import moon.shared.BaseToggleActivity;
import moon.shared.MyClientTask;

public class MainActivity extends AppCompatActivity implements BaseToggleActivity{

    private ToggleButton tb;

    //m = new MyClientTask("192.168.1.126", 9875);
    MyClientTask m = new MyClientTask(this, "192.168.1.101", 10150);

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m.cancel(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tb = (ToggleButton) findViewById(R.id.toggleButton);

        //Set status of toggle on resume
        if (m.getState().equals("ON")) {
            setToggle(true);
        }
        else {
            setToggle(false);
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
                }
            }
        });

        try {
            //Only start a new task if it's not already running
            if ((m == null) || (m.getStatus() != AsyncTask.Status.RUNNING)) {
                 m.execute();
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
}
