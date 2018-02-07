package cf.playhi.freezeyou;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import java.io.DataOutputStream;

import static cf.playhi.freezeyou.Support.addFrozen;
import static cf.playhi.freezeyou.Support.checkFrozen;
import static cf.playhi.freezeyou.Support.getDevicePolicyManager;
import static cf.playhi.freezeyou.Support.isDeviceOwner;
import static cf.playhi.freezeyou.Support.savePkgName2Name;
import static cf.playhi.freezeyou.Support.showToast;

public class OneKeyFreeze extends Activity {
    private static Process process = null;
    private static DataOutputStream outputStream = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity activity = this;
        String[] pkgNameList = getApplicationContext().getSharedPreferences(
                "AutoFreezeApplicationList", Context.MODE_PRIVATE).getString("pkgName","").split("\\|\\|");
        if (Build.VERSION.SDK_INT>=21 && isDeviceOwner(activity)){
            try {
                for (String aPkgNameList : pkgNameList) {
                    if (!checkFrozen(activity,aPkgNameList)){
                        savePkgName2Name(activity,aPkgNameList);
                        if (getDevicePolicyManager(activity).setApplicationHidden(
                                DeviceAdminReceiver.getComponentName(activity),aPkgNameList,true)){
                            addFrozen(activity,aPkgNameList);
                        } else {
                            showToast(activity,aPkgNameList + " Failed!");
                        }
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
                showToast(activity,"发生了点异常，操作仍将继续:" + e.getLocalizedMessage());
            }
        } else {
            try {
                process = Runtime.getRuntime().exec("su");
                outputStream = new DataOutputStream(process.getOutputStream());
                for (String aPkgNameList : pkgNameList) {
                    int tmp = getPackageManager().getApplicationEnabledSetting(aPkgNameList.replaceAll("\\|", ""));
                    if (tmp != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER && tmp != PackageManager.COMPONENT_ENABLED_STATE_DISABLED){
                        outputStream.writeBytes(
                                "pm disable " + aPkgNameList.replaceAll("\\|", "") + "\n");
                    }
                }
                outputStream.writeBytes("exit\n");
                outputStream.flush();
                int exitValue = process.waitFor();
                if (exitValue == 0) {
                    showToast(activity,R.string.executed);
                } else {
                    showToast(activity,R.string.mayUnrootedOrOtherEx);
                }
                Support.destroyProcess(true,outputStream,process,activity);
            } catch (Exception e){
                e.printStackTrace();
                showToast(activity,getString(R.string.exception)+e.getMessage());
                if (e.getMessage().contains("Permission denied")){
                    showToast(activity,R.string.mayUnrooted);
                }
                Support.destroyProcess(true,outputStream,process,activity);
            }
        }

    }

//    private static void destroyProcess(Boolean finish, DataOutputStream dataOutputStream, Process process1, Activity activity){
//        try {
//            if (dataOutputStream != null) {
//                dataOutputStream.close();
//            }
//            process1.destroy();
//            if (finish){
//                activity.finish();
//            }
//        } catch (Exception e) {
//            if (finish){
//                activity.finish();
//            }
//        }
//    }
}
