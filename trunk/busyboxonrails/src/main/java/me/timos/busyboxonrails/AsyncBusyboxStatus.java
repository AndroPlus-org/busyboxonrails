package me.timos.busyboxonrails;

import android.text.TextUtils;
import android.widget.Toast;

import com.stericson.RootShell.RootShell;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import me.timos.busyboxonrails.ActivityMain.ENUM_BB_STATUS;

import static me.timos.busyboxonrails.ActivityMain.ENUM_BB_STATUS.BB_NOT_LINKED_APPLETS;
import static me.timos.busyboxonrails.ActivityMain.ENUM_BB_STATUS.BB_OK;
import static me.timos.busyboxonrails.ActivityMain.ENUM_BB_STATUS.NO_BB;
import static me.timos.busyboxonrails.Utility.shellExec;
import static me.timos.busyboxonrails.Utility.uncheckedCast;

public class AsyncBusyboxStatus extends
        FragmentAsyncTask<Void, Void, ENUM_BB_STATUS> {

    private TreeMap<String, String> mBusyboxInfo = new TreeMap<String, String>();
    private TreeSet<String> mSupportedApplets = new TreeSet<String>();
    private TreeSet<String> mNotLinkedApplets = new TreeSet<String>();

    @Override
    public ENUM_BB_STATUS doInBackground(Void... params) {
        if (!RootShell.isAccessGiven()) {
            ((SbApp) getActivity().getApplication()).showToast(
                    R.string.msg_root_na, Toast.LENGTH_LONG);
            return null;
        }

        String[] paths = new String[]{"/system/bin", "/system/xbin"};
        for (String dir : paths) {
            File busybox = new File(dir, "busybox");
            if (busybox.canExecute()) {
                String[] text = shellExec(dir, null, "./busybox").split(" ");
                if (text.length > 2) {
                    String version = text[1];
                    if (!TextUtils.isEmpty(version)) {
                        mBusyboxInfo.put(busybox.getPath(), version);
                        String[] supportedApplets = shellExec(null, null,
                                busybox.getPath().concat(" --list")).split("\n");
                        mSupportedApplets.addAll(Arrays.asList(supportedApplets));
                    }
                }
            }
        }
        if (mBusyboxInfo.isEmpty()) {
            return NO_BB;
        }

        Set<String> busyboxPaths = mBusyboxInfo.keySet();
        mNotLinkedApplets = uncheckedCast(mSupportedApplets.clone());
        for (String appletName : mSupportedApplets) {
            for (String dir : paths) {
                File applet = new File(dir, appletName);
                try {
                    if (applet.canExecute()
                            && (busyboxPaths.contains(applet.getCanonicalPath()) ||
                            applet.length() > 0)) {
                        mNotLinkedApplets.remove(appletName);
                    }
                } catch (IOException e) {
                }
            }
        }

        if (mNotLinkedApplets.isEmpty()) {
            return BB_OK;
        } else {
            return BB_NOT_LINKED_APPLETS;
        }
    }

    @Override
    public void onPostExecute(ENUM_BB_STATUS result) {
        if (result == null) {
            getActivity().finish();
            return;
        }
        ((ActivityMain) getActivity()).setBbStatus(result, mBusyboxInfo,
                mSupportedApplets, mNotLinkedApplets);
    }
}
