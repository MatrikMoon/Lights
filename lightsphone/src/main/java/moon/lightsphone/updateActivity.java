package moon.lightsphone;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.AlertDialog;
import android.widget.Toast;

/**
 * Created by moon on 3/15/2016.
 * FIXME: This is a really ugly thing. Needs fixing
 */
class updateActivity extends AsyncTask<Context, Void, String> {

    static void updateApp(Context cont) {
        new updateActivity().execute(cont);
    }

    private class updateDownloader extends AsyncTask<Context, Integer, String> {

        Context mContext = null;
        private PowerManager.WakeLock mWakeLock;

        // declare the dialog as a member field of your activity
        ProgressDialog mProgressDialog;

        @Override
        protected String doInBackground(Context... params) {
            File file = new File(params[0].getFilesDir(), "temp.apk");

            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                String downloadLocation = moonNetworking.downloadText_MOON("https://pastebin.com/raw/L4y1nLzF");

                URL url = new URL(downloadLocation);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(file);


                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            file.setReadable(true, false); //ensure package installer has access to apk
            params[0].startActivity(intent);

            return null;
        }


        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setMessage("Downloading");
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(true);

            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result != null)
                Toast.makeText(mContext, "Download error: " + result, Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(values[0]);
        }
    }

    static Handler handler = new Handler(Looper.getMainLooper());

    public void alertUpToDate(String currVer, Context param) {
        final Context cont = param;
        final String current = currVer;
        handler.post(new Runnable() {
            public void run() {
                new AlertDialog.Builder(cont)
                        .setTitle("Up to date!")
                        .setMessage("You're already up to date! (Version " + current + ")")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // continue with delete
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();
            }
        });
    }

    public void alertUpdateNeeded(String currVer, String availVer, Context param) {
        final Context cont = param;
        final String current = currVer;
        final String available = availVer;
        handler.post(new Runnable() {
            public void run() {
                new AlertDialog.Builder(cont)
                        .setTitle("Update")
                        .setMessage("You have version " + current + ", would you like to update to version " + available + "?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                updateDownloader ud = new updateDownloader();
                                ud.mContext = cont;
                                ud.execute(cont);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
    }

    public void alertMoonMessage(String currVer, String availVer, Context param) {
        final Context cont = param;
        final String current = currVer;
        final String available = availVer;
        handler.post(new Runnable() {
            public void run() {
                new AlertDialog.Builder(cont)
                        .setTitle("!!!")
                        .setMessage("Looks like you've got a version more up-to-date than the update server. If you're not Moon, send him a message! He's done something stupid! (Tell him you have version " + current + ", and the server has " + available + ")")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // continue
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
    }

    public void alertNoConnection(Context param) {
        final Context cont = param;
        handler.post(new Runnable() {
            public void run() {
                new AlertDialog.Builder(cont)
                        .setTitle("No connection to server")
                        .setMessage("Can't connect to the server. Don't worry, it's not a big deal. Moon moves the server a lot, and it probably spends more time offline than it does online. You're welcome to try again later.")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //continue
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();
            }
        });
    }

    public void alertError(Context param, String msg) {
        final Context cont = param;
        final String text = msg;
        handler.post(new Runnable() {
            public void run() {
                new AlertDialog.Builder(cont)
                        .setTitle("Error (Are you being blocked by a filter or proxy?)")
                        .setMessage(text)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //continue
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();
            }
        });
    }


    @Override
    protected String doInBackground(Context... params) {
        int currVer = updateManager.currentVersion;

        String availText = moonNetworking.downloadText_MOON("https://pastebin.com/raw/YYdQrj29");

        if (availText.isEmpty()) {
            alertNoConnection(params[0]);
            return "failed";
        }

        int availVer = 0;
        try {
            availVer = Integer.parseInt(availText);
        } catch (Exception ex) {
            alertError(params[0], ex.toString());
            return "failed";
        }
        if (currVer == availVer) {
            alertUpToDate(Integer.toString(currVer), params[0]);
        } else if (currVer < availVer) {
            alertUpdateNeeded(Integer.toString(currVer), Integer.toString(availVer), params[0]);
        } else if (currVer > availVer) {
            alertMoonMessage(Integer.toString(currVer), Integer.toString(availVer), params[0]);
        }

        return "Executed";
    }
}
