package me.timos.busyboxonrails;

import static me.timos.busyboxonrails.Utility.checkFileIntegrity;
import static me.timos.busyboxonrails.Utility.shellExec;

import java.io.File;

import me.timos.br.Logcat;
import android.widget.Toast;

public class ServiceNormalOperation extends ServiceBase {

	public ServiceNormalOperation() {
		super(ServiceNormalOperation.class.getName());
	}

	@Override
	protected void doBusybox(File busybox) {
		mApp.showToast(R.string.msg_working, Toast.LENGTH_LONG);
		File tmp = new File("/mnt/asec/busybox");
		File writableTest = new File("/system/simple_busybox_test");
		File target = new File("/system/xbin/busybox");
		File lastApplet = new File("/system/xbin/zcat");

		Logcat.d("Write temp busybox");
		shellExec(null, null, "cat \"" + busybox + "\" > " + tmp, "chmod 755 "
				+ tmp);
		if (!tmp.canExecute()) {
			Logcat.e("ERROR CREATE TEMP BUSYBOX");
			mApp.showToast(R.string.error_tmp_busybox, Toast.LENGTH_LONG);
			return;
		}

		Logcat.d("Remount /system writable");
		shellExec(tmp.getParent(), null,
				"./busybox mount -o remount,rw /system", "touch "
						+ writableTest);
		if (!writableTest.exists()) {
			Logcat.e("ERROR MOUNT");
			mApp.showToast(R.string.error_mount_rw, Toast.LENGTH_LONG);
			return;
		}

		Logcat.d("Cleanup /system/bin and /system/xbin of previous busybox installations");
		shellExec(tmp.getParent(), null,
				"for i in `./busybox find /system/bin -type l`; do",
				"if [ \"`./busybox ls -l $i|./busybox grep busybox`\" ]; then",
				"echo $i", "rm $i", "fi", "done", "rm /system/bin/busybox");
		shellExec(tmp.getParent(), null,
				"for i in `./busybox find /system/xbin -type l`; do",
				"if [ \"`./busybox ls -l $i|./busybox grep busybox`\" ]; then",
				"echo $i", "rm $i", "fi", "done", "rm /system/xbin/busybox");

		if (mOpId == R.id.radCleanupInstall) {
			Logcat.d("Write busybox to /system/xbin");
			shellExec(tmp.getParent(), null, "cat " + tmp + " > " + target,
					"chmod 755 " + target);
			if (!target.canExecute()
					|| !checkFileIntegrity(this, target, getBusyboxResPath())) {
				Logcat.e("ERROR WRITE BUSYBOX TO /system/xbin");
				mApp.showToast(R.string.error_write_busybox_to_xbin,
						Toast.LENGTH_LONG);
				return;
			}

			Logcat.d("Create applets");
			String ret = shellExec(target.getParent(), null,
					"for i in `./busybox --list`; do", "rm $i",
					"./busybox ln -s busybox $i", "done", "./busybox ls -l "
							+ lastApplet);
			if (ret.endsWith("zcat -> busybox")) {
				Logcat.d("INSTALLATION SUCCEEDED");
				mApp.showToast(R.string.msg_install_succeeded,
						Toast.LENGTH_LONG);
			} else {
				mApp.showToast(R.string.error_applets, Toast.LENGTH_LONG);
				Logcat.e("FAILED creating applets \n" + ret);
			}
		} else {
			Logcat.d("UNINSTALL COMPLETED");
			mApp.showToast(R.string.msg_uninstall_completed, Toast.LENGTH_LONG);
		}

		Logcat.d("Remount /system readonly");
		shellExec(tmp.getParent(), null, "rm " + writableTest + " " + tmp,
				"sync", "./busybox mount -o remount,ro /system");

		Logcat.d("Remove temp busybox");
		shellExec(null, null, "rm \"" + tmp + "\"");
	}

}
