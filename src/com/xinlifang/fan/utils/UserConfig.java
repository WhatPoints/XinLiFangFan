package com.xinlifang.fan.utils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class UserConfig {
	private static final String PREF_NAME = "userInfo.config";
	private static final String KEY_ACCOUNT = "KEY_ACCOUNT";
	private static final String KEY_ACCOUNT_LIST = "KEY_ACCOUNT_LIST";
	private static final String KEY_REFRESH_FREQUENCE = "KEY_REFRESH_FREQUENCE";
	private static final String KEY_OFFSET = "KEY_OFFSET";
	private Context mContext;

	public UserConfig(Context context) {
		mContext = context;
	}

	/**
	 * 添加帐号
	 * 
	 * @param accntName
	 * @param accntAppId
	 * @param accntAppSecrete
	 * @return true成功添加帐号，false，该帐号已经存在
	 */
	public boolean addAccount(AccountModel model) {
		List<AccountModel> accountsList = getAccountsList();
		if (model != null && accountsList.contains(model)) {
			return false;
		}
		accountsList.add(model);
		String json = new Gson().toJson(accountsList);
		getPreferences().edit().putString(KEY_ACCOUNT_LIST, json).commit();
		return true;
	}

	/**
	 * @return 非空的帐号列表
	 */
	public List<AccountModel> getAccountsList() {
		String accntListJson = getPreferences().getString(KEY_ACCOUNT_LIST, null);
		if (!TextUtils.isEmpty(accntListJson)) {
			Type type = new TypeToken<List<AccountModel>>() {
			}.getType();
			return new Gson().fromJson(accntListJson, type);
		}
		return new ArrayList<AccountModel>();
	}

	/**
	 * 记录当前显示的帐号
	 */
	public boolean setAccount(AccountModel model, int index) {
		if (model == null) {
			return false;
		}
		String json = new Gson().toJson(model);
		getPreferences().edit().putString(KEY_ACCOUNT + index, json).commit();
		return true;
	}

	/**
	 * @param mIndex
	 * @return 上次显示的帐号
	 */
	public AccountModel getAccount(int index) {
		String json = getPreferences().getString(KEY_ACCOUNT + index, null);
		if (!TextUtils.isEmpty(json)) {
			return new Gson().fromJson(json, AccountModel.class);
		}
		return null;
	}

	/**
	 * @param frequence
	 *            刷新频率，毫秒单位，不能小于1分钟，小于一分钟的参数视为1分钟
	 */
	public void setRefreshFrequence(long frequence) {
		getPreferences().edit().putLong(KEY_REFRESH_FREQUENCE, frequence).commit();
	}

	/**
	 * @return 刷新频率，默认1.5分钟
	 */
	public long getRefreshFrequence() {
		return getPreferences().getLong(KEY_REFRESH_FREQUENCE, 90000);
	}

	private SharedPreferences getPreferences() {
		return mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
	}

	public void setAccountList(List<AccountModel> accntsList) {
		String json = new Gson().toJson(accntsList);
		getPreferences().edit().putString(KEY_ACCOUNT_LIST, json).commit();
	}

	public void deleteAccountsList(ArrayList<AccountModel> tempList) {
		List<AccountModel> accntsList = getAccountsList();
		accntsList.removeAll(tempList);
		setAccountList(accntsList);
	}

	public void deleteAccount(int index) {
		getPreferences().edit().remove(KEY_ACCOUNT + index).commit();
	}

	public void setOffset(int index, long offset) {
		getPreferences().edit().putLong(KEY_OFFSET + index, offset).commit();
	}

	/**
	 * 获取上抛的粉丝数，默认12万
	 * 
	 * @param index
	 *            帐号序号
	 * @return 上抛的粉丝数
	 */
	public long getOffset(int index) {
		return getPreferences().getLong(KEY_OFFSET + index, 120000);
	}
}
