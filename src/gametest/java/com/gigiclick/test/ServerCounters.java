package com.gigiclick.test;

import java.util.concurrent.atomic.AtomicInteger;

public final class ServerCounters {
	public static final AtomicInteger SWINGS = new AtomicInteger();
	public static final AtomicInteger ATTACKS = new AtomicInteger();

	private ServerCounters() {
	}
}
