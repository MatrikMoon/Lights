package moon.shared;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ProviderInfo;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

/**
 * Created by Moon on 6/29/2017.
 * Shared client task for communicating with the lights
 */

//Task to keep networking off the main thread, shared by wear and phone for efficiency
public class MyClientTask {
    private Socket socket;
    private String state = "OFF";

    private String dstAddress;
    private int dstPort;
    private BufferedOutputStream os;
    private BaseToggleActivity activity;

    private boolean connected = false;
    private boolean cancelled = false;
    private boolean wearableFallback = false;

    private Messages m;

    private static ArrayList<MyClientTask> instances = new ArrayList<>();

    public MyClientTask(BaseToggleActivity act, String address, int port){
        this.activity = act;
        this.dstAddress = address;
        this.dstPort = port;
        instances.add(this);

        //If this is a phone, let's go ahead and get the MessageApi up and running so we're ready for any
        //incoming-devices-to-be
        if (activity.getType().equals("PHONE")) {
            m = new Messages(this);
            m.connect((Context)activity);
        }
    }

    //Pass along debug info
    void debugData(String data) {
        activity.debugData(data);
    }

    //Get connection state
    public boolean isConnected() {
        return connected;
    }

    //Get state
    public String getState() {
        return state;
    }

    //Set state
    public void setState(String state) {
        this.state = state;
    }

    //Return the typ eof
    String getType() {
        return activity.getType();
    }

    private void receive(final InputStream is) {
        Thread receiveThread = new Thread() {
            @Override
            public void run() {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(65535);
                byte[] buffer = new byte[65535];
                int bytesRead;
                while (!cancelled) {
                    try {
                        if ((bytesRead = is.read(buffer)) == -1) continue;
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                        String response = byteArrayOutputStream.toString("UTF-8");
                        try {
                            String[] s = response.split("<EOF>");
                            parseCommands(s[0]);
                        } catch (Exception e) {
                            e.printStackTrace();
                            debugData(ExceptionTools.stackTraceToString(e));
                        }
                        byteArrayOutputStream.reset();
                    }
                    //If it's a timeout exception, we're good for another round
                    catch (SocketTimeoutException e) {
                        //e.printStackTrace();
                        //debugData(ExceptionTools.stackTraceToString(e));
                    }
                    //If not, it's time for this thread to die
                    catch (Exception e) {
                        e.printStackTrace();
                        debugData(ExceptionTools.stackTraceToString(e));
                        return;
                    }
                }
            }
        };
        receiveThread.setName("RECEIVE-THREAD");
        receiveThread.start();
    }

    //Review and act on commands received from the server
    void parseCommands(String response) {
        //If we're the phone, broadcast the message off to a potential awaiting watch
        if (getType().equals("PHONE")) {
            //If the watch is requesting preferences, send them
            if (response.equals("REQUEST_PREFS")) {
                m.send("isRice " + (activity.getIsRice() ? "ON" : "OFF"));
            }
            else {
                m.send(response);
                debugData("parseCommands : broadcasting \"" + response + "\" back to watch");
            }
        }
        //If we're the wearable, we need to accept incoming prefs updates.
        //This isn't TOO important considering the watch will be using MessageApi the majority
        //of the time, but if we accept these updates, when the phone dies, the watch
        //will know what our settings are so it can at least have a chance at connecting to the
        //right address.
        else if (getType().equals("WEARABLE")) {
            if (response.startsWith("isRice ")) {
                String r = response.split(" ")[1];
                SharedPreferences s = ((Activity)activity).getPreferences(Context.MODE_PRIVATE);
                s.edit().putBoolean("rice", r.equals("ON")).commit();
            }
        }
        if (response.equals("ON")) {
            activity.setToggle(true);
        }
        else if (response.equals("OFF")) {
            activity.setToggle(false);
        }
    }

    //Send command to the server
    public void send(final String string){
        debugData("send : sending \"" + string + "\" to server");
        //If we're using MessageApi, send it as such
        if (wearableFallback) {
            sendThroughMessageApi(string);
        }
        //If not, do it the normal way
        else {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Thread mThread = new Thread() {
                    @Override
                    public void run() {
                        low_send(string);
                    }
                };
                mThread.start();
            } else {
                low_send(string);
            }
        }
    }

    //Send comand specifically to wear
    public void sendThroughMessageApi(final String s) {
        if ((m == null) || !m.isConnected()) {
            m = new Messages(this);
            m.connect((Activity)activity); //Cast activity to activity. Can't seem to figure out how to do it another way while leaving
                                            //the specific types of activities alone in the MainActivity files
        }
        m.send(s);
    }

    //Low send is a method to make the send() method more concise. Do not use.
    private void low_send(String string) {
        try {
            os.write((string + "<EOF>\0").getBytes());
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
            debugData(ExceptionTools.stackTraceToString(e));
        }
    }

    @SuppressWarnings("SpellCheckingInspection") //Suppress while loop warning
    public void execute() {
        final MyClientTask instance = this;
        Thread startThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!cancelled && !connected && !wearableFallback) {
                        try {
                            Log.i(dstAddress, Integer.toString(dstPort));

                            //If we're a wearable device, there's a high chance WIFI capabilities are disabled.
                            //If so, we'll set a timeout on the socket and fall back to communicating through
                            //the phone if we can.
                            if (getType().equals("WEARABLE")) {
                                socket = new Socket();
                                socket.connect(new InetSocketAddress(dstAddress, dstPort), 1000);
                            }
                            else {
                                socket = new Socket(dstAddress, dstPort);
                            }
                            connected = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                            debugData(ExceptionTools.stackTraceToString(e));
                            /*
                            if (e instanceof java.net.ConnectException && getType().equals("WEARABLE")) {
                                //There must be no connection to the phone OR the network in this case
                                //We're screwed
                            }
                            */
                            if (e instanceof java.net.SocketTimeoutException && getType().equals("WEARABLE")) {
                                wearableFallback = true;
                            }
                            else {
                                try {
                                    Thread.sleep(10000);
                                } catch (Exception ex) {
                                    e.printStackTrace();
                                    debugData(ExceptionTools.stackTraceToString(e));
                                }
                            }
                        }
                    }

                    //If connected, do things
                    //It's possible we're not connected if the task is being cancelled
                    if (connected) {
                        socket.setSoTimeout(5000);
                        InputStream inputStream = socket.getInputStream();
                        os = new BufferedOutputStream(socket.getOutputStream());

                        //Request current status
                        send("REQUEST_STATUS");

                        //Receive data
                        receive(inputStream);
                    }

                    //If we're a wearable connected through a phone, let's set that up
                    if (wearableFallback) {
                        debugData("CONNECT : wearableFallback triggered");
                        m = new Messages(instance);
                        m.connect((Context)instance.activity);

                        send("REQUEST_STATUS");
                        send("REQUEST_PREFS");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    debugData(ExceptionTools.stackTraceToString(e));
                }
            }
        });
        startThread.setName("CONNECT-THREAD");
        startThread.start();
    }

    //When the constructor is called, cancel all current tasks
    public static void killAll() {
        for (MyClientTask e : instances) {
            e.cancel();
        }
    }

    //Help the connection loop stop and kill the receive thread
    @SuppressWarnings("deprecation")
    private void cancel() {
        cancelled = true;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
                debugData(ExceptionTools.stackTraceToString(e));
            }
        }
    }
}