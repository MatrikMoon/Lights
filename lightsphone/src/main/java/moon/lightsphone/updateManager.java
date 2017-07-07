package moon.lightsphone;

import android.content.Context;

import java.io.File;

/**
 * Created by Moon on 7/6/2017.
 * Small class to handle the monstrous updateActivity class in an easier manner.
 * FIXME: This is just a mask of the bigger updateActivity problem
 */

class updateManager {
    static int currentVersion = 3;

    private static boolean isUpToDate() {
        String availText = moonNetworking.downloadText_MOON("https://pastebin.com/raw/YYdQrj29");
        int availInt = 0;
        try {
            availInt = Integer.parseInt(availText);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return (availInt <= currentVersion);
    }

    static void updateIfNotUpToDate(Context c) {
        if (!isUpToDate()) {
            updateActivity.updateApp(c);
        }
    }

    public static void InstallAPK(String filename){
        File file = new File(filename);
        if(file.exists()){
            try {
                String command;
                command = "adb install -r " + filename;
                Process proc = Runtime.getRuntime().exec(new String[] { "su", "-c", command });
                proc.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
