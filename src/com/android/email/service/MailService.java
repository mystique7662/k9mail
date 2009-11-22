
package com.android.email.service;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Config;
import android.util.Log;

import com.android.email.Account;
import com.android.email.Email;
import com.android.email.MessagingController;
import com.android.email.MessagingListener;
import com.android.email.Preferences;
import com.android.email.R;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Pusher;

/**
 */
public class MailService extends CoreService {
    private static final String ACTION_CHECK_MAIL = "com.android.email.intent.action.MAIL_SERVICE_WAKEUP";
    private static final String ACTION_RESCHEDULE = "com.android.email.intent.action.MAIL_SERVICE_RESCHEDULE";
    private static final String ACTION_RESCHEDULE_CHECK = "com.android.email.intent.action.MAIL_SERVICE_RESCHEDULE_CHECK";
    private static final String ACTION_CANCEL = "com.android.email.intent.action.MAIL_SERVICE_CANCEL";
    private static final String ACTION_REFRESH_PUSHERS = "com.android.email.intent.action.MAIL_SERVICE_REFRESH_PUSHERS";
    private static final String CONNECTIVITY_CHANGE = "com.android.email.intent.action.MAIL_SERVICE_CONNECTIVITY_CHANGE";
    private static final String BACKGROUND_DATA_CHANGED = "com.android.email.intent.action.MAIL_SERVICE_BACKGROUND_DATA_CHANGED";
    private static final String CANCEL_CONNECTIVITY_NOTICE = "com.android.email.intent.action.MAIL_SERVICE_CANCEL_CONNECTIVITY_NOTICE";
    
    private static final String HAS_CONNECTIVITY = "com.android.email.intent.action.MAIL_SERVICE_HAS_CONNECTIVITY";
    
    private final ExecutorService threadPool = Executors.newFixedThreadPool(1);  // Must be single threaded
    
   
    public static void actionReschedule(Context context, Integer wakeLockId) {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.ACTION_RESCHEDULE);
        addWakeLockId(i, wakeLockId);
        context.startService(i);
    }
    
    public static void rescheduleCheck(Context context, Integer wakeLockId) {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.ACTION_RESCHEDULE_CHECK);
        addWakeLockId(i, wakeLockId);
        context.startService(i);
    }
    
    public static void actionCancel(Context context, Integer wakeLockId)  {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.ACTION_CANCEL);
        addWakeLockId(i, wakeLockId);
        context.startService(i);
    }
    
    public static void connectivityChange(Context context, boolean hasConnectivity, Integer wakeLockId)  {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.CONNECTIVITY_CHANGE);
        i.putExtra(HAS_CONNECTIVITY, hasConnectivity);
        addWakeLockId(i, wakeLockId);
        context.startService(i);
    }
    
    public static void backgroundDataChanged(Context context, Integer wakeLockId)  {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.BACKGROUND_DATA_CHANGED);
        addWakeLockId(i, wakeLockId);
        context.startService(i);
    }

    @Override
    public void onCreate() {
    	super.onCreate();
    	Log.v(Email.LOG_TAG, "***** MailService *****: onCreate");
    }

    @Override
    public void startService(Intent intent, int startId) {
        Integer startIdObj = startId;
        long startTime = System.currentTimeMillis();
        try
        {
            ConnectivityManager connectivityManager = (ConnectivityManager)getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
            boolean doBackground = true;
            boolean hasConnectivity = false;
            
            if (connectivityManager != null)
            {
                NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
                if (netInfo != null)
                {
                    State state = netInfo.getState();
                    hasConnectivity = state == State.CONNECTED;
                }
                boolean backgroundData = connectivityManager.getBackgroundDataSetting();
                
                Email.BACKGROUND_OPS bOps = Email.getBackgroundOps();
                doBackground = (backgroundData == true && bOps != Email.BACKGROUND_OPS.NEVER) 
                    | (backgroundData == false && bOps == Email.BACKGROUND_OPS.ALWAYS);
            
            }
            
        	setForeground(true);  // if it gets killed once, it'll never restart
        		Log.i(Email.LOG_TAG, "MailService.onStart(" + intent + ", " + startId 
        		        + "), hasConnectivity = " + hasConnectivity + ", doBackground = " + doBackground);
    
           // MessagingController.getInstance(getApplication()).addListener(mListener);
            if (ACTION_CHECK_MAIL.equals(intent.getAction())) {
               Log.i(Email.LOG_TAG, "***** MailService *****: checking mail");
                    
                if (hasConnectivity && doBackground)
                {
                    PollService.startService(this);
                }
    
                reschedule(startIdObj);
                startIdObj = null;
            }
            else if (ACTION_CANCEL.equals(intent.getAction())) {
                if (Config.LOGV) {
                    Log.v(Email.LOG_TAG, "***** MailService *****: cancel");
                }
                MessagingController.getInstance(getApplication()).log("***** MailService *****: cancel");
    
                cancel();
            }
            else if (ACTION_RESCHEDULE.equals(intent.getAction())) {
                if (Config.LOGV) {
                    Log.v(Email.LOG_TAG, "***** MailService *****: reschedule");
                }
                rescheduleAll(hasConnectivity, doBackground, startIdObj);
                startIdObj = null;
                MessagingController.getInstance(getApplication()).log("***** MailService *****: reschedule");
                
            }
            else if (ACTION_RESCHEDULE_CHECK.equals(intent.getAction())) {
                if (Config.LOGV) {
                    Log.v(Email.LOG_TAG, "***** MailService *****: reschedule check");
                }
                reschedule(startIdObj);
                startIdObj = null;
                MessagingController.getInstance(getApplication()).log("***** MailService *****: reschedule");
                
            }
            else if (ACTION_REFRESH_PUSHERS.equals(intent.getAction()))
            {
                if (hasConnectivity && doBackground)
                {
                    schedulePushers(null);
                    refreshPushers(startIdObj);
                    startIdObj = null;
                }
            }
            else if (CONNECTIVITY_CHANGE.equals(intent.getAction()) ||
                    BACKGROUND_DATA_CHANGED.equals(intent.getAction()))
            {
                rescheduleAll(hasConnectivity, doBackground, startIdObj);
                startIdObj = null;
                Log.i(Email.LOG_TAG, "Got connectivity action with hasConnectivity = " + hasConnectivity + ", doBackground = " + doBackground);
                
                notifyConnectionStatus(hasConnectivity);
            }
            else if (CANCEL_CONNECTIVITY_NOTICE.equals(intent.getAction()))
            {
                notifyConnectionStatus(true);
            }
        }
        finally
        {
            if (startIdObj != null)
            {
                stopSelf(startId);
            }
        }
        long endTime = System.currentTimeMillis();
        Log.i(Email.LOG_TAG, "MailService.onStart took " + (endTime - startTime) + "ms");
    }
    
    private void rescheduleAll(final boolean hasConnectivity, final boolean doBackground, final Integer startId)
    {
        if (hasConnectivity && doBackground)
        {
                reschedule(null);
                reschedulePushers(startId);
        }
        else
        {
            stopPushers(startId);
        }
    }

    private void notifyConnectionStatus(boolean hasConnectivity)
    {
        NotificationManager notifMgr =
            (NotificationManager)getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
        if (hasConnectivity == false)
        {
            String notice = getApplication().getString(R.string.no_connection_alert);
            String header = getApplication().getString(R.string.alert_header);
            
            
            Notification notif = new Notification(R.drawable.stat_notify_email_generic,
                    header, System.currentTimeMillis());
            
            Intent i = new Intent();
            i.setClassName(getApplication().getPackageName(), "com.android.email.service.MailService");
            i.setAction(MailService.CANCEL_CONNECTIVITY_NOTICE);
    
            PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
    
            notif.setLatestEventInfo(getApplication(), header, notice, pi);
            notif.flags = Notification.FLAG_ONGOING_EVENT;
    
            notifMgr.notify(Email.CONNECTIVITY_ID, notif); 
        }
        else
        {
            notifMgr.cancel(Email.CONNECTIVITY_ID);
        }
    }

    @Override
    public void onDestroy() {
    		Log.v(Email.LOG_TAG, "***** MailService *****: onDestroy()");
        super.onDestroy();
   //     MessagingController.getInstance(getApplication()).removeListener(mListener);
    }

    private void cancel() {
        Intent i = new Intent();
        i.setClassName(getApplication().getPackageName(), "com.android.email.service.MailService");
        i.setAction(ACTION_CHECK_MAIL);
        BootReceiver.cancelIntent(this, i);
    }

    private void reschedule(Integer startId) {
        execute(getApplication(), new Runnable() 
        {
            public void run()
            {
                int shortestInterval = -1;
                
                for (Account account : Preferences.getPreferences(MailService.this).getAccounts()) {
                    if (account.getAutomaticCheckIntervalMinutes() != -1
                            && (account.getAutomaticCheckIntervalMinutes() < shortestInterval || shortestInterval == -1)) {
                        shortestInterval = account.getAutomaticCheckIntervalMinutes();
                    }
                }
        
                if (shortestInterval == -1) {
                		Log.v(Email.LOG_TAG, "No next check scheduled for package " + getApplication().getPackageName());
                		cancel();
                }
                else
                {
        	        long delay = (shortestInterval * (60 * 1000));
        
        	        long nextTime = System.currentTimeMillis() + delay;
        	        try
        	        {
        	          String checkString = "Next check for package " + getApplication().getPackageName() + " scheduled for " + new Date(nextTime);
        	          Log.i(Email.LOG_TAG, checkString);
        	          MessagingController.getInstance(getApplication()).log(checkString);
        	        }
        	        catch (Exception e)
        	        {
        	          // I once got a NullPointerException deep in new Date();
        	          Log.e(Email.LOG_TAG, "Exception while logging", e);
        	        }
        
        	        Intent i = new Intent();
        	        i.setClassName(getApplication().getPackageName(), "com.android.email.service.MailService");
        	        i.setAction(ACTION_CHECK_MAIL);
                    BootReceiver.scheduleIntent(MailService.this, nextTime, i);
        	        
                }
            }
        }, Email.MAIL_SERVICE_WAKE_LOCK_TIMEOUT, startId);
    }
    
    private void stopPushers(final Integer startId)
    {
        execute(getApplication(), new Runnable() 
        {
            public void run()
            {
                MessagingController.getInstance(getApplication()).stopAllPushing();
                PushService.stopService(MailService.this);
            }
        }, Email.MAIL_SERVICE_WAKE_LOCK_TIMEOUT, startId );
    }
    
    private void reschedulePushers(final Integer startId)
    {
        execute(getApplication(), new Runnable() 
        {
            public void run()
            {
                
                Log.i(Email.LOG_TAG, "Rescheduling pushers");
                stopPushers(null);
                setupPushers(null);
                schedulePushers(startId);
                
            }
        }, Email.MAIL_SERVICE_WAKE_LOCK_TIMEOUT, null );
    }
    
    private void setupPushers(final Integer startId)
    {
        execute(getApplication(), new Runnable() 
        {
            public void run()
            {
                boolean pushing = false;
                for (Account account : Preferences.getPreferences(MailService.this).getAccounts()) {
                    Log.i(Email.LOG_TAG, "Setting up pushers for account " + account.getDescription());
                    Pusher pusher = MessagingController.getInstance(getApplication()).setupPushing(account);
                    if (pusher != null)
                    {
                        pushing = true;
                        Log.i(Email.LOG_TAG, "Starting configured pusher for account " + account.getDescription());
                        pusher.start();
                    }
                } 
                if (pushing)
                {
                    PushService.startService(MailService.this);
                }
            }
        }, Email.MAIL_SERVICE_WAKE_LOCK_TIMEOUT, startId);
    }
    
    private void refreshPushers(final Integer startId)
    {
        execute(getApplication(), new Runnable() 
        {
            public void run()
            {
                try
                {
                    Log.i(Email.LOG_TAG, "Refreshing pushers");
                    Collection<Pusher> pushers = MessagingController.getInstance(getApplication()).getPushers();
                    for (Pusher pusher : pushers)
                    {
                        pusher.refresh();
                    }
                }
                catch (Exception e)
                {
                    Log.e(Email.LOG_TAG, "Exception while refreshing pushers", e);
                }
            }
        }, Email.MAIL_SERVICE_WAKE_LOCK_TIMEOUT, startId );
    }
    
    private void schedulePushers(final Integer startId)
    {
        execute(getApplication(), new Runnable() 
        {
            public void run()
            {
                int minInterval = -1;
        
                Collection<Pusher> pushers = MessagingController.getInstance(getApplication()).getPushers();
                for (Pusher pusher : pushers)
                {
                    int interval = pusher.getRefreshInterval();
                    if (interval != -1 && (interval < minInterval || minInterval == -1))
                    {
                        minInterval = interval;
                    }
                }
                if (Email.DEBUG)
                {
                    Log.v(Email.LOG_TAG, "Pusher refresh interval = " + minInterval);
                }
                if (minInterval != -1)
                {
                    long nextTime = System.currentTimeMillis() + minInterval;
                    String checkString = "Next pusher refresh scheduled for " + new Date(nextTime);
                    if (Email.DEBUG)
                    {
                        Log.d(Email.LOG_TAG, checkString);
                    }
                    Intent i = new Intent();
                    i.setClassName(getApplication().getPackageName(), "com.android.email.service.MailService");
                    i.setAction(ACTION_REFRESH_PUSHERS);
                    BootReceiver.scheduleIntent(MailService.this, nextTime, i);
                }}
        }, Email.MAIL_SERVICE_WAKE_LOCK_TIMEOUT, startId);
    }
    
    public void execute(Context context, final Runnable runner, int wakeLockTime, final Integer startId)
    {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        final WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Email");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(wakeLockTime);
        Log.i(Email.LOG_TAG, "MailService queueing Runnable " + runner.hashCode() + " with startId " + startId);
        Runnable myRunner = new Runnable()
        {
            public void run()
            {
                try
                {

                    Log.i(Email.LOG_TAG, "MailService running Runnable " + runner.hashCode() + " with startId " + startId);
                    runner.run();
                }
                finally
                {
                    Log.i(Email.LOG_TAG, "MailService completed Runnable " + runner.hashCode() + " with startId " + startId);
                    wakeLock.release();
                    if (startId != null)
                    {
                        stopSelf(startId);
                    }
                }
            }
    
        };

        threadPool.execute(myRunner);
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

  
}
