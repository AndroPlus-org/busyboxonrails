package me.timos.busyboxonrails;

import static me.timos.busyboxonrails.Utility.doEntry;
import static me.timos.busyboxonrails.Utility.shellExec;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipOutputStream;

import kellinwood.security.zipsigner.ZipSigner;
import me.timos.br.Logcat;
import android.widget.Toast;

public class ServiceRecoveryInstall extends ServiceBase {

	public ServiceRecoveryInstall() {
		super(ServiceRecoveryInstall.class.getName());
	}

	@Override
	protected void doBusybox(File busybox) {
		File unsignedZip = getFileStreamPath("busybox-unsigned.zip");
		File signedZip = getFileStreamPath("busybox-signed.zip");
		File reboot = new File("/system/bin/reboot");

		try {
			ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(
					unsignedZip));
			doEntry(this, zout, R.raw.install_update_binary,
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
				String.format("cat \"%s\" > /cache/busybox.zip",
						signedZip.getAbsolutePath()),
				String.format("cat \"%s\" > /cache/busybox",
						busybox.getAbsolutePath()),
				"mkdir -p /cache/recovery",
				"echo '--update_package=CACHE:busybox.zip' > /cache/recovery/command",
				"chmod 644 /cache/busybox.zip",
				"chmod 644 /cache/recovery/command",
				"sync",
				"if [ -f /cache/busybox.zip ] && [ -f /cache/busybox ] && [ -f /cache/recovery/command ]; then",
				"echo OK", "fi");
		if (!ret.startsWith("OK")) {
			Logcat.e("ERROR WRITIING RECOVERY DATA TO CACHE PARTITION\n" + ret);
			mApp.showToast(R.string.error_write_recovery_data,
					Toast.LENGTH_LONG);
			return;
		}

		if (reboot.canExecute()) {
			// TODO
			// shellExec(null, null, "stop", "sync", "reboot recovery");
			Logcat.d("OKOKOKKOKOKKO");
		} else {
			mApp.showToast(R.string.msg_reboot_manually, Toast.LENGTH_LONG);
		}
	}

}
