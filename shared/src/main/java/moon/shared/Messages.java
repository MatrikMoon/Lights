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

/**
 * Created by Moon on 6/30/2017.
 * This class handles communication between the watch and the phone
 */

class Messages implements MessageApi.MessageListener {

    private GoogleApiClient mGoogleApiClient;
    private MyClientTask m;

    private boolean connecting = false;

    private static final String API_PATH = "/moon_lights";

    Messages(MyClientTask m) {
        this.m = m;
    }

    void connect(Context c) {
        if (!connecting) {
            connecting = true;
            mGoogleApiClient = new GoogleApiClient.Builder(c)
                    .addApi(Wearable.API)
                    .build();
            mGoogleApiClient.connect();
            Wearable.MessageApi.addListener(mGoogleApiClient, this);
        }
    }

    boolean isConnected() {
        return mGoogleApiClient.isConnected();
    }

    void send(final String s) {
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
                        }
                    });
                }
            }
        }).start();
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }
        return results;
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(API_PATH)) {
            //If we're the phone, we're receiving data from the watch. Send it to the server!
            if (m.getType().equals("PHONE")) {
                m.send(new String(messageEvent.getData()));
            }

            //If we're the watch, we're receiving data from the phone. Parse it!
            else if (m.getType().equals("WEARABLE")) {
                m.parseCommands(new String(messageEvent.getData()));
            }
        }
    }
}
