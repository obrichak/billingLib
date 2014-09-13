package com.demo.billing;

public abstract class BillingObserver {
	public abstract void onProductPurchased(String transactionId, String sku);
	public abstract void onPurchaseRestored(String sku);
	public abstract void onPurchaseCanceled();
	public abstract void onPurchaseFailed(int errorCode);
	
	public static native void nativeOnProductPurchased(String sku);
	public static native void nativeOnPurchaseRestored(String sku);
	public static native void nativeOnPurchaseCanceled();
	public static native void nativeOnPurchaseFailed(int errorCode);
}
