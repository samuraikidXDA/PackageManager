/*
 * Copyright (C) 2020-2021 sunilpaulmathew <sunil.kde@gmail.com>
 *
 * This file is part of Package Manager, a simple, yet powerful application
 * to manage other application installed on an android device.
 *
 */

package com.smartpack.packagemanager.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatEditText;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.smartpack.packagemanager.BuildConfig;
import com.smartpack.packagemanager.MainActivity;
import com.smartpack.packagemanager.R;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/*
 * Created by sunilpaulmathew <sunil.kde@gmail.com> on October 07, 2020
 */

public class Utils {

    static {
        Shell.Config.verboseLogging(BuildConfig.DEBUG);
        Shell.Config.setTimeout(10);
    }

    public static AppCompatEditText mSearchWord;
    private static boolean mWelcomeDialog = true;
    public static boolean mReloadPage = true;
    public static boolean mSortByOEM = false;
    public static boolean mSystemApp = true;
    public static CharSequence mApplicationName;
    public static Drawable mApplicationIcon;
    public static String mApplicationID;
    public static String mDirData;
    public static String mDirNatLib;
    public static String mDirSource;
    public static String mSearchText;

    /*
     * The following code is partly taken from https://github.com/SmartPack/SmartPack-Kernel-Manager
     * Ref: https://github.com/SmartPack/SmartPack-Kernel-Manager/blob/beta/app/src/main/java/com/smartpack/kernelmanager/utils/root/RootUtils.java
     */
    public static boolean rootAccess() {
        return Shell.rootAccess();
    }

    public static void runCommand(String command) {
        Shell.su(command).exec();
    }

    @NonNull
    static String runAndGetOutput(String command) {
        StringBuilder sb = new StringBuilder();
        try {
            List<String> outputs = Shell.su(command).exec().getOut();
            if (ShellUtils.isValidOutput(outputs)) {
                for (String output : outputs) {
                    sb.append(output).append("\n");
                }
            }
            return removeSuffix(sb.toString()).trim();
        } catch (Exception e) {
            return "";
        }
    }

    @NonNull
    public static String runAndGetError(String command) {
        StringBuilder sb = new StringBuilder();
        List<String> outputs = new ArrayList<>();
        List<String> stderr = new ArrayList<>();
        try {
            Shell.su(command).to(outputs, stderr).exec();
            outputs.addAll(stderr);
            if (ShellUtils.isValidOutput(outputs)) {
                for (String output : outputs) {
                    sb.append(output).append("\n");
                }
            }
            return removeSuffix(sb.toString()).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String removeSuffix(@Nullable String s) {
        if (s != null && s.endsWith("\n")) {
            return s.substring(0, s.length() - "\n".length());
        }
        return s;
    }

    /*
     * The following code is partly taken from https://github.com/Grarak/KernelAdiutor
     * Ref: https://github.com/Grarak/KernelAdiutor/blob/master/app/src/main/java/com/grarak/kerneladiutor/utils/ViewUtils.java
     */

    public interface OnDialogEditTextListener {
        void onClick(String text);
    }

    public static MaterialAlertDialogBuilder dialogEditText(String text, final DialogInterface.OnClickListener negativeListener,
                                                            final OnDialogEditTextListener onDialogEditTextListener,
                                                            Context context) {
        return dialogEditText(text, negativeListener, onDialogEditTextListener, -1, context);
    }

    public static MaterialAlertDialogBuilder dialogEditText(String text, final DialogInterface.OnClickListener negativeListener,
                                        final OnDialogEditTextListener onDialogEditTextListener, int inputType,
                                        Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setPadding(75, 75, 75, 75);

        final AppCompatEditText editText = new AppCompatEditText(context);
        editText.setGravity(Gravity.CENTER);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (text != null) {
            editText.append(text);
        }
        editText.setSingleLine(true);
        if (inputType >= 0) {
            editText.setInputType(inputType);
        }

        layout.addView(editText);

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context).setView(layout);
        if (negativeListener != null) {
            dialog.setNegativeButton(context.getString(R.string.cancel), negativeListener);
        }
        if (onDialogEditTextListener != null) {
            dialog.setPositiveButton(context.getString(R.string.ok), (dialog1, which)
                    -> onDialogEditTextListener.onClick(Objects.requireNonNull(editText.getText()).toString()))
                    .setOnDismissListener(dialog1 -> {
                        if (negativeListener != null) {
                            negativeListener.onClick(dialog1, 0);
                        }
                    });
        }
        return dialog;
    }

    /*
     * The following code is partly taken from https://github.com/Grarak/KernelAdiutor
     * Ref: https://github.com/Grarak/KernelAdiutor/blob/master/app/src/main/java/com/grarak/kerneladiutor/utils/Prefs.java
     */
    public static boolean getBoolean(String name, boolean defaults, Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(name, defaults);
    }

    public static void saveBoolean(String name, boolean value, Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(name, value).apply();
    }

    /*
     * The following code is partly taken from https://github.com/Grarak/KernelAdiutor
     * Ref: https://github.com/Grarak/KernelAdiutor/blob/master/app/src/main/java/com/grarak/kerneladiutor/utils/Utils.java
     */

    public static boolean isPackageInstalled(String packageID, Context context) {
        try {
            context.getPackageManager().getApplicationInfo(packageID, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    public static boolean isNotDonated(Context context) {
        return !isPackageInstalled("com.smartpack.donate", context);
    }

    public static void initializeAppTheme(Context context) {
        if (getBoolean("dark_theme", false, context)) {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES);
        } else if (getBoolean("light_theme", false, context)) {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    public static boolean isDarkTheme(Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    public static void delete(String path) {
        if (existFile(path)) {
            runCommand("rm -r " + path);
        }
    }

    public static void copy(String source, String dest) {
        runCommand("cp -r " + source + " " + dest);
    }

    public static void sleep(int sec) {
        runCommand("sleep " + sec);
    }

    public static void snackbar(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.dismiss, v -> snackbar.dismiss());
        snackbar.show();
    }

    public static CharSequence fromHtml(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(text);
        }
    }

    public static void launchUrl(String url, View view, Context context) {
        if (isNetworkUnavailable(context)) {
            snackbar(view, context.getString(R.string.no_internet));
        } else {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                context.startActivity(i);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean existFile(String file) {
        String output = runAndGetOutput("[ -e " + file + " ] && echo true");
        return !output.isEmpty() && output.equals("true");
    }

    public static boolean isNetworkUnavailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        return (cm.getActiveNetworkInfo() == null) || !cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    public static String getPath(File file) {
        String path = file.getAbsolutePath();
        if (path.startsWith("/document/raw:")) {
            path = path.replace("/document/raw:", "");
        } else if (path.startsWith("/document/primary:")) {
            path = (Environment.getExternalStorageDirectory() + ("/") + path.replace("/document/primary:", ""));
        } else if (path.startsWith("/document/")) {
            path = path.replace("/document/", "/storage/").replace(":", "/");
        }
        if (path.startsWith("/storage_root/storage/emulated/0")) {
            path = path.replace("/storage_root/storage/emulated/0", "/storage/emulated/0");
        } else if (path.startsWith("/storage_root")) {
            path = path.replace("storage_root", "storage/emulated/0");
        }
        if (path.startsWith("/external")) {
            path = path.replace("external", "storage/emulated/0");
        } if (path.startsWith("/root/")) {
            path = path.replace("/root", "");
        }
        if (path.contains("file%3A%2F%2F%2F")) {
            path = path.replace("file%3A%2F%2F%2F", "").replace("%2F", "/");
        }
        return path;
    }

    public static boolean isDocumentsUI(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /*
     * Taken and used almost as such from the following stackoverflow discussion
     * Ref: https://stackoverflow.com/questions/7203668/how-permission-can-be-checked-at-runtime-without-throwing-securityexception
     */
    public static boolean isStorageWritePermissionDenied(Context context) {
        return (context.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED);
    }

    /*
     * The following code is partly taken from https://github.com/morogoku/MTweaks-KernelAdiutorMOD/
     * Ref: https://github.com/morogoku/MTweaks-KernelAdiutorMOD/blob/dd5a4c3242d5e1697d55c4cc6412a9b76c8b8e2e/app/src/main/java/com/moro/mtweaks/fragments/kernel/BoefflaWakelockFragment.java#L133
     */
    public static void WelcomeDialog(Context context) {
        View checkBoxView = View.inflate(context, R.layout.rv_checkbox, null);
        MaterialCheckBox checkBox = checkBoxView.findViewById(R.id.checkbox);
        checkBox.setChecked(true);
        checkBox.setText(context.getString(R.string.always_show));
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mWelcomeDialog = isChecked;
        });

        MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(Objects.requireNonNull(context));
        alert.setIcon(R.mipmap.ic_launcher);
        alert.setTitle(context.getString(R.string.app_name));
        alert.setMessage(context.getText(R.string.welcome_message));
        alert.setView(checkBoxView);
        alert.setCancelable(false);
        alert.setPositiveButton(context.getString(R.string.got_it), (dialog, id) -> {
            saveBoolean("welcomeMessage", mWelcomeDialog, context);
        });

        alert.show();
    }

    public static String readAssetFile(Context context, String file) {
        InputStream input = null;
        BufferedReader buf = null;
        try {
            StringBuilder s = new StringBuilder();
            input = context.getAssets().open(file);
            buf = new BufferedReader(new InputStreamReader(input));

            String str;
            while ((str = buf.readLine()) != null) {
                s.append(str).append("\n");
            }
            return s.toString().trim();
        } catch (IOException ignored) {
        } finally {
            try {
                if (input != null) input.close();
                if (buf != null) buf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void restartApp(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    public static void setDefaultLanguage(Context context) {
        saveBoolean("use_english", false, context);
        saveBoolean("use_korean", false, context);
        saveBoolean("use_am", false, context);
        saveBoolean("use_el", false, context);
        saveBoolean("use_ml", false, context);
        saveBoolean("use_pt", false, context);
        saveBoolean("use_ru", false, context);
        saveBoolean("use_uk", false, context);
    }

    public static boolean languageDefault(Context context) {
        return !getBoolean("use_english", false, context)
                && !getBoolean("use_korean", false, context)
                && !getBoolean("use_am", false, context)
                && !getBoolean("use_el", false, context)
                && !getBoolean("use_ml", false, context)
                && !getBoolean("use_pt", false, context)
                && !getBoolean("use_ru", false, context)
                && !getBoolean("use_uk", false, context);
    }

    public static String getLanguage(Context context) {
        if (getBoolean("use_english", false, context)) {
            return  "en_US";
        } else if (getBoolean("use_korean", false, context)) {
            return  "ko";
        } else if (getBoolean("use_am", false, context)) {
            return  "am";
        } else if (getBoolean("use_el", false, context)) {
            return  "el";
        } else if (getBoolean("use_ml", false, context)) {
            return  "ml";
        } else if (getBoolean("use_pt", false, context)) {
            return  "pt";
        } else if (getBoolean("use_ru", false, context)) {
            return  "ru";
        } else if (getBoolean("use_uk", false, context)) {
            return  "uk";
        } else {
            return java.util.Locale.getDefault().getLanguage();
        }
    }

    public static void setLanguage(Context context) {
        Locale myLocale = new Locale(getLanguage(context));
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
    }

    public static void resetDefault(Context context) {
        saveBoolean("asus_apps", false, context);
        saveBoolean("google_apps", false, context);
        saveBoolean("huawei_apps", false, context);
        saveBoolean("lg_apps", false, context);
        saveBoolean("mi_apps", false, context);
        saveBoolean("moto_apps", false, context);
        saveBoolean("oneplus_apps", false, context);
        saveBoolean("samsung_apps", false, context);
        saveBoolean("sony_apps", false, context);
        if (mSortByOEM) {
            saveBoolean("system_apps", false, context);
            saveBoolean("user_apps", false, context);
            mSortByOEM = false;
        }
    }

}