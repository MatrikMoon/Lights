package moon.lightsphone;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import moon.shared.BaseToggleActivity;
import moon.shared.MyClientTask;

/**
 * Created by Moon on 7/1/2017.
 * This service makes sure the phone can always relay a watch message if it needs to,
 * even when the app isn't open.
 */

public class watchService extends Service implements BaseToggleActivity {

    MainActivity m;

    //We need a default constructor for AndroidManifest to register this as a service
    @SuppressWarnings("unused")
    public watchService() {

    }

    //Let's add the current activity instance to the service
    public watchService(MainActivity m) {
        this.m = m;
    }

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
        //No parent? No problem. Let's just use our own.
        else {
            if (mct == null || !mct.isConnected()) {
                mct = new MyClientTask(this, "192.168.1.101", 10150);
                mct.execute();
            }
        }
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
