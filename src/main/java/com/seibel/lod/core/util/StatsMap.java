package com.seibel.lod.core.util;

import java.util.TreeMap;

public class StatsMap 
{
	final TreeMap<String, Long> longMap = new TreeMap<String, Long>();
	final TreeMap<String, UnitBytes> bytesMap = new TreeMap<String, UnitBytes>();

	/**
	 * 
	 */
	@SuppressWarnings("unused")
	private static final long serialVersionUID = 1926219295516863173L;
	
	public StatsMap() {super();}
	
	public void incStat(String key) {
		incStat(key, 1);
	}
	public void incStat(String key, long value) {
		longMap.put(key, longMap.getOrDefault(key, 0L)+value);
	}
	public void incBytesStat(String key, long bytes) {
		bytesMap.put(key, new UnitBytes(bytesMap.getOrDefault(key, new UnitBytes(0)).value()+bytes));
	}
	
	@Override
	public String toString() {
		return longMap.toString() + " " + bytesMap.toString();
	}
	
	
}
