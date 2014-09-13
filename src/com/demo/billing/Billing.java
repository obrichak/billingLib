package com.demo.billing;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

public abstract class Billing {
	private static final String TAG = "Billing";
	
	//Google billing values were taken. Convert result values to these in other implementations.
	protected static final int RESULT_OK = 0;
	protected static final int RESULT_CANCELED = 1;
	protected static final int RESULT_BILLING_UNAVAILABLE = 3;
	protected static final int RESULT_ITEM_UNAVAILABLE = 4;
	protected static final int RESULT_DEVELOPER_ERROR = 5;
	protected static final int RESULT_ERROR = 6;
	protected static final int RESULT_ITEM_ALREADY_OWNED = 7;
	protected static final int RESULT_ITEM_NOT_OWNED = 8;
	
	protected Context context;
	protected ArrayList<BillingObserver> observers = new ArrayList<BillingObserver>();
	
	protected boolean nativeAppEnabled = false;
	protected boolean debugEnabled = false;
	
	/*
	 * @param nativeAppEnabled if set to true, native callbacks will also be called. 
	 */
	public Billing(Context context, boolean nativeAppEnabled){
		this.context = context;
		this.nativeAppEnabled = nativeAppEnabled;
		debugEnabled = (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)); 
	}
	
	public abstract boolean isBillingSupported();
	public abstract void purchaseItem(String sku, boolean consumable);
	public abstract void restorePurchases();
	public abstract boolean isProductPurchased(String sku);
	
	public void addObserver(BillingObserver observer){
		observers.add(observer);
	}
	
	public void removeObserver(BillingObserver observer){
		observers.remove(observer);
	} 
	
	public void destroy(){
		observers.clear();
		context = null;
	}
	
	/*
	 * Call this method in your Activity's onActivityResult method 
	 * passing all parameters to the billing library
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent responseIntent){}
	
	protected void notifyItemPurhcased(String transactionId, String sku){
		for(BillingObserver observer : observers){
			observer.onProductPurchased(transactionId, sku);
		}
		
		if(nativeAppEnabled)
			BillingObserver.nativeOnProductPurchased(sku);
	}
	
	protected void notifyPurchaseRestored(String sku){
		for(BillingObserver observer : observers){
			observer.onPurchaseRestored(sku);
		}
		
		if(nativeAppEnabled)
			BillingObserver.nativeOnPurchaseRestored(sku);
	}
	
	protected void notifyPurchaseCanceled(){
		for(BillingObserver observer : observers){
			observer.onPurchaseCanceled();
		}
		
		if(nativeAppEnabled)
			BillingObserver.nativeOnPurchaseCanceled();
	}
	
	protected void notifyPurchaseFailed(int errorCode){
		for(BillingObserver observer : observers){
			observer.onPurchaseFailed(errorCode);
		}
		
		if(nativeAppEnabled)
			BillingObserver.nativeOnPurchaseFailed(errorCode);
	}
	
	protected void log(String msg){
		if(debugEnabled) Log.d(TAG, msg);
	}
	
	protected native void nativeInit();
}
