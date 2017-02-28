package com.odoo.core.support;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.odoo.core.rpc.helper.OdooVersion;
import com.odoo.core.rpc.helper.utils.OBundleUtils;
import com.odoo.work.R;

public class OUser {

    public static final String TAG = OUser.class.getSimpleName();
    public static final int USER_ACCOUNT_VERSION = 2;
    private Account account;
    private String username, name, timezone, avatar, database, host, session_id, fcm_project_id;
    private Integer userId, partnerId, companyId;
    private Boolean isActive = false, allowForceConnect = false;
    private OdooVersion odooVersion;

    public static OUser current(Context context) {
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        Account[] accounts = accountManager.getAccountsByType(context.getString(R.string.auth_type));
        if (accounts.length > 0) {
            OUser user = new OUser();
            user.fillFromAccount(accountManager, accounts[0]);
            user.setAccount(accounts[0]);
            return user;
        }
        return null;
    }

    public Boolean isAllowForceConnect() {
        return allowForceConnect;
    }

    public void setAllowForceConnect(Boolean allowForceConnect) {
        this.allowForceConnect = allowForceConnect;
    }

    public String getSession_id() {
        return session_id;
    }

    public void setSession_id(String session_id) {
        this.session_id = session_id;
    }

    public Boolean isActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public Integer getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Integer partnerId) {
        this.partnerId = partnerId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getAndroidName() {
        return username + "[" + database + "]";
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public OdooVersion getOdooVersion() {
        return odooVersion;
    }

    public void setOdooVersion(OdooVersion odooVersion) {
        this.odooVersion = odooVersion;
    }

    public Bundle getAsBundle() {
        Bundle data = new Bundle();
        data.putString("username", getUsername());
        data.putString("name", getName());
        data.putString("timezone", getTimezone());
        data.putString("avatar", getAvatar());
        data.putString("database", getDatabase());
        data.putString("host", getHost());
        data.putString("android_name", getAndroidName());
        data.putInt("user_id", getUserId());
        data.putInt("partner_id", getPartnerId());
        data.putInt("company_id", getCompanyId());
        data.putBoolean("is_active", isActive());
        data.putBoolean("allow_force_connect", isAllowForceConnect());
        data.putString("session_id", getSession_id());
        data.putString("fcm_project_id", getFCMId());
        if (odooVersion != null) {
            data.putAll(odooVersion.getAsBundle());
        }
        // Converting each value to string. Account supports only string values
        for (String key : data.keySet()) {
            data.putString(key, data.get(key) + "");
        }
        return data;
    }

    public void fillFromBundle(Bundle data) {
        if (OBundleUtils.hasKey(data, "username"))
            setUsername(data.getString("username"));
        if (OBundleUtils.hasKey(data, "name"))
            setName(data.getString("name"));
        if (OBundleUtils.hasKey(data, "timezone"))
            setTimezone(data.getString("timezone"));
        if (OBundleUtils.hasKey(data, "avatar"))
            setAvatar(data.getString("avatar"));
        if (OBundleUtils.hasKey(data, "database"))
            setDatabase(data.getString("database"));
        if (OBundleUtils.hasKey(data, "host"))
            setHost(data.getString("host"));
        if (OBundleUtils.hasKey(data, "user_id"))
            setUserId(data.getInt("user_id"));
        if (OBundleUtils.hasKey(data, "partner_id"))
            setPartnerId(data.getInt("partner_id"));
        if (OBundleUtils.hasKey(data, "company_id"))
            setCompanyId(data.getInt("company_id"));
        if (OBundleUtils.hasKey(data, "is_active"))
            setIsActive(data.getBoolean("is_active"));
        if (OBundleUtils.hasKey(data, "allow_force_connect"))
            setAllowForceConnect(data.getBoolean("allow_force_connect"));
        if (OBundleUtils.hasKey(data, "session_id")) {
            setSession_id(data.getString("session_id"));
        }
        if (OBundleUtils.hasKey(data, "fcm_project_id")) {
            setFCMID(data.getString("fcm_project_id"));
        }
        odooVersion = new OdooVersion();
        odooVersion.fillFromBundle(data);
    }

    public String getFCMId() {
        return fcm_project_id;
    }

    public void setFCMID(String fcmid) {
        fcm_project_id = fcmid;
    }

    public void setFromBundle(Bundle data) {
        fillFromBundle(data);
    }

    public void fillFromAccount(AccountManager accMgr, Account account) {
        setName(accMgr.getUserData(account, "name"));
        setUsername(accMgr.getUserData(account, "username"));
        setUserId(Integer.parseInt(accMgr.getUserData(account, "user_id")));
        setPartnerId(Integer.parseInt(accMgr.getUserData(account, "partner_id")));
        setAvatar(accMgr.getUserData(account, "avatar"));
        setDatabase(accMgr.getUserData(account, "database"));
        setHost(accMgr.getUserData(account, "host"));
        setCompanyId(Integer.parseInt(accMgr.getUserData(account, "company_id")));
        setSession_id(accMgr.getUserData(account, "session_id"));
        setFCMID(accMgr.getUserData(account, "fcm_project_id"));
    }

    public String getDBName() {
        String db_name = "OdooSQLite";
        db_name += "_" + getUsername();
        db_name += "_" + getDatabase();
        return db_name + ".db";
    }

    @Override
    public String toString() {
        return getAndroidName();
    }

}
