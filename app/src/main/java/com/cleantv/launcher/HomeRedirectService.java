package com.cleantv.launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;

/**
 * Google TV always sends the Home button and the boot to its own launcher, whatever the
 * default home is. Disabling that launcher crashed the system, so instead it stays enabled
 * as a safety net and this service replaces its home screen with Casa Ciompi as soon as
 * it appears. Only the home screen is replaced: Google's search and voice results still work.
 */
public class HomeRedirectService extends AccessibilityService {

    private static final String GOOGLE_HOME_PACKAGE = "com.google.android.apps.tv.launcherx";
    private static final String GOOGLE_HOME_ACTIVITY = GOOGLE_HOME_PACKAGE + ".home.HomeActivity";

    /** If Casa Ciompi keeps failing to stay up, give up and leave Google's home usable. */
    private static final int MAX_REDIRECTS = 3;
    private static final long REDIRECT_WINDOW_MS = 15_000;
    private static final long BOOT_GRACE_MS = 3 * 60_000;

    private long windowStart;
    private int redirectsInWindow;

    @Override
    protected void onServiceConnected() {
        // At boot Google's home may already be on screen before we are connected.
        if (SystemClock.elapsedRealtime() < BOOT_GRACE_MS) {
            redirect();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        if (GOOGLE_HOME_PACKAGE.contentEquals(nonNull(event.getPackageName()))
                && GOOGLE_HOME_ACTIVITY.contentEquals(nonNull(event.getClassName()))) {
            redirect();
        }
    }

    private void redirect() {
        long now = SystemClock.elapsedRealtime();
        if (now - windowStart > REDIRECT_WINDOW_MS) {
            windowStart = now;
            redirectsInWindow = 0;
        }
        if (++redirectsInWindow > MAX_REDIRECTS) {
            return;
        }
        Intent home = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try {
            startActivity(home);
        } catch (RuntimeException e) {
            // Never let a failed launch take the service down: Google's home stays as fallback.
        }
    }

    private static CharSequence nonNull(CharSequence text) {
        return text != null ? text : "";
    }

    @Override
    public void onInterrupt() {
    }
}
