package me.timos.busyboxonrails;

import android.widget.Toast;

import java.io.File;

import me.timos.br.Logcat;

import static me.timos.busyboxonrails.Utility.installBinary;

public abstract class AsyncOperation extends
        FragmentAsyncTask<Integer, Void, Void> {

    protected SbApp mApp;
    protected int mOpId;
    private String mBusyboxResPath;

    protected abstract void doBusybox(File busybox, File reboot);

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
            File reboot = installBinary(mApp, "reboot", R.raw.reboot_armv7,
                    "res/raw/reboot_armv7");
            if (busybox == null || reboot == null) {
                mApp.showToast(R.string.error_write_binary_internal_data,
                        Toast.LENGTH_LONG);
            } else {
                doBusybox(busybox, reboot);
            }
        } else if (arch.contains("86")) {
            Logcat.d("---x86---");
            mBusyboxResPath = "res/raw/busybox_x86";
            File busybox = installBinary(mApp, "busybox", R.raw.busybox_x86,
                    mBusyboxResPath);
            File reboot = installBinary(mApp, "reboot", R.raw.reboot_x86,
                    "res/raw/reboot_x86");
            if (busybox == null || reboot == null) {
                mApp.showToast(R.string.error_write_binary_internal_data,
                        Toast.LENGTH_LONG);
            } else {
                doBusybox(busybox, reboot);
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

    @Override
    public void onPreExecute() {
        ((ActivityMain) getActivity()).setPreOperation();
    }

    protected String getBusyboxResPath() {
        return mBusyboxResPath;
    }

}
