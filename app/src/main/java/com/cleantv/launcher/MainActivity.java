package com.cleantv.launcher;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.GridLayoutAnimationController;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Home screen: a plain grid of installed apps, with no rows, ads or recommendations. */
public class MainActivity extends Activity {

    private static final String PREFS = "launcher";
    private static final String KEY_HIDDEN = "hidden_packages";
    private static final float FOCUS_SCALE = 1.1f;
    private static final float IDLE_ALPHA = 0.78f;
    private static final long FOCUS_ANIM_MS = 260;

    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Interpolator soft = new DecelerateInterpolator(2f);

    private GridView grid;
    private TextView greeting;
    private ObjectAnimator breathing;
    private AppAdapter adapter;
    private SharedPreferences prefs;
    private int selectedPosition = 0;
    private String settingsPackage;
    private boolean firstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        TextView title = findViewById(R.id.title);
        title.setFontVariationSettings("'wght' 700");
        greeting = findViewById(R.id.greeting);

        // The light in the room slowly breathes, like embers in a fireplace.
        View glow = findViewById(R.id.glow);
        glow.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        breathing = ObjectAnimator.ofFloat(glow, View.ALPHA, 0.55f, 1f);
        breathing.setDuration(7000);
        breathing.setRepeatCount(ValueAnimator.INFINITE);
        breathing.setRepeatMode(ValueAnimator.REVERSE);
        breathing.setInterpolator(new AccelerateDecelerateInterpolator());
        breathing.start();

        View header = findViewById(R.id.header);
        header.setAlpha(0f);
        header.setTranslationY(-dp(16));
        header.animate().alpha(1f).translationY(0f).setDuration(700).setInterpolator(soft).start();

        grid = findViewById(R.id.grid);
        adapter = new AppAdapter();
        grid.setAdapter(adapter);

        grid.setOnItemClickListener((parent, view, position, id) -> launch(adapter.getItem(position)));
        grid.setOnItemLongClickListener((parent, view, position, id) -> {
            onLongPress(adapter.getItem(position));
            return true;
        });
        grid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPosition = position;
                refreshHighlight();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Fired while the grid is still empty: keep the last position for after loading.
                refreshHighlight();
            }
        });
        grid.setOnFocusChangeListener((v, hasFocus) -> refreshHighlight());
        grid.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        greeting.setText(greetingForNow());
        breathing.resume();
        reload();
    }

    @Override
    protected void onPause() {
        breathing.pause();
        super.onPause();
    }

    private int greetingForNow() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) return R.string.greeting_morning;
        if (hour >= 12 && hour < 18) return R.string.greeting_afternoon;
        if (hour >= 18 && hour < 23) return R.string.greeting_evening;
        return R.string.greeting_night;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDestroy() {
        loader.shutdownNow();
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        // We are the home screen: there is nothing to go back to.
    }

    private void reload() {
        Set<String> hidden = new HashSet<>(prefs.getStringSet(KEY_HIDDEN, new HashSet<>()));
        loader.execute(() -> {
            List<AppEntry> apps = loadApps(hidden);
            ui.post(() -> {
                if (firstLoad) {
                    // Tiles drift up into place one after another, only when the home first opens.
                    firstLoad = false;
                    AnimationSet rise = new AnimationSet(true);
                    rise.addAnimation(new AlphaAnimation(0f, 1f));
                    rise.addAnimation(new TranslateAnimation(0, 0, dp(28), 0));
                    rise.setDuration(520);
                    rise.setInterpolator(soft);
                    grid.setLayoutAnimation(new GridLayoutAnimationController(rise, 0.12f, 0.18f));
                    grid.setLayoutAnimationListener(new Animation.AnimationListener() {
                        @Override public void onAnimationStart(Animation a) { }
                        @Override public void onAnimationRepeat(Animation a) { }
                        @Override public void onAnimationEnd(Animation a) {
                            grid.setLayoutAnimation(null);
                        }
                    });
                    grid.scheduleLayoutAnimation();
                }
                adapter.setItems(apps);
                selectedPosition = Math.max(0, Math.min(selectedPosition, apps.size() - 1));
                grid.setSelection(selectedPosition);
                refreshHighlight();
            });
        });
    }

    private List<AppEntry> loadApps(Set<String> hidden) {
        PackageManager pm = getPackageManager();
        ResolveInfo settingsInfo = pm.resolveActivity(new Intent(Settings.ACTION_SETTINGS), 0);
        settingsPackage = settingsInfo != null ? settingsInfo.activityInfo.packageName : null;
        Map<String, AppEntry> byPackage = new LinkedHashMap<>();
        // TV apps first so their wide banner wins over the phone-style icon.
        collect(pm, Intent.CATEGORY_LEANBACK_LAUNCHER, true, hidden, byPackage);
        collect(pm, Intent.CATEGORY_LAUNCHER, false, hidden, byPackage);

        List<AppEntry> apps = new ArrayList<>(byPackage.values());
        Collator collator = Collator.getInstance();
        apps.sort((a, b) -> collator.compare(a.label, b.label));

        // Most TVs already list their Settings app; add our own tile only when it is missing.
        if (settingsPackage == null || !byPackage.containsKey(settingsPackage)) {
            Intent settings = new Intent(Settings.ACTION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            apps.add(new AppEntry(null, getString(R.string.settings),
                    getDrawable(android.R.drawable.ic_menu_preferences), false, settings));
        }
        return apps;
    }

    /** The Settings tile can't be hidden; long-pressing it offers to show hidden apps again. */
    private boolean isSettings(AppEntry app) {
        return app.packageName == null || app.packageName.equals(settingsPackage);
    }

    private void collect(PackageManager pm, String category, boolean useBanner,
                         Set<String> hidden, Map<String, AppEntry> out) {
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
        for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
            ActivityInfo activity = info.activityInfo;
            String pkg = activity.packageName;
            if (pkg.equals(getPackageName()) || hidden.contains(pkg) || out.containsKey(pkg)) {
                continue;
            }
            Drawable banner = useBanner ? activity.loadBanner(pm) : null;
            Drawable image = banner != null ? banner : activity.loadIcon(pm);
            Intent launch = new Intent(Intent.ACTION_MAIN)
                    .addCategory(category)
                    .setComponent(new ComponentName(pkg, activity.name))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            out.put(pkg, new AppEntry(pkg, info.loadLabel(pm).toString(), image, banner != null, launch));
        }
    }

    private void launch(AppEntry app) {
        try {
            startActivity(app.launchIntent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, getString(R.string.launch_failed, app.label), Toast.LENGTH_SHORT).show();
        }
    }

    private void onLongPress(AppEntry app) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this,
                android.R.style.Theme_DeviceDefault_Dialog_Alert);
        if (isSettings(app)) {
            dialog.setTitle(R.string.unhide_title)
                    .setPositiveButton(R.string.unhide, (d, w) -> {
                        prefs.edit().remove(KEY_HIDDEN).apply();
                        reload();
                    });
        } else {
            dialog.setTitle(getString(R.string.hide_title, app.label))
                    .setMessage(R.string.hide_message)
                    .setPositiveButton(R.string.hide, (d, w) -> {
                        Set<String> hidden = new HashSet<>(prefs.getStringSet(KEY_HIDDEN, new HashSet<>()));
                        hidden.add(app.packageName);
                        prefs.edit().putStringSet(KEY_HIDDEN, hidden).apply();
                        reload();
                    });
        }
        dialog.setNegativeButton(R.string.cancel, null).show();
    }

    private void refreshHighlight() {
        int first = grid.getFirstVisiblePosition();
        boolean focused = grid.hasFocus();
        for (int i = 0; i < grid.getChildCount(); i++) {
            applyHighlight(grid.getChildAt(i), focused && first + i == selectedPosition, true);
        }
    }

    /** The selected tile lifts towards you and glows amber; the others rest slightly dimmed. */
    private void applyHighlight(View item, boolean on, boolean animate) {
        ImageView image = item.findViewById(R.id.image);
        TextView label = item.findViewById(R.id.label);
        image.setForeground(on ? getDrawable(R.drawable.focus_frame) : null);
        label.setTextColor(getColor(on ? R.color.label : R.color.label_muted));

        float scale = on ? FOCUS_SCALE : 1f;
        float alpha = on ? 1f : IDLE_ALPHA;
        float lift = on ? dp(20) : 0f;
        if (animate) {
            item.animate().scaleX(scale).scaleY(scale).alpha(alpha)
                    .setDuration(FOCUS_ANIM_MS).setInterpolator(soft).start();
            image.animate().translationZ(lift).setDuration(FOCUS_ANIM_MS).setInterpolator(soft).start();
        } else {
            item.animate().cancel();
            image.animate().cancel();
            item.setScaleX(scale);
            item.setScaleY(scale);
            item.setAlpha(alpha);
            image.setTranslationZ(lift);
        }
    }

    private static final class AppEntry {
        /** Null for the built-in Settings tile. */
        final String packageName;
        final String label;
        final Drawable image;
        final boolean isBanner;
        final Intent launchIntent;

        AppEntry(String packageName, String label, Drawable image, boolean isBanner, Intent launchIntent) {
            this.packageName = packageName;
            this.label = label;
            this.image = image;
            this.isBanner = isBanner;
            this.launchIntent = launchIntent;
        }
    }

    private final class AppAdapter extends BaseAdapter {
        private List<AppEntry> items = new ArrayList<>();

        void setItems(List<AppEntry> newItems) {
            items = newItems;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public AppEntry getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView != null ? convertView
                    : LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
            AppEntry app = getItem(position);

            ImageView image = view.findViewById(R.id.image);
            image.setImageDrawable(app.image);
            image.setClipToOutline(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Tinted shadow = a warm halo around the lifted tile.
                image.setOutlineSpotShadowColor(getColor(R.color.ember));
                image.setOutlineAmbientShadowColor(getColor(R.color.ember));
            }
            if (app.isBanner) {
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setPadding(0, 0, 0, 0);
            } else {
                int pad = Math.round(dp(18));
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                image.setPadding(pad, pad, pad, pad);
            }
            ((TextView) view.findViewById(R.id.label)).setText(app.label);

            applyHighlight(view, grid.hasFocus() && position == selectedPosition, false);
            return view;
        }
    }
}
