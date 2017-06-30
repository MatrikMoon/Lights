package moon.shared;

import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
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

    private static ArrayList<MyClientTask> instances = new ArrayList<>();

    public MyClientTask(BaseToggleActivity act, String addr, int port){
        this.activity = act;
        this.dstAddress = addr;
        this.dstPort = port;
        instances.add(this);
    }

    //Get connction state
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
                        }
                        byteArrayOutputStream.reset();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        receiveThread.setName("RECEIVETHREAD");
        receiveThread.start();
    }

    //Review and act on commands received from the server
    private void parseCommands(String response) {
        if (response.equals("ON")) {
            activity.setToggle(true);
        }
        else if (response.equals("OFF")) {
            activity.setToggle(false);
        }
    }

    //Send command to the server
    public void send(final String string){
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Thread mThread = new Thread() {
                @Override
                public void run() {
                    low_send(string);
                }
            };
            mThread.start();
        }
        else {
            low_send(string);
        }
    }

    //Low send is a method to make the send() method more concise. Do not use.
    private void low_send(String string) {
        try {
            os.write((string + "<EOF>\0").getBytes());
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //@SuppressWarnings("all") //Suppress while loop warning
    public void execute() {
        Thread startThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!cancelled && !connected) {
                        try {
                            Log.i(dstAddress, Integer.toString(dstPort));

                            //If we're a wearable device, there's a high chance WIFI capabilities are disabled.
                            //If so, we'll set a timeout on the socket and fall back to communicating through
                            //the phone if we can.
                            if (activity.getType().equals("WEARABLE")) {
                                socket = new Socket();
                                socket.connect(new InetSocketAddress(dstAddress, dstPort), 1000);
                            }
                            else {
                                socket = new Socket(dstAddress, dstPort);
                            }
                            connected = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                            try {
                                Thread.sleep(10000);
                            } catch (Exception ex) {
                                e.printStackTrace();
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        startThread.setName("CONNECTTHREAD");
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
            }
        }
    }
}