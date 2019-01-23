package cf.playhi.freezeyou;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import net.grandcentrix.tray.AppPreferences;

import static cf.playhi.freezeyou.ApplicationIconUtils.getApplicationIcon;
import static cf.playhi.freezeyou.ApplicationIconUtils.getBitmapFromDrawable;
import static cf.playhi.freezeyou.ApplicationLabelUtils.getApplicationLabel;
import static cf.playhi.freezeyou.DebugModeUtils.isDebugModeEnabled;
import static cf.playhi.freezeyou.Support.checkMRootFrozen;
import static cf.playhi.freezeyou.Support.checkRootFrozen;
import static cf.playhi.freezeyou.Support.processFreezeAction;
import static cf.playhi.freezeyou.Support.processUnfreezeAction;
import static cf.playhi.freezeyou.Support.realGetFrozenStatus;
import static cf.playhi.freezeyou.Support.shortcutMakeDialog;
import static cf.playhi.freezeyou.ThemeUtils.processAddTranslucent;
import static cf.playhi.freezeyou.ThemeUtils.processSetTheme;
import static cf.playhi.freezeyou.ToastUtils.showToast;

public class Freeze extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        processSetTheme(this);
        processAddTranslucent(this);
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    private void init() {
        Intent intent = getIntent();
        if (intent != null) {

            if (isDebugModeEnabled(this)) {
                Log.e("DebugModeLogcat", "Intent toString: " + intent.toString());
                Log.e("DebugModeLogcat", "Intent getDataString: " + intent.getDataString());
                Log.e("DebugModeLogcat", "Intent getStringExtra_pkgName: " + intent.getStringExtra("pkgName"));
                Log.e("DebugModeLogcat", "Intent getBooleanExtra_auto: " + intent.getBooleanExtra("auto", true));
                Log.e("DebugModeLogcat", "Intent getAction: " + intent.getAction());
                Log.e("DebugModeLogcat", "Intent getPackage: " + intent.getPackage());
                Log.e("DebugModeLogcat", "Intent getScheme: " + intent.getScheme());
                Log.e("DebugModeLogcat", "Intent getType: " + intent.getType());
            }

            String pkgName;
            boolean auto;
            if ("freezeyou".equals(intent.getScheme())) {
                Uri dataUri = intent.getData();
                pkgName = (dataUri == null) ? null : dataUri.getQueryParameter("pkgName");
                auto = false;
            } else {
                pkgName = intent.getStringExtra("pkgName");
                auto = intent.getBooleanExtra("auto", true);
            }
            if (pkgName == null) {
                showToast(getApplicationContext(), R.string.invalidArguments);
                Freeze.this.finish();
            } else if ("".equals(pkgName)) {
                showToast(getApplicationContext(), R.string.invalidArguments);
                Freeze.this.finish();
            }
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            if (auto && sp.getBoolean("shortcutAutoFUF", false)) {
                if (realGetFrozenStatus(this, pkgName, getPackageManager())) {
                    processUnfreezeAction(this, pkgName, true, sp.getBoolean("openImmediatelyAfterUnfreezeUseShortcutAutoFUF", true), this, true);
                } else {
                    if (sp.getBoolean("needConfirmWhenFreezeUseShortcutAutoFUF", false)) {
                        processDialog(pkgName, false, 2);
                    } else {
                        processFreezeAction(this, pkgName, true, this, true);
                    }
                }
            } else if ((!checkRootFrozen(Freeze.this, pkgName, null)) && (!checkMRootFrozen(Freeze.this, pkgName))) {
                processDialog(pkgName, auto, 2);
            } else {
                processDialog(pkgName, auto, 1);
            }
            if (Build.VERSION.SDK_INT >= 21) {
                setTaskDescription(new ActivityManager.TaskDescription(getApplicationLabel(this, null, null, pkgName) + " - " + getString(R.string.app_name), getBitmapFromDrawable(getApplicationIcon(this, pkgName, null, false))));
            }
        }
    }

    private void processDialog(String pkgName, boolean auto, int ot) {
        shortcutMakeDialog(Freeze.this, getApplicationLabel(Freeze.this, null, null, pkgName), getString(R.string.chooseDetailAction), Freeze.this, null, pkgName, ot, auto, true);
    }

    @Override
    public void finish() {
        if (Build.VERSION.SDK_INT >= 21 && !(new AppPreferences(this).getBoolean("showInRecents", true))) {
            finishAndRemoveTask();
        }
        super.finish();
    }
}
