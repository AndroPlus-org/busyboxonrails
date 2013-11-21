package me.timos.busyboxonrails;

import static me.timos.busyboxonrails.Utility.installBinary;

import java.io.File;

import me.timos.br.Logcat;
import android.widget.Toast;

public abstract class AsyncOperation extends
		FragmentAsyncTask<Integer, Void, Void> {

	protected SbApp mApp;
	protected int mOpId;
	private String mBusyboxResPath;

	protected abstract void doBusybox(File busybox);

	@Override
	public void onPreExecute() {
		((ActivityMain) getActivity()).setPreOperation();
	}

	@Override
	public Void doInBackground(Integer... params) {
		mOpId = params[0];
		mApp = (SbApp) getActivity().getApplication();

		if (mOpId == 0) {
			throw new IllegalStateException(
					"Intent extra operation must be set");
		}

		String arch = System.getProperty("os.arch", "");
		if (arch.startsWith("armv7")) {
			Logcat.d("---armv7---");
			mBusyboxResPath = "res/raw/busybox_armv7";
			File busybox = installBinary(mApp, "busybox", R.raw.busybox_armv7,
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
		return null;
	}

	@Override
	public void onPostExecute(Void result) {
		((ActivityMain) getActivity()).setPostOperation();
	}
	
	protected String getBusyboxResPath() {
		return mBusyboxResPath;
	}

}
