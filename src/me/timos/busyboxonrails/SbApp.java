package me.timos.busyboxonrails;

import static me.timos.busyboxonrails.Constant.LOG_TAG;
import me.timos.br.BR;
import me.timos.br.enumerator.LogStorageType;
import me.timos.br.enumerator.ReportMode;
import android.app.Application;
import android.os.Handler;
import android.widget.Toast;

import com.stericson.RootTools.RootTools;

public class SbApp extends Application {

	private Handler mHandler;
	private Toast mToast;

	private void configLogger() {
		BR.setMode(ReportMode.NONE);
		BR.setLogStorage(LogStorageType.BUFFER);
		BR.setLogBufferSize(65536);
		BR.setLogTag(LOG_TAG);
		// TODO: logcat
		BR.logToLogcat(true);

		BR.init(this);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		RootTools.handlerEnabled = false;
		configLogger();

		mHandler = new Handler();
	}

	public void showToast(int resId, int duration) {
		try {
			showToast(getString(resId), duration);
		} catch (Exception e) {
		}
	}

	public void showToast(final String toast, final int duration) {
		mHandler.post(new Runnable() {

			public void run() {
				if (mToast == null) {
					mToast = Toast.makeText(SbApp.this, toast, duration);
				} else {
					mToast.setText(toast);
					mToast.setDuration(duration);
				}
				mToast.show();
			}
		});
	}

}
