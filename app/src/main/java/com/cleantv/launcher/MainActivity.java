package com.cleantv.launcher;

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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
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
    private static final float FOCUS_SCALE = 1.08f;

    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    private GridView grid;
    private AppAdapter adapter;
    private SharedPreferences prefs;
    private int selectedPosition = 0;
    private String settingsPackage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

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
        reload();
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
            applyHighlight(grid.getChildAt(i), focused && first + i == selectedPosition);
        }
    }

    private void applyHighlight(View item, boolean on) {
        ImageView image = item.findViewById(R.id.image);
        image.setForeground(on ? getDrawable(R.drawable.focus_frame) : null);
        float scale = on ? FOCUS_SCALE : 1f;
        item.animate().scaleX(scale).scaleY(scale).setDuration(120).start();
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
            if (app.isBanner) {
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setPadding(0, 0, 0, 0);
            } else {
                int pad = Math.round(18 * getResources().getDisplayMetrics().density);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                image.setPadding(pad, pad, pad, pad);
            }
            ((TextView) view.findViewById(R.id.label)).setText(app.label);

            boolean on = grid.hasFocus() && position == selectedPosition;
            image.setForeground(on ? getDrawable(R.drawable.focus_frame) : null);
            view.setScaleX(on ? FOCUS_SCALE : 1f);
            view.setScaleY(on ? FOCUS_SCALE : 1f);
            return view;
        }
    }
}
