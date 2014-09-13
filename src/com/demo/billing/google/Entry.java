package com.demo.billing.google;

import java.util.Map;

public class Entry<K, V> implements Map.Entry<K, V> {
	private K key = null;
	private V value = null;
	
	public Entry(K key, V value){
		this.key = key;
		this.value = value;
	}
	
	@Override
	public K getKey() {
		return key;
	}

	@Override
	public V getValue() {
		return value;
	}

	@Override
	public V setValue(V object) {
		value = object;
		return value;
	}
}
