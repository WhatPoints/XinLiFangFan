package com.xinlifang.fan;

import java.text.NumberFormat;
import java.util.List;
import java.util.Random;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.xinlifang.fan.utils.AccountModel;
import com.xinlifang.fan.utils.TipUtils;
import com.xinlifang.fan.utils.UserConfig;

public class AccountFragment extends Fragment implements OnClickListener {
	private static final String TAG = "AccountFragment";
	private static final String URL_FOR_TOKEN = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
	private static final String URL_FOR_FAN_NUMBER = "https://api.weixin.qq.com/cgi-bin/user/get?access_token=%s";
	private static final String KEY_INDEX = "KEY_INDEX";
	private static final long TWO_HOUR = 6000 * 1000; // 每隔两个小时需要重新获取token，这里设置一个略小于2小时的时间间隔
	private TextView mTvAccountName;
	private TextView mTvFanNumber;
	private TextView mTvShortNumber;
	private AccountModel mCurrAccount;
	private AsyncHttpClient mClient;
	private String mToken;
	private String mUrlForToken;
	private CountDownTimer mTokenTimer;
	private CountDownTimer mFanNumberTimer;
	private int mIndex;
	private BroadcastReceiver mReceiver;
	private NumberFormat nf = NumberFormat.getInstance();

	public static AccountFragment newInstance(int index) {
		AccountFragment fragment = new AccountFragment();
		Bundle args = new Bundle();
		args.putInt(KEY_INDEX, index);
		fragment.setArguments(args);
		return fragment;
	}

	public AccountFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			mIndex = getArguments().getInt(KEY_INDEX);
			UserConfig config = new UserConfig(getActivity());
			mCurrAccount = config.getAccount(mIndex);
			mOffset = config.getOffset(mIndex);
		}
		mReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				UserConfig config = new UserConfig(getActivity());
				if (!config.getAccountsList().contains(mCurrAccount)) {
					config.deleteAccount(mIndex);
					stopTimer();
					setAccountName("帐号名");
					mTvFanNumber.setText("--");
				}
			}
		};
		IntentFilter filter = new IntentFilter(MainActivity.ACTION_DELETE_ACCOUNT);
		getActivity().registerReceiver(mReceiver, filter);

		mClient = new AsyncHttpClient();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View layout = inflater.inflate(R.layout.frag_followers_number, container, false);
		mTvAccountName = (TextView) layout.findViewById(R.id.tv_frag_accountName);
		mTvFanNumber = (TextView) layout.findViewById(R.id.tv_frag_FanNumber);
		mTvShortNumber = (TextView) layout.findViewById(R.id.btn_frag_shortNumber);
		layout.findViewById(R.id.btn_frag_offset).setOnClickListener(this);
		if (mCurrAccount != null) {
			setAccountName(mCurrAccount.accountName);
			mUrlForToken = getUrlForToken();
			startTokenTimer();
		}
		layout.findViewById(R.id.btn_frag_switchAccnt).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onClickSwitchAccnt();
			}
		});
		return layout;
	}

	private void setFollowerNumber(long number) {
		// FIXME 厦门本地新闻这个帐号没有认证，得不到获取用户列表的权限，因此取厦门快讯的粉丝数x0.9拿来显示
		if (mIndex == 2) {
			number = getNumber();
			if (number == 0) {
				number = (218000 + new Random().nextInt(50));
			} else {
				number += new Random().nextInt(10);
			}
			saveNumber(number);
		}
		mTvFanNumber.setText(nf.format(number));
	}

	private long getNumber() {
		SharedPreferences prefs = getActivity().getSharedPreferences("temp", Context.MODE_PRIVATE);
		return prefs.getLong("number", 0);
	}

	private void saveNumber(long number) {
		SharedPreferences prefs = getActivity().getSharedPreferences("temp", Context.MODE_PRIVATE);
		prefs.edit().putLong("number", number).commit();
	}

	private long getFollowerNumber() {
		StringBuffer number = new StringBuffer(mTvFanNumber.getText());
		if (TextUtils.isEmpty(number) || "--".equals(number)) {
			return 0;
		} else {
			int location = -1;
			while (true) {
				location = number.indexOf(",");
				if (location == -1)
					break;
				else
					number.deleteCharAt(location);
			}
			return Long.valueOf(number.toString().trim());
		}
	}

	private String getUrlForToken() {
		String tokenUrl = String.format(URL_FOR_TOKEN, mCurrAccount.appId, mCurrAccount.appSecret);
		Log.e(TAG + mIndex, "getToken: url = " + tokenUrl);
		return tokenUrl;
	}

	private String getUrlForFanNumber() {
		String numberUrl = String.format(URL_FOR_FAN_NUMBER, mToken);
		Log.e(TAG + mIndex, "getFanNumber: url = " + numberUrl);
		return numberUrl;
	}

	private void setAccountName(String accountName) {
		mTvAccountName.setText(accountName);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		getActivity().unregisterReceiver(mReceiver);
		stopTimer();
	}

	private void startTokenTimer() {
		if (mTokenTimer != null) {
			mTokenTimer.cancel();
			mTokenTimer = null;
		}
		mTokenTimer = new CountDownTimer(TWO_HOUR * 12, TWO_HOUR) { // 获取token的计时器

			@Override
			public void onTick(long arg0) {
				Log.e(TAG + mIndex, "TokenTimer onTick");
				getToken();
			}

			@Override
			public void onFinish() {
			}
		}.start();
	}

	private void startFanNumberTimer(final String url) {
		if (mFanNumberTimer != null) {
			mFanNumberTimer.cancel();
			mFanNumberTimer = null;
		}
		long frequence = new UserConfig(getActivity()).getRefreshFrequence();
		mFanNumberTimer = new CountDownTimer(TWO_HOUR * 12, frequence) {// 刷新公众号关注数的计时器

			@Override
			public void onTick(long arg0) {
				Log.e(TAG + mIndex, "FanNumberTimer onTick");
				getFanNumber(url);
			}

			@Override
			public void onFinish() {
			}
		}.start();
	}

	private void stopTimer() {
		if (mTokenTimer != null) {
			mTokenTimer.cancel();
		}
		if (mFanNumberTimer != null) {
			mFanNumberTimer.cancel();
		}
	}

	// 获取微信公众号借口访问凭证
	private void getToken() {
		mClient.get(mUrlForToken, new AsyncHttpResponseHandler() {

			@Override
			public void onSuccess(int arg0, Header[] arg1, byte[] arg2) {
				JSONObject jsonObject;
				String json = null;
				try {
					json = new String(arg2);
					jsonObject = new JSONObject(json);
					mToken = jsonObject.getString("access_token");
					String url = getUrlForFanNumber();
					// 重新启动获取粉丝数的计时器
					startFanNumberTimer(url);
					Log.e(TAG + mIndex, "getToken: " + mToken);
				} catch (JSONException e) {
					Log.e(TAG + mIndex, "getToken: JSONException " + e.getMessage());
				}
			}

			@Override
			public void onFailure(int arg0, Header[] arg1, byte[] arg2, Throwable arg3) {
				Log.e(TAG + mIndex, "getToken: onFailure " + arg3.getMessage());
			}
		});
	}

	// 获取公众号的用户数
	protected void getFanNumber(String url) {
		mClient.get(url, new AsyncHttpResponseHandler() {

			@Override
			public void onSuccess(int arg0, Header[] arg1, byte[] arg2) {
				JSONObject jsonObject;
				String json = new String(arg2);
				String totalFanNumber = "0";
				try {
					jsonObject = new JSONObject(json);
					Log.e(TAG + mIndex, "jsonObject: " + jsonObject);

					if (jsonObject.has("total")) {
						totalFanNumber = jsonObject.getString("total");
					}
				} catch (JSONException e) {
					Log.e(TAG + mIndex, "getFanNumber: JSONException " + e.getMessage());
				}
				setFollowerNumber(Long.valueOf(totalFanNumber) + mOffset);
			}

			@Override
			public void onFailure(int arg0, Header[] arg1, byte[] arg2, Throwable arg3) {
				Log.e(TAG + mIndex, "getFanNumber: onFailure " + arg3.getMessage());
			}
		});
	}

	// 切换帐号
	public void onClickSwitchAccnt() {
		final UserConfig config = new UserConfig(getActivity());
		final List<AccountModel> accntsList = config.getAccountsList();
		if (accntsList.size() == 0) {
			TipUtils.showToast(getActivity(), "还未添加帐号！");
			return;
		}
		String[] items = new String[accntsList.size()];
		for (int i = 0; i < accntsList.size(); i++) {
			items[i] = accntsList.get(i).toString();
		}
		final int index = mCurrAccount == null ? -1 : accntsList.indexOf(mCurrAccount);
		final int[] choiceIndex = new int[1];
		choiceIndex[0] = index;
		new AlertDialog.Builder(getActivity()).setTitle("切换帐号")
				.setSingleChoiceItems(items, index, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						choiceIndex[0] = which;
					}
				}).setNegativeButton("取消", null)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (choiceIndex[0] == index) {
							return;
						}
						mCurrAccount = accntsList.get(choiceIndex[0]);
						config.setAccount(mCurrAccount, mIndex);
						mUrlForToken = getUrlForToken();
						setAccountName(mCurrAccount.accountName);
						startTokenTimer(); // 重新获取token
						dialog.dismiss();
					}
				}).show();
	}

	private int mHiddenBtnClickCnt;
	private boolean mIsStarted; // 重置隐藏按钮点击计数
	protected long mOffset; // 上抛粉丝数

	// 隐藏按钮 设置上抛粉丝数
	@Override
	public void onClick(View v) {
		mHiddenBtnClickCnt++;
		if (mHiddenBtnClickCnt == 7) { // 连续点击 7 次开启对话框
			mHiddenBtnClickCnt = 0;
			showAddFanDialog();
		}

		if (!mIsStarted) {
			v.postDelayed(new Runnable() {

				@Override
				public void run() {
					mHiddenBtnClickCnt = 0;
					mIsStarted = false;
				}
			}, 3000);
			mIsStarted = true;
		}
	}

	public void showAddFanDialog() {
		View layout = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_offset, null);
		final EditText editWan = (EditText) layout.findViewById(R.id.edit_dialog_offset_wan);
		final EditText editQian = (EditText) layout.findViewById(R.id.edit_dialog_offset_qian);
		editWan.addTextChangedListener(new OffsetTextWatcher());
		editQian.addTextChangedListener(new OffsetTextWatcher());

		if (mOffset > 0 && mOffset < 10000) {
			editQian.setText("" + mOffset / 1000);
		} else if (mOffset >= 10000) {
			editWan.setText("" + mOffset / 10000);
			editQian.setText("" + mOffset % 10000 / 1000);
		}
		editWan.setSelection(editWan.length());
		editQian.setSelection(editQian.length());

		final AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle("设置上抛数量")
				.setView(layout).create();
		layout.findViewById(R.id.btn_dialog_offset_cancel).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						dialog.dismiss();
					}
				});
		layout.findViewById(R.id.btn_dialog_offset_confirm).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						String wan = editWan.getText().toString().trim();
						String qian = editQian.getText().toString().trim();
						long offset = Long.valueOf(wan) * 10000 + Long.valueOf(qian) * 1000;
						long followerNumber = getFollowerNumber() - mOffset;
						setFollowerNumber(followerNumber + offset);

						UserConfig config = new UserConfig(getActivity());
						config.setOffset(mIndex, offset);
						mOffset = offset;

						dialog.dismiss();
					}
				});
		dialog.show();
	}

	private class OffsetTextWatcher implements TextWatcher {

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
		}

		@Override
		public void afterTextChanged(Editable s) {
			if (s.length() == 0) {
				s.append('0');
			} else if (s.length() > 1 && s.charAt(0) == '0') {
				s.delete(0, 1); // 删除第一个字符0
			}
		}

	}

	public long getFunNumber() {
		String number = mTvFanNumber.getText().toString().trim().replace(",", "");
		if (!"--".equals(number)) {
			return Long.valueOf(number);
		}
		return 0;
	}

}
