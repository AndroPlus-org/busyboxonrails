package me.timos.busyboxonrails;

import static me.timos.busyboxonrails.Constant.INTENT_OPERATION;
import static me.timos.busyboxonrails.Utility.installBinary;

import java.io.File;

import me.timos.br.Logcat;
import android.app.IntentService;
import android.content.Intent;
import android.widget.Toast;

import com.stericson.RootTools.RootTools;

public abstract class ServiceBase extends IntentService {

	protected SbApp mApp;
	protected int mOpId;
	private String mBusyboxResPath;

	public ServiceBase(String name) {
		super(name);
	}

	protected abstract void doBusybox(File busybox);

	protected String getBusyboxResPath() {
		return mBusyboxResPath;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		mOpId = intent.getIntExtra(INTENT_OPERATION, 0);
		mApp = (SbApp) getApplication();

		if (mOpId == 0) {
			throw new IllegalStateException(
					"Intent extra operation must be set");
		}

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
