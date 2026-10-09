package com.gigiclick.ui;

public final class Anim {
	private float value;
	private final float speed;

	public Anim(float initial, float speed) {
		this.value = initial;
		this.speed = speed;
	}

	public float update(float target, float dt) {
		value += (target - value) * (1f - (float) Math.exp(-speed * dt));
		if (Math.abs(target - value) < 0.001f) {
			value = target;
		}
		return value;
	}

	public float get() {
		return value;
	}

	public void set(float value) {
		this.value = value;
	}
}
