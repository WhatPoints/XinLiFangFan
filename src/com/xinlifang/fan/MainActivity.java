package com.xinlifang.fan;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

import com.xinlifang.fan.utils.AccountModel;
import com.xinlifang.fan.utils.TipUtils;
import com.xinlifang.fan.utils.UserConfig;

public class MainActivity extends FragmentActivity {
	public static final String ACTION_DELETE_ACCOUNT = "action.delete.account";
	private static final int MENU_ADD_ACCNT = 1;
	private static final int MENU_DEL_ACCNT = 2;
	private static final int MENU_SWI_ACCNT1 = 31;
	private static final int MENU_SWI_ACCNT2 = 32;
	private static final int MENU_ADD_FAN1 = 41;
	private static final int MENU_ADD_FAN2 = 42;
	private Context mContext = this;
	private AccountFragment mFrag1;
	private AccountFragment mFrag2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// add default accounts
		addDefaultAccounts();

		initUI();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_ADD_ACCNT, 0, "添加帐号");
		menu.add(0, MENU_DEL_ACCNT, 1, "删除帐号");
		menu.add(0, MENU_SWI_ACCNT1, 2, "切换帐号1");
		menu.add(0, MENU_SWI_ACCNT2, 3, "切换帐号2");
		menu.add(0, MENU_ADD_FAN1, 4, "上抛帐号1粉丝数");
		menu.add(0, MENU_ADD_FAN2, 5, "上抛帐号2粉丝数");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ADD_ACCNT:
			onClickAddAccnt(null);
			break;
		case MENU_DEL_ACCNT:
			onClickDelAccnt(null);
			break;
		case MENU_SWI_ACCNT1:
			mFrag1.onClickSwitchAccnt();
			break;
		case MENU_SWI_ACCNT2:
			mFrag2.onClickSwitchAccnt();
			break;
		case MENU_ADD_FAN1:
			mFrag1.showAddFanDialog();
			break;
		case MENU_ADD_FAN2:
			mFrag2.showAddFanDialog();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void addDefaultAccounts() {
		UserConfig userConfig = new UserConfig(this);
		List<AccountModel> accountsList = userConfig.getAccountsList();
		if (accountsList.size() == 0) {
			AccountModel model1 = new AccountModel("厦门快讯", "wxa363b4a01f1ee33e",
					"f4e90263b59ccc80dd4332323d1dfa2c");
			AccountModel model2 = new AccountModel("厦门本地新闻", "wx7df96cdb3e4ae7a3",
					"222ac40fef8906e3a2879025aa5ea85d");
			userConfig.addAccount(model1);
			userConfig.addAccount(model2);
			userConfig.setAccount(model1, 1);
			userConfig.setAccount(model2, 2);
		}
	}

	private void initUI() {
		mFrag1 = AccountFragment.newInstance(1);
		mFrag2 = AccountFragment.newInstance(2);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.add(R.id.frag_container1, mFrag1);
		ft.add(R.id.frag_container2, mFrag2);
		ft.commit();
	}

	// 添加帐号
	public void onClickAddAccnt(View view) {
		final View dialogView = LayoutInflater.from(this)
				.inflate(R.layout.dialog_add_account, null);
		final AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
		dialogView.findViewById(R.id.btn_dialog_addAccnt_cancel).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						dialog.dismiss();
					}
				});
		dialogView.findViewById(R.id.btn_dialog_addAccnt_confirm).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						EditText editAccntName = (EditText) dialogView
								.findViewById(R.id.edit_dialog_addAccnt_accountName);
						EditText editAppId = (EditText) dialogView
								.findViewById(R.id.edit_dialog_addAccnt_appId);
						EditText editAppSecrete = (EditText) dialogView
								.findViewById(R.id.edit_dialog_addAccnt_appSecrete);

						String name = editAccntName.getText().toString().trim();
						String id = editAppId.getText().toString().trim();
						String secrete = editAppSecrete.getText().toString().trim();

						if (TextUtils.isEmpty(id) || TextUtils.isEmpty(secrete)) {
							TipUtils.showToast(mContext, "AppId和AppSecrete不能为空！");
							return;
						}

						boolean success = new UserConfig(mContext).addAccount(new AccountModel(
								name, id, secrete));
						if (success) {
							TipUtils.showToast(mContext, "帐号添加成功！");
							dialog.dismiss();
						} else {
							TipUtils.showToast(mContext, "帐号已经存在！");
						}

					}

				});
		dialog.show();
	}

	// 删除帐号
	public void onClickDelAccnt(View view) {
		final UserConfig config = new UserConfig(this);
		final List<AccountModel> accntsList = config.getAccountsList();
		if (accntsList.size() == 0) {
			TipUtils.showToast(mContext, "还未添加帐号！");
			return;
		}
		String[] items = new String[accntsList.size()];
		for (int i = 0; i < accntsList.size(); i++) {
			items[i] = accntsList.get(i).toString();
		}
		final ArrayList<AccountModel> tempList = new ArrayList<AccountModel>();
		new AlertDialog.Builder(this).setTitle("删除帐号")
				.setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						if (isChecked) {
							tempList.add(accntsList.get(which));
						} else {
							tempList.remove(accntsList.get(which));
						}
					}
				}).setNegativeButton("取消", null)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						config.deleteAccountsList(tempList);
						dialog.dismiss();
						Intent intent = new Intent(ACTION_DELETE_ACCOUNT);
						sendBroadcast(intent);
						TipUtils.showToast(mContext, "删除帐号成功！");
					}
				}).show();
	}

	// 刷新频率
	public void onClickRefreshFreq(View view) {
		// TODO Auto-generated method stub
	}

	public long getFunNumber(int index) {
		switch (index) {
		case 1:
			return mFrag1.getFunNumber();
		case 2:
			return mFrag2.getFunNumber();
		default:
			return 0;
		}
	}
}
