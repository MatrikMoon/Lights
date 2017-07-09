package moon.lightsphone;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * Created by moon on 7/8/2017
 * Handles widget actions. Still don't fully understand why my previous attempt at a broadcast
 * receiver failed.
 */
public class ButtonWidget extends AppWidgetProvider {
    private static final String BUTTON_CLICKED = "moon.lightsphone.ButtonWidget.BUTTON_CLICKED";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        RemoteViews remoteViews;
        ComponentName watchWidget;

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.button_widget);
        watchWidget = new ComponentName(context, ButtonWidget.class);

        remoteViews.setOnClickPendingIntent(R.id.widgetButton, getPendingSelfIntent(context, BUTTON_CLICKED));
        appWidgetManager.updateAppWidget(watchWidget, remoteViews);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (BUTTON_CLICKED.equals(intent.getAction())) {
            watchService.widgetToggle();
        }
    }

    //Set state if widget exists
    public static void setState(String state, Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        RemoteViews remoteViews;
        ComponentName watchWidget;

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.button_widget);
        watchWidget = new ComponentName(context, ButtonWidget.class);

        remoteViews.setTextViewText(R.id.widgetButton, state);

        appWidgetManager.updateAppWidget(watchWidget, remoteViews);
    }

    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }
}