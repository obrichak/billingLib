package com.demo.billing.amazon;

import java.util.HashMap;
import java.util.HashSet;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import com.amazon.inapp.purchasing.BasePurchasingObserver;
import com.amazon.inapp.purchasing.GetUserIdResponse;
import com.amazon.inapp.purchasing.ItemDataResponse;
import com.amazon.inapp.purchasing.Offset;
import com.amazon.inapp.purchasing.PurchaseResponse;
import com.amazon.inapp.purchasing.PurchaseResponse.PurchaseRequestStatus;
import com.amazon.inapp.purchasing.PurchaseUpdatesResponse;
import com.amazon.inapp.purchasing.PurchaseUpdatesResponse.PurchaseUpdatesRequestStatus;
import com.amazon.inapp.purchasing.PurchasingManager;
import com.amazon.inapp.purchasing.Receipt;
import com.demo.billing.Billing;

public class AmazonBilling extends Billing {
	private static final String TAG = "AmazonBilling";
	
	private class AmazonBillingObserever extends BasePurchasingObserver{
		public AmazonBillingObserever(Context context) {
			super(context);
		}
		
		@Override
		public void onSdkAvailable(boolean sandbox) {
			billingSupported = true;
			if(sandbox)
				Toast.makeText(context, "In-app purchases are running in sandbox mode", Toast.LENGTH_LONG).show();
			if(nativeAppEnabled)
				nativeInit();
			restorePurchases();
		}
		
		@Override
		public void onGetUserIdResponse(GetUserIdResponse response) {}
		
		@Override
		public void onItemDataResponse(ItemDataResponse response) {}
		
		@Override
		public void onPurchaseResponse(PurchaseResponse response) {
			String sku = purchaseRequests.remove(response.getRequestId());
			if(sku == null)
				return;
			
			switch(response.getPurchaseRequestStatus()){
			case FAILED:
				//notifyPurchaseFailed(RESULT_ERROR);
				notifyPurchaseCanceled();
				return;
				
			case INVALID_SKU:
				Toast.makeText(context, "Invalid SKU", Toast.LENGTH_LONG).show();
				notifyPurchaseFailed(RESULT_ITEM_UNAVAILABLE);
				return;
				
			case ALREADY_ENTITLED:
				restorePurchases();
				return; 
				
			case SUCCESSFUL:
				purchasedSkus.add(response.getReceipt().getSku());
				notifyItemPurhcased(response.getRequestId(), response.getReceipt().getSku());
				return;
			}
		}
		
		@Override
		public void onPurchaseUpdatesResponse(PurchaseUpdatesResponse response) {
			if(response.getPurchaseUpdatesRequestStatus() != PurchaseUpdatesRequestStatus.SUCCESSFUL)
				return;
			
			for(Receipt receipt : response.getReceipts()){
				String sku = receipt.getSku();
				purchasedSkus.add(sku);
				notifyPurchaseRestored(sku);
			}
			
			if(response.isMore())
				PurchasingManager.initiatePurchaseUpdatesRequest(response.getOffset());
		}
	}
	
	
	
	private AmazonBillingObserever amazonObserver;	
	private boolean billingSupported = false;
	
	private HashSet<String> purchasedSkus;
	private HashMap<String, String> purchaseRequests;
	
	private Handler mHandler;
	
	
	public AmazonBilling(Context context, boolean nativeAppEnabled) {
		super(context, nativeAppEnabled);
		
		purchasedSkus = new HashSet<String>();
		purchaseRequests = new HashMap<String, String>();
		
		amazonObserver = new AmazonBillingObserever(context);
		PurchasingManager.registerObserver(amazonObserver);
		
		mHandler = new Handler();
	}

	@Override
	public boolean isBillingSupported() {
		return billingSupported;
	}

	/*
	 * Amazon handles the purchase type by itself so the 'consumable' field value is ignored. 
	 */
	@Override
	public void purchaseItem(final String sku, boolean consumable) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				purchaseRequests.put(PurchasingManager.initiatePurchaseRequest(sku), sku);
			}
		});
	}

	@Override
	public void restorePurchases() {
		PurchasingManager.initiatePurchaseUpdatesRequest(Offset.BEGINNING);
	}

	@Override
	public boolean isProductPurchased(String sku) {
		return purchasedSkus.contains(sku);
	}

}
