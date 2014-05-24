package me.timos.busyboxonrails;

import static me.timos.busyboxonrails.Constant.DIALOG_LIST;
import static me.timos.busyboxonrails.Constant.DIALOG_TITLE;
import static me.timos.busyboxonrails.Utility.setSpanBetweenTokens;
import static me.timos.busyboxonrails.Utility.uncheckedCast;

import java.util.TreeMap;
import java.util.TreeSet;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

public class ActivityMain extends Activity implements OnClickListener,
		OnCheckedChangeListener {

	public static class DialogFragmentList extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Builder b = new Builder(getActivity());
			b.setTitle(getArguments().getInt(DIALOG_TITLE));
			b.setItems(getArguments().getStringArray(DIALOG_LIST), null);
			return b.create();
		}

	}

	public static enum ENUM_BB_STATUS {
		CHECKING, NO_BB, BB_NOT_LINKED_APPLETS, BB_OK
	}

	private static final String RUNNING_OPERATION = "running_operation";
	private static final String BB_INFO = "bb_info";
	private static final String BB_STATUS = "bb_status";
	private static final String NOT_LINKED_APPLETS = "not_linked_applets";
	private static final String SUPPORTED_APPLETS = "supported_applets";

	private RadioGroup mRadGrpMethod;
	private RadioGroup mRadGrpOp;
	private TextView mTxtBbStatus;
	private TextView mTxtBbInfo;
	private Button mBtnSupportedApplets;
	private Button mBtnNotLinkedApplets;
	private Button mBtnGo;
	private ENUM_BB_STATUS mBbStatus;
	private TreeMap<String, String> mBusyboxInfo;
	private TreeSet<String> mSupportedApplets;
	private TreeSet<String> mNotLinkedApplets;

	public void checkSystemBusybox() {
		setBbStatus(ENUM_BB_STATUS.CHECKING, new TreeMap<String, String>(),
				new TreeSet<String>(), new TreeSet<String>());
		new AsyncBusyboxStatus().execute(getFragmentManager(), null);
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		switch (checkedId) {
		case R.id.radCleanupInstall:
			mBtnGo.setText(R.string.install);
			break;
		case R.id.radCleanup:
			mBtnGo.setText(R.string.cleanup);
			break;
		}
	}

	@Override
	public void onClick(View v) {
		Bundle args = new Bundle();
		if (v == mBtnSupportedApplets) {
			args.putInt(DIALOG_TITLE, R.string.supported_applets);
			args.putStringArray(DIALOG_LIST, mSupportedApplets
					.toArray(new String[mSupportedApplets.size()]));
		} else if (v == mBtnNotLinkedApplets) {
			args.putInt(DIALOG_TITLE, R.string.not_linked_applets);
			args.putStringArray(DIALOG_LIST, mNotLinkedApplets
					.toArray(new String[mNotLinkedApplets.size()]));
		}
		DialogFragmentList dialog = new DialogFragmentList();
		dialog.setArguments(args);
		dialog.show(getFragmentManager(), null);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mRadGrpOp = (RadioGroup) findViewById(R.id.radGrpOperation);
		mRadGrpMethod = (RadioGroup) findViewById(R.id.radGrpMethod);
		mTxtBbStatus = (TextView) findViewById(R.id.txtBbStatus);
		mTxtBbInfo = (TextView) findViewById(R.id.txtBbInfo);
		mBtnSupportedApplets = (Button) findViewById(R.id.btnSupportedApplets);
		mBtnNotLinkedApplets = (Button) findViewById(R.id.btnNotlinkedApplets);
		mBtnGo = (Button) findViewById(R.id.btnGo);

		mBtnSupportedApplets.setOnClickListener(this);
		mBtnNotLinkedApplets.setOnClickListener(this);
		mRadGrpOp.setOnCheckedChangeListener(this);

		if (savedInstanceState == null) {
			checkSystemBusybox();
		} else {
			TreeMap<String, String> bbInfo = uncheckedCast(savedInstanceState
					.getSerializable(BB_INFO));
			TreeSet<String> supportedApplets = uncheckedCast(savedInstanceState
					.getSerializable(SUPPORTED_APPLETS));
			TreeSet<String> notLinkedApplets = uncheckedCast(savedInstanceState
					.getSerializable(NOT_LINKED_APPLETS));
			setBbStatus(
					ENUM_BB_STATUS.values()[savedInstanceState
							.getInt(BB_STATUS)],
					bbInfo, supportedApplets, notLinkedApplets);
			Fragment f = getFragmentManager().findFragmentByTag(
					RUNNING_OPERATION);
			if (f != null) {
				setPreOperation();
			}
		}
	}

	public void onExit(View v) {
		finish();
	}

	public void onGo(View v) {
		AsyncOperation op = mRadGrpMethod.getCheckedRadioButtonId() == R.id.radNormal ? new AsyncOperationNormal()
				: new AsyncOperationRecovery();
		op.execute(getFragmentManager(), RUNNING_OPERATION,
				mRadGrpOp.getCheckedRadioButtonId());
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(BB_STATUS, mBbStatus.ordinal());
		outState.putSerializable(BB_INFO, mBusyboxInfo);
		outState.putSerializable(SUPPORTED_APPLETS, mSupportedApplets);
		outState.putSerializable(NOT_LINKED_APPLETS, mNotLinkedApplets);
		super.onSaveInstanceState(outState);
	}

	private void setBbInfoText() {
		StringBuilder sb = new StringBuilder();
		for (String key : mBusyboxInfo.navigableKeySet()) {
			sb.append(" - ").append(key).append(", version: ")
					.append(mBusyboxInfo.get(key)).append("\n");
		}
		sb.deleteCharAt(sb.length() - 1);
		mTxtBbInfo.setVisibility(View.VISIBLE);
		mTxtBbInfo.setText(setSpanBetweenTokens(
				getString(R.string.lbl_busybox_info, sb.toString()), "##",
				new ForegroundColorSpan(0xFFAAAAAA)));
	}

	public void setBbStatus(ENUM_BB_STATUS result,
			TreeMap<String, String> busyboxInfo,
			TreeSet<String> supportedApplets, TreeSet<String> notLinkedApplets) {
		mBbStatus = result;
		mBusyboxInfo = busyboxInfo;
		mSupportedApplets = supportedApplets;
		mNotLinkedApplets = notLinkedApplets;

		switch (mBbStatus) {
		case CHECKING:
			setBbStatusText(R.string.bb_checking, 0);
			mTxtBbInfo.setVisibility(View.GONE);
			mBtnNotLinkedApplets.setVisibility(View.GONE);
			mBtnSupportedApplets.setVisibility(View.GONE);
			break;
		case NO_BB:
			setBbStatusText(R.string.bb_not_found, 0xFFCC0000);
			break;
		case BB_NOT_LINKED_APPLETS:
			setBbStatusText(R.string.bb_missing_applets, 0xFFFF8800);
			setBbInfoText();
			mBtnNotLinkedApplets.setVisibility(View.VISIBLE);
			mBtnSupportedApplets.setVisibility(View.VISIBLE);
			break;
		case BB_OK:
			setBbStatusText(R.string.bb_ok, 0xFF669900);
			setBbInfoText();
			mBtnSupportedApplets.setVisibility(View.VISIBLE);
			break;
		}
	}

	private void setBbStatusText(int statusTextRes, int textColor) {
		mTxtBbStatus
				.setText(setSpanBetweenTokens(
						getString(R.string.lbl_system_status,
								getString(statusTextRes)), "##",
						new ForegroundColorSpan(textColor), new StyleSpan(
								Typeface.BOLD), new RelativeSizeSpan(1.4f)));
	}

	public void setPostOperation() {
		mBtnGo.setEnabled(true);
		onCheckedChanged(mRadGrpOp, mRadGrpOp.getCheckedRadioButtonId());
	}

	public void setPreOperation() {
		mBtnGo.setEnabled(false);
		mBtnGo.setText(R.string.msg_working);
	}

}
