package moon.lightsphone;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ToggleButton;

import moon.shared.BaseToggleActivity;
import moon.shared.MyClientTask;

public class MainActivity extends AppCompatActivity implements BaseToggleActivity {

    private ToggleButton tb;

    //m = new MyClientTask("192.168.1.126", 9875);
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

        //Sigh, let's get this out of the way
        watchService w = new watchService(this);
        startService(new Intent(this, w.getClass()));

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
            if ((m == null) || !m.isConnected()) {
                m = new MyClientTask(this, "192.168.1.101", 10150);
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

    //Gets the activity type
    public String getType() {
        return "PHONE";
    }
}
