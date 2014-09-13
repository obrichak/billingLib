package com.demo.billing.google;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.demo.billing.Billing;

public class GoogleBilling extends Billing{
	private static final String TAG = "GoogleBilling";
	private static final String BILLING_SERVICE_INTENT = "com.android.vending.billing.InAppBillingService.BIND";
	private static final int API_VERSION = 3;
	private static final String PURCHASE_TYPE_INAPP = "inapp";
	private static final String PURCHASE_TYPE_SUBSCRIPTIONS = "subs";
	
	private static final String KEY_RESPONSE_CODE = "RESPONSE_CODE";
	private static final String KEY_BUY_INTENT = "BUY_INTENT";
	private static final String KEY_INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
	private static final String KEY_INAPP_PURCHASE_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST";
	private static final String KEY_INAPP_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN";
	
	private static final String JSON_ORDER_ID = "orderId";
	private static final String JSON_PACKAGE_NAME = "packageName";
	private static final String JSON_PRODUCT_ID = "productId";
	private static final String JSON_PURCHASE_STATE = "purchaseState";
	private static final String JSON_DEVELOPER_PAYLOAD = "developerPayload";
	private static final String JSON_PURCHASE_TOKEN = "purchaseToken";
	
	private static final int PURCHASE_STATE_PURCHASED = 0;
	private static final int PURCHASE_STATE_CANCELED = 1;
	private static final int PURCHASE_STATE_REFUNDED = 2;
	
	private static int currentRequestCode = 0;
	
	private IInAppBillingService mService;
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			log("Service disconnected");
			mService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			log("Service connected");
			mService = IInAppBillingService.Stub.asInterface(service);
			
			if(nativeAppEnabled)
				nativeInit();
			
			if(!isBillingSupported()){
				log("Billing not supported");
				unbind();
				log("Unbinded");
				mService = null;
			}
		}
	};
	
	private Preferences prefs;
	//         Request Code  Dev payload  Is Consumable
	private HashMap<Integer, Entry<String, Boolean>> unprocessedRequests = new HashMap<Integer, Entry<String,Boolean>>();
	
	public GoogleBilling(Context context, boolean nativeAppEnabled) {
		super(context, nativeAppEnabled);
		
		prefs = new Preferences(context);
		
		context.bindService(new Intent(BILLING_SERVICE_INTENT), mServiceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public boolean isBillingSupported() {
		if(mService == null){
			return false;
		}
		
		try {
			int result = mService.isBillingSupported(API_VERSION, context.getPackageName(), PURCHASE_TYPE_INAPP); 
			log("IsBillingSupported result = "+result);
			return result == RESULT_OK;
		} catch (RemoteException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void purchaseItem(String sku, boolean consumable) {
		log("PurchaseItem started for "+(consumable?"consumable":"non-consumable")+" item "+sku);
		
		if(mService == null){
			log("Error! Service = null");
			return;
		}
		
		try {
			currentRequestCode++;
			//TODO: Create developer payload string and check it in onActivityResult!!!
			String developerPayload = "payload";
			Bundle responseBundle = mService.getBuyIntent(API_VERSION, context.getPackageName(), sku, PURCHASE_TYPE_INAPP, developerPayload);
			
			int result = responseBundle.getInt(KEY_RESPONSE_CODE, RESULT_ERROR); 
			if(result!=RESULT_OK){
				log("Purchase failed with error code "+result);
				notifyPurchaseFailed(result);
				return;
			}
			
			PendingIntent intent = responseBundle.getParcelable(KEY_BUY_INTENT);
			((Activity) context).startIntentSenderForResult(intent.getIntentSender(), currentRequestCode, new Intent(), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0));
			
			unprocessedRequests.put(currentRequestCode, new Entry<String, Boolean>(developerPayload, consumable));
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (SendIntentException e) {
			e.printStackTrace();
		}
		
		log("PurchaseItem finished");
	}

	@Override
	public void restorePurchases() {
		if(mService==null){
			log("RestorePurchases not started because mService is null");
			return;
		}
		
		log("RestorePurchases started");
		//Consume all purchased but not consumed consumables
		for(Iterator<Map.Entry<String, String>> it = prefs.getConsumables(); it.hasNext(); ){
			Map.Entry<String, String> purchase = it.next();
			log("Consuming consumable "+purchase.getValue());
			consumePurchase(purchase.getKey(), purchase.getValue());
		}
		
		//Restore all non-consumable purchases 
		if(prefs.isRestoreNeeded()){
			log("Need to restore non-consumable purchases");
			String continuationToken = null;
			do {
				try {
					Bundle purchasedItems = mService.getPurchases(API_VERSION, context.getPackageName(), PURCHASE_TYPE_INAPP, continuationToken);
					int response = purchasedItems.getInt(KEY_RESPONSE_CODE);
					log("Response = "+response);
					if (response == RESULT_OK) {
						ArrayList<String> skus = purchasedItems.getStringArrayList(KEY_INAPP_PURCHASE_ITEM_LIST);
						continuationToken = purchasedItems.getString(KEY_INAPP_CONTINUATION_TOKEN);
						
						log("Continuation token = "+continuationToken);
						log("Purchased skus: "+(skus.isEmpty()?"No purchased skus":""));
						for (String sku : skus) {
							log("SKU: "+sku);
							prefs.addNonConsumable(sku);
							notifyPurchaseRestored(sku);
						}
					} else
						break;

				} catch (RemoteException e) {
					log("Remote exception!");
					e.printStackTrace();
				}
			} while (continuationToken != null);
			
			prefs.setRestoreNeeded(false);
		}
		
		prefs.save();
		
		log("Restore purchases finished");
	}
	
	@Override
	public boolean isProductPurchased(String sku) {
		return prefs.isProductPurchased(sku);
	}
	
	/**
	 * In this method Google Play's purchase activity returns the purchase result
	 * and purchase info as the extras of the response intent
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent responseIntent) {
		log("OnActivityResult started with request code "+requestCode+", result code "+resultCode+" and intent "+responseIntent);
		Entry<String, Boolean> requestInfo = unprocessedRequests.remove(requestCode);

		//Do nothing if purchase was just canceled
		if(resultCode == RESULT_CANCELED){
			log("Purchase was canceled");
			notifyPurchaseCanceled();
			return;
		}
		
		//Checking various error scenarios
		if(requestInfo == null){
			log("Purchase failed because no purchase request was made");
			notifyPurchaseFailed(RESULT_ERROR);
			return;
		}
		
		if(responseIntent == null){
			log("Purchase failed because result intent was null");
			notifyPurchaseFailed(RESULT_ERROR);
			return;
		}
		
		int result = responseIntent.getIntExtra(KEY_RESPONSE_CODE, RESULT_ERROR);
		if(result != RESULT_OK){
			if(result == RESULT_CANCELED){
				log("Purchae canceled");
				notifyPurchaseCanceled();
			}else{
				log("Purchae failed with error code "+result);
				notifyPurchaseFailed(result);
			}
			return;
		}
		
		String purchaseInfo = responseIntent.getStringExtra(KEY_INAPP_PURCHASE_DATA);
		if(purchaseInfo == null){
			log("Purchase failed because Google Play returned no purchase info");
			notifyPurchaseFailed(RESULT_ERROR);
			return;
		}
		
		//Purchase was made and its paramaters were provided in a json string
		try {
			JSONObject jsonPurchaseInfo = new JSONObject(purchaseInfo);
			final String productId = jsonPurchaseInfo.getString(JSON_PRODUCT_ID);
			int purchaseState = jsonPurchaseInfo.getInt(JSON_PURCHASE_STATE);
			String developerPayload = jsonPurchaseInfo.getString(JSON_DEVELOPER_PAYLOAD);
			final String purchaseToken = jsonPurchaseInfo.getString(JSON_PURCHASE_TOKEN);
			
			if(purchaseState == PURCHASE_STATE_CANCELED){
				log("Purchase canceled");
				notifyPurchaseCanceled();
				return;
			}
			
			//Developer payload check for better security
			if(!developerPayload.equals(requestInfo.getKey())){
				log("Purchase failed because of developer payload mismatch");
				notifyPurchaseFailed(RESULT_ERROR);
				return;
			}
			
			//Start consume request if purchase was consumable or just provision it to the user.
			if(requestInfo.getValue()){
				prefs.addConsumable(purchaseToken, productId);
				prefs.save();
				consumePurchase(purchaseToken, productId);
			}else{
				prefs.addNonConsumable(productId);
				prefs.save();
				notifyItemPurhcased(purchaseToken, productId);
			}
			
		} catch (JSONException e) {
			log("Unable to parse purchase info JSON");
			e.printStackTrace();
		}
	}
	
	@Override
	public void destroy() {
		unbind();
		
		mService = null;
		mServiceConnection = null;
		prefs.save();
		
		super.destroy();
	}
	
	private void unbind(){
		if(mService != null){
			context.unbindService(mServiceConnection);
			log("Unbinded");
		}
	}
	
	private void consumePurchase(final String purchaseToken, final String sku){
		new Thread(new Runnable() {
			@Override
			public void run() {
				log("ConsumePurchase started");
				try {
					int response = mService.consumePurchase(API_VERSION, context.getPackageName(), purchaseToken);
					
					if(response == RESULT_OK){
						notifyItemPurhcased(purchaseToken, sku);
					}else{
						log("Consuming purchase failed with error code "+response);
						notifyPurchaseFailed(response);
					}
					
					prefs.removeConsumable(purchaseToken);
					prefs.save();					
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				
				log("ConsumePurchase finished");
			}
		}).start();
	}
}
