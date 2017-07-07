package moon.lightsphone;

import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.*;
import java.io.*;
import javax.net.ssl.*;

import moon.shared.ExceptionTools;

/**
 * Created by moon on 6/4/2016.
 * Handles network text grabbing in a blocking manner.
 * FIXME: IS HACKY AND NEEDS UPDATING
 */
class moonNetworking {

    private static String getter = "";

    static String downloadText_MOON(String url) {

        //If we're on the main thread, thread the network call and make it wait for the return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                getter = new getter().execute(url).get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            return getter;
        }

        String str = "";
        try {
            int BUFFER_SIZE = 2000;
            InputStream in = null;
            int response = -1;
            URL urlo = new URL(url);
            URLConnection conn = urlo.openConnection();

            if (!(conn instanceof HttpURLConnection)) {
                //Log.e("Test", "Not an HTTP connection");
                //throw new IOException("Not an HTTP connection");
                return "";
            }

            HttpsURLConnection httpConn = (HttpsURLConnection) conn;
            httpConn.setConnectTimeout(3000);
            httpConn.setReadTimeout(3000);
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.connect();

            response = httpConn.getResponseCode();
            if (response == HttpsURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();
            }

            if (in != null) {
                InputStreamReader isr = new InputStreamReader(in);
                int charRead;
                char[] inputBuffer = new char[BUFFER_SIZE];
                try {
                    while ((charRead = isr.read(inputBuffer)) > 0) {
                        // ---convert the chars to a String---
                        String readString = String.copyValueOf(inputBuffer, 0, charRead);
                        str += readString;
                        inputBuffer = new char[BUFFER_SIZE];
                    }
                    in.close();
                } catch (IOException e) {
                    return "";
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }

    private static class getter extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            return moonNetworking.downloadText_MOON(params[0]);
        }
    }
}
