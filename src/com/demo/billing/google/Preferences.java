package com.demo.billing.google;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

class Preferences {
	private static final String TAG = "Google Billing Preferences";
	private static final String PREFS_NAME = "BillingPrefs";
	private static final String KEY_CONSUMABLES = "consumables";
	private static final String KEY_NON_CONSUMABLES = "nonConsumables";
	private static final String KEY_RESTORE_NEEDED = "restoreNeeded";
	
	private SharedPreferences prefs;
	
	//      Purchase token  SKU
	private HashMap<String, String> consumables;
	private HashSet<String> nonConsumables;
	private boolean restoreNeeded;
	
	@SuppressWarnings("unchecked")
	public Preferences(Context context){
		prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		
		consumables = (HashMap<String, String>) deserializeObject(prefs.getString(KEY_CONSUMABLES, null));
		nonConsumables = (HashSet<String>) deserializeObject(prefs.getString(KEY_NON_CONSUMABLES, null));
		restoreNeeded = prefs.getBoolean(KEY_RESTORE_NEEDED, true);
		
		if(consumables == null) consumables = new HashMap<String, String>();
		if(nonConsumables == null) nonConsumables = new HashSet<String>();
	}
	
	public Iterator<Entry<String, String>> getConsumables(){
		return consumables.entrySet().iterator();
	}
	
	public Iterator<String> getNonConsumables(){
		return nonConsumables.iterator();
	}
	
	public void addConsumable(String purchaseToken, String productId){
		consumables.put(purchaseToken, productId);
	}
	
	public void removeConsumable(String purchaseToken){
		consumables.remove(purchaseToken);
	}
	
	public void addNonConsumable(String productId){
		nonConsumables.add(productId);
	}
	
	public void setRestoreNeeded(boolean needed){
		restoreNeeded = needed;
	}
	
	public boolean isProductPurchased(String sku){
		return nonConsumables.contains(sku);
	}
	
	public boolean isRestoreNeeded(){
		return restoreNeeded;
	}
	
	public void save(){
		prefs.edit().putString(KEY_CONSUMABLES, serializeObject(consumables)).
					 putString(KEY_NON_CONSUMABLES, serializeObject(nonConsumables)).
					 putBoolean(KEY_RESTORE_NEEDED, restoreNeeded).
					 commit();
	}
	
	private String serializeObject(Object object){
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		ObjectOutputStream objOut = null;
		try {
			objOut = new ObjectOutputStream(byteOut);
			objOut.writeObject(object);
			objOut.flush();
			
			return Base64.encodeToString(byteOut.toByteArray(), Base64.NO_WRAP);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally{
			try {
				objOut.close();
				byteOut.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private Object deserializeObject(String source){
		if(source==null || source.length()==0){
			return null;
		}
		
		byte[] buffer = Base64.decode(source, Base64.NO_WRAP);
		ByteArrayInputStream byteIn = new ByteArrayInputStream(buffer);
		ObjectInputStream objIn = null;
		try {
			objIn = new ObjectInputStream(byteIn);
			return objIn.readObject();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				objIn.close();
				byteIn.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
