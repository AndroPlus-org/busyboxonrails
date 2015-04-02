package me.timos.busyboxonrails;

import android.widget.Toast;

import java.io.File;

import me.timos.br.Logcat;

import static me.timos.busyboxonrails.Utility.checkFileIntegrity;
import static me.timos.busyboxonrails.Utility.shellExec;

public class AsyncOperationNormal extends AsyncOperation {

    @Override
    protected void doBusybox(File busybox, File reboot) {
        boolean lowSpace = false;
        File tmp = new File("/cache/busybox");
        File target = new File("/system/xbin/busybox");
        File lastApplet = new File("/system/xbin/zcat");
        String ret;

        Logcat.d("Write temp busybox");
        ret = shellExec(null, null, "cat \"" + busybox + "\" > " + tmp,
                "chmod 755 " + tmp, "ls -l " + tmp);
        if (!ret.startsWith("-rwxr-xr-x")) {
            Logcat.e("ERROR CREATE TEMP BUSYBOX");
            mApp.showToast(R.string.error_tmp_busybox, Toast.LENGTH_LONG);
            return;
        }

        Logcat.d("Remount /system writable");
        ret = shellExec(tmp.getParent(), null,
                "mount -o remount,rw /system &> /dev/null",
                "if [ $? -eq 0 ]; then", "echo good", "else",
                "./busybox mount -o remount,rw /system &> /dev/null",
                "if [ $? -eq 0 ]; then", "echo good", "fi", "fi");
        if (!ret.contains("good")) {
            Logcat.e("ERROR MOUNT");
            mApp.showToast(R.string.error_mount_rw, Toast.LENGTH_LONG);
            return;
        }

        Logcat.d("Cleanup /system/bin and /system/xbin of previous busybox installations");
        ret = shellExec(
                tmp.getParent(),
                null,
                "for i in /system/bin/*; do",
                "if [ -L \"$i\" ] && ([ \"`./busybox ls -l \\\"$i\\\"|./busybox grep busybox`\" ]" +
                        " || [ ! -e \"$i\" ]); then",
                "echo $i", "rm \"$i\"", "fi", "done", "rm /system/bin/busybox");
        ret = shellExec(
                tmp.getParent(),
                null,
                "for i in /system/xbin/*; do",
                "if [ -L \"$i\" ] && ([ \"`./busybox ls -l \\\"$i\\\"|./busybox grep busybox`\" ]" +
                        " || [ ! -e \"$i\" ]); then",
                "echo $i", "rm \"$i\"", "fi", "done", "rm /system/xbin/busybox",
                "rm /data/local/busybox");
        if (mOpId == R.id.radCleanupInstall) {
            if (new File("/system").getFreeSpace() < 2097152) {
                lowSpace = true;
                target = new File("/data/local/busybox");
            }

            Logcat.d("Write busybox to device");
            if (lowSpace) {
                shellExec(tmp.getParent(), null, "./busybox mkdir -p /data/local",
                        "./busybox chown 0.0 /data/local", "chmod 755 /data/local",
                        "cat " + tmp + " > " + target, "chmod 755 " + target,
                        "./busybox mkdir -p /system/xbin",
                        "./busybox chown 0.2000 /system/xbin",
                        "chmod 755 /system/xbin",
                        "./busybox ln -s " + target + " /system/xbin/busybox");
                target = new File("/system/xbin/busybox");
            } else {
                shellExec(tmp.getParent(), null, "./busybox mkdir -p /system/xbin",
                        "./busybox chown 0.2000 /system/xbin",
                        "chmod 755 /system/xbin", "cat " + tmp + " > " + target,
                        "chmod 755 " + target);
            }
            if (!target.canExecute()
                    || !checkFileIntegrity(mApp, target, getBusyboxResPath())) {
                Logcat.e("ERROR WRITE BUSYBOX TO /system/xbin");
                mApp.showToast(R.string.error_write_busybox_to_xbin,
                        Toast.LENGTH_LONG);
                return;
            }

            Logcat.d("Create applets");
            ret = shellExec(target.getParent(), null,
                    "./busybox --install -s .", "./busybox ls -l " + lastApplet);
            if (ret.endsWith("zcat -> /system/xbin/busybox") || ret.endsWith("zcat -> " +
                    "/data/local/busybox")) {
                Logcat.d("INSTALLATION SUCCEEDED");
                mApp.showToast(R.string.msg_install_succeeded, Toast.LENGTH_LONG);
            } else {
                mApp.showToast(R.string.error_applets, Toast.LENGTH_LONG);
                Logcat.e("FAILED creating applets \n" + ret);
            }
        } else {
            Logcat.d("UNINSTALL COMPLETED");
            mApp.showToast(R.string.msg_cleanup_completed, Toast.LENGTH_LONG);
        }

        Logcat.d("Cleanup and remount /system readonly");
        shellExec(tmp.getParent(), null, "mount -o remount,ro /system",
                "./busybox mount -o remount,ro /system", "rm " + tmp, "sync");
    }

    @Override
    public void onPostExecute(Void result) {
        super.onPostExecute(result);
        ((ActivityMain) getActivity()).checkSystemBusybox();
    }

}
