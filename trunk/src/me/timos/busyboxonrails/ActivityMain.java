package me.timos.busyboxonrails;

import static me.timos.busyboxonrails.Constant.INTENT_OPERATION;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;

public class ActivityMain extends Activity {

	public static class BusyboxStatusInitializer extends
			FragmentAsyncTask<Void, Void, String[]> {

		@Override
		public String[] doInBackground(Void... params) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void onPostExecute(String[] result) {
			// TODO
		}
	}

	private RadioGroup mRadGrpMethod;
	private RadioGroup mRadGrpOp;

	public void onCancel(View v) {
		finish();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mRadGrpOp = (RadioGroup) findViewById(R.id.radGrpOperation);
		mRadGrpMethod = (RadioGroup) findViewById(R.id.radGrpMethod);

		if (savedInstanceState == null) {
			new BusyboxStatusInitializer().execute(getFragmentManager(), null);
		}
	}

	public void onGo(View v) {
		Intent i = new Intent(
				this,
				mRadGrpMethod.getCheckedRadioButtonId() == R.id.radNormal ? ServiceNormalOperation.class
						: ServiceRecoveryOperation.class);
		i.putExtra(INTENT_OPERATION, mRadGrpOp.getCheckedRadioButtonId());
		startService(i);
		finish();
	}

}
