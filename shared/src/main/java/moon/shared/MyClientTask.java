package moon.shared;

import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Created by Moon on 6/29/2017.
 * Shared client task for communicating with the lights
 */

//Task to keep networking off the main thread, shared by wear and phone for efficiency
public class MyClientTask extends AsyncTask<Void, Void, Void> {
    private Socket socket;
    private String state = "OFF";

    private String dstAddress;
    private int dstPort;
    private BufferedOutputStream os;
    private BaseToggleActivity activity;

    private boolean connected = false;

    public MyClientTask(BaseToggleActivity act, String addr, int port){
        this.activity = act;
        this.dstAddress = addr;
        this.dstPort = port;
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
            return;
        }
        low_send(string);
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

    @SuppressWarnings("")
    @Override
    protected Void doInBackground(Void... arg0) {
        boolean cont = true;
        while(cont) { //if the connection dies, connect again
            try {
                int tries = 0;
                while (!connected) {
                    try {
                        Log.i(dstAddress, Integer.toString(dstPort));
                        socket = new Socket(dstAddress, dstPort);
                        connected = true;
                    }
                    catch (Exception e) {
                        tries++;
                        e.printStackTrace();

                        //Stop trying to connect after a while
                        if (tries > 15) {
                            break;
                        }
                    }
                }

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(65535);
                byte[] buffer = new byte[65535];

                int bytesRead;
                InputStream inputStream = socket.getInputStream();
                this.os = new BufferedOutputStream( socket.getOutputStream() );

                //Request current status
                if (connected) {
                    send("REQUEST_STATUS");
                }

					/*
					 * notice:
					 * inputStream.read() will block if no data return
					 */
                while ((bytesRead = inputStream.read(buffer)) != -1){
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                    String response = byteArrayOutputStream.toString("UTF-8");
                    try {
                        String[] s = response.split("<EOF>");
                        parseCommands(s[0]);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    byteArrayOutputStream.reset();
                }

            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if (socket != null){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        //textResponse.setText(response);
        super.onPostExecute(result);
        //toast(response);
    }
}