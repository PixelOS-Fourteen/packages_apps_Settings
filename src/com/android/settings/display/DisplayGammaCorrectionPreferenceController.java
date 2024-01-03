/*
 * Copyright (C) 2023 Project Astera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.display;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.SystemProperties;
import android.os.ServiceManager;
import android.app.IActivityManager;
import android.content.Intent;
import android.content.ComponentName;
import android.os.IBinder;
import android.view.WindowManagerGlobal;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.R;

public class DisplayGammaCorrectionPreferenceController extends BasePreferenceController {

    private static final String KEY_TOGGLE_GAMMA = "toggle_gamma";
    private static final String PROP_GAMMA_CORRECTION_ENABLED = "persist.sys.brightness.low.gamma";

    public DisplayGammaCorrectionPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        boolean isGammaCorrectionEnabled = SystemProperties.getBoolean(PROP_GAMMA_CORRECTION_ENABLED, false);
        ((SwitchPreference) preference).setChecked(isGammaCorrectionEnabled);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_TOGGLE_GAMMA.equals(preference.getKey())) {
            boolean isGammaCorrectionEnabled = ((SwitchPreference) preference).isChecked();
            SystemProperties.set(PROP_GAMMA_CORRECTION_ENABLED, Boolean.toString(isGammaCorrectionEnabled));
            showSystemUiRestartDialog(mContext);
            return true;
        }
        return false;
    }

    public static void restartSystemUi(Context context) {
        new RestartSystemUiTask(context).execute();
    }

    public static void showSystemUiRestartDialog(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.systemui_restart_title)
                .setMessage(R.string.systemui_restart_message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        restartSystemUi(context);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static class RestartSystemUiTask extends AsyncTask<Void, Void, Void> {
        private Context mContext;

        public RestartSystemUiTask(Context context) {
            super();
            mContext = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                ActivityManager am =
                        (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                IActivityManager ams = ActivityManager.getService();
                for (ActivityManager.RunningAppProcessInfo app: am.getRunningAppProcesses()) {
                    if ("com.android.systemui".equals(app.processName)) {
                        ams.killApplicationProcess(app.processName, app.uid);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
