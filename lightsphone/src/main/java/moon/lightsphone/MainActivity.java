package moon.lightsphone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.lang.reflect.Field;

import moon.shared.BaseToggleActivity;
import moon.shared.ExceptionTools;
import moon.shared.MyClientTask;

public class MainActivity extends AppCompatActivity implements BaseToggleActivity {

    private ToggleButton tb;
    private EditText debugText;
    private Button rebootButton;

    MyClientTask m;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tb = (ToggleButton) findViewById(R.id.toggleButton);
        debugText = (EditText) findViewById(R.id.debugText);
        rebootButton = (Button) findViewById(R.id.rebootButton);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);

        setSupportActionBar(myToolbar);
        makeActionOverflowMenuShown();

        rebootButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                m.send("REBOOT");
            }
        });

        updateManager.updateIfNotUpToDate(this);

        //Sigh, let's get this out of the way
        if (!watchService.isRunning()) {
            watchService.m = this; //Set teh service's instance of this class to this instance
            startService(new Intent(this, watchService.class)); //Start the service and it will take care of the rest
        }
        else {
            watchService.connectActivity(this); //Use the service's connect method to attach our activity and request status
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
                    debugData(ExceptionTools.stackTraceToString(e));
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.menu, menu);

        MenuItem m = menu.findItem(R.id.rice_switch);
        m.setChecked(getIsRice());
        return true;
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

    public boolean getIsRice() {
        return this.getPreferences(MODE_PRIVATE).getBoolean("rice", false);
    }

    @SuppressWarnings("all") //Suppress .commit() warning
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.rice_switch:
                SharedPreferences s = this.getPreferences(MODE_PRIVATE);
                s.edit().putBoolean("rice", !s.getBoolean("rice", false)).commit();
                item.setChecked(getIsRice());
                m.sendThroughMessageApi("isRice " + (getIsRice() ? "ON" : "OFF")); //Send prefs update to wear
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    //devices with hardware menu button (e.g. Samsung Note) don't show action overflow menu
    private void makeActionOverflowMenuShown() {
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            Log.d("DEBUG", e.getLocalizedMessage());
        }
    }
}
