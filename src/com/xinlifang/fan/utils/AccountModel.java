package com.xinlifang.fan.utils;

public class AccountModel {
	public String accountName;
	public String appId;
	public String appSecret;

	@Override
	public String toString() {
		return accountName;
	}

	public AccountModel(String name, String id, String secrete) {
		accountName = name;
		appId = id;
		appSecret = secrete;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((appId == null) ? 0 : appId.hashCode());
		result = prime * result + ((appSecret == null) ? 0 : appSecret.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AccountModel other = (AccountModel) obj;
		if (appId == null) {
			if (other.appId != null)
				return false;
		} else if (!appId.equals(other.appId))
			return false;
		if (appSecret == null) {
			if (other.appSecret != null)
				return false;
		} else if (!appSecret.equals(other.appSecret))
			return false;
		return true;
	}

}
