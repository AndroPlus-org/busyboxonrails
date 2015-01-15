package me.timos.busyboxonrails;

import android.os.SystemClock;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipOutputStream;

import kellinwood.security.zipsigner.ZipSigner;
import me.timos.br.Logcat;

import static me.timos.busyboxonrails.Utility.doEntry;
import static me.timos.busyboxonrails.Utility.shellExec;

public class AsyncOperationRecovery extends AsyncOperation {

    @Override
    protected void doBusybox(File busybox, File reboot) {
        File unsignedZip = mApp.getFileStreamPath("busybox-unsigned.zip");
        File signedZip = mApp.getFileStreamPath("busybox-signed.zip");
        File rebootTmp = new File("/sbin/reboot");

        try {
            ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(
                    unsignedZip));
            doEntry(mApp,
                    zout,
                    mOpId == R.id.radCleanupInstall ? R.raw.install_update_binary
                            : R.raw.uninstall_update_binary,
                    "META-INF/com/google/android/update-binary");
            zout.close();
        } catch (Exception e) {
            Logcat.e("ERROR WRITING RECOVERY ZIP IN INTERNAL APP DATA", e);
            mApp.showToast(R.string.error_write_binary_internal_data,
                    Toast.LENGTH_LONG);
            return;
        }

        try {
            ZipSigner zipSigner = new ZipSigner();
            zipSigner.setKeymode(ZipSigner.KEY_TESTKEY);
            zipSigner.signZip(unsignedZip.getPath(), signedZip.getPath());
        } catch (Exception e) {
            Logcat.e("ERROR SIGNING RECOVERY ZIP", e);
            mApp.showToast(R.string.error_sign_zip, Toast.LENGTH_LONG);
            return;
        }

        String ret = shellExec(
                null,
                null,
                "mount -o remount,rw /",
                String.format("cat \"%s\" > \"%s\"", reboot.getAbsolutePath(),
                        rebootTmp.getAbsolutePath()),
                String.format("chmod 755 \"%s\"", rebootTmp.getAbsolutePath()),
                String.format("cat \"%s\" > /cache/busybox.zip",
                        signedZip.getAbsolutePath()),
                String.format("cat \"%s\" > /cache/busybox",
                        busybox.getAbsolutePath()),
                "mkdir -p /cache/recovery",
                "echo '--update_package=CACHE:busybox.zip' > /cache/recovery/command",
                "echo 'install /cache/busybox.zip' > /cache/recovery/openrecoveryscript",
                "chmod 644 /cache/busybox.zip",
                "chmod 644 /cache/recovery/command",
                "chmod 644 /cache/recovery/openrecoveryscript",
                "sync",
                "if [ -f /cache/busybox.zip ] && [ -f /cache/busybox ] && [ -f " +
                        "/cache/recovery/command ]; then",
                "echo OK", "fi");
        if (!ret.startsWith("OK")) {
            Logcat.e("ERROR WRITIING RECOVERY DATA TO CACHE PARTITION\n" + ret);
            mApp.showToast(R.string.error_write_recovery_data,
                    Toast.LENGTH_LONG);
            return;
        }

        shellExec(null, null, "stop", "sync", "/system/bin/reboot recovery",
                String.format("\"%s\" recovery", rebootTmp.getAbsolutePath()));
        SystemClock.sleep(100);
        mApp.showToast(R.string.msg_reboot_manually, Toast.LENGTH_LONG);
    }
}
