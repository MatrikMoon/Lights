package moon.lightsphone;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.net.Socket;

import moon.shared.BaseToggleActivity;
import moon.shared.MyClientTask;

/**
 * Created by Moon on 7/1/2017.
 */

public class watchService extends Service implements BaseToggleActivity {

    //Ugly, but it works
    public static MainActivity m;

    //Our own instance of MCT, if there's no foreground running
    private MyClientTask mct;

    @Override
    public void onCreate() {
        //Use parent class's MyClientTask
        if (m != null) {
            if (m.m == null || !m.m.isConnected()) {
                m.m = new MyClientTask(m, "192.168.1.101", 10150);
                m.m.execute();
            }
        }
        else {
            if (mct == null || !mct.isConnected()) {
                mct = new MyClientTask(this, "192.168.1.101", 10150);
                mct.execute();
            }
        }
        Log.i("SERVICE", "MCT STARTED");
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public String getType() {
        return "PHONE";
    }

    @Override
    public void setToggle(boolean toggle) {
        //Call parent's function
        if (m != null) {
            m.setToggle(toggle);
        }
    }
}
