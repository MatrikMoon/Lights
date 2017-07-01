package moon.shared;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Moon on 6/30/2017.
 * This class handles communication between the watch and the phone
 */

public class Messages implements MessageApi.MessageListener {

    private String nodeId = "";
    private GoogleApiClient mGoogleApiClient;
    private MyClientTask m;

    private static final String API_PATH = "/moon_lights";

    public Messages(MyClientTask m) {
        this.m = m;
    }

    public void connect(Context c) {
        mGoogleApiClient = new GoogleApiClient.Builder(c)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
        Log.i("LISTENER", "ADDED");
        Thread.dumpStack();
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    /*

    public boolean isConnected() {
        return mGoogleApiClient.isConnected();
    }

    public void send(final String s) {
        Log.i("MESSAGEAPI", "SENDING: " + s);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (final String node : getNodes()) {
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, node, API_PATH, s.getBytes()).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                // Failed to send message
                                Log.i("FAILED", "RESULT");
                            }
                            else if (sendMessageResult.getStatus().isSuccess()) {
                                Log.i("SUCCESS", node);
                            }
                        }
                    });
                }
            }
        }).start();

    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }
        return results;
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i("MessageReceived", messageEvent.getPath());
        if (messageEvent.getPath().equals(API_PATH)) {
            Log.i("onReceived", new String(messageEvent.getData()));

            if (m.getType().equals("PHONE")) {

            }
            else if (m.getType().equals("WEARABLE")) {
                m.parseCommands(new String(messageEvent.getData()));
            }
        }
    }
    */
}
