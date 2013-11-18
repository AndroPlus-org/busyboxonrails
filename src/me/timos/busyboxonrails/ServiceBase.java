package me.timos.busyboxonrails;

import static me.timos.busyboxonrails.Utility.installBinary;

import java.io.File;

import com.stericson.RootTools.RootTools;

import me.timos.br.Logcat;
import me.timos.busyboxonrails.R;
import android.app.IntentService;
import android.content.Intent;
import android.widget.Toast;

public abstract class ServiceBase extends IntentService {

	protected SbApp mApp;
	private String mBusyboxResPath;

	public ServiceBase(String name) {
		super(name);
	}

	protected String getBusyboxResPath() {
		return mBusyboxResPath;
	}

	protected abstract void doBusybox(File busybox);

	@Override
	protected void onHandleIntent(Intent intent) {
		mApp = (SbApp) getApplication();

		if (!RootTools.isAccessGiven()) {
			mApp.showToast(R.string.msg_root_na, Toast.LENGTH_LONG);
			return;
		}

		String arch = System.getProperty("os.arch", "");
		if (arch.startsWith("armv7")) {
			Logcat.d("---armv7---");
			mBusyboxResPath = "res/raw/busybox_armv7";
			File busybox = installBinary(this, "busybox", R.raw.busybox_armv7,
					mBusyboxResPath);
			if (busybox == null) {
				mApp.showToast(R.string.error_write_binary_internal_data,
						Toast.LENGTH_LONG);
			} else {
				doBusybox(busybox);
			}
		} else {
			mApp.showToast(R.string.error_unsupported_arch, Toast.LENGTH_LONG);
		}
	}

}
