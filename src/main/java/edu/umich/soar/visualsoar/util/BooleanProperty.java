package edu.umich.soar.visualsoar.util;

import java.util.Objects;

/**
 * Mutable {@code boolean} wrapper
 */
public class BooleanProperty {

	boolean value;

	public BooleanProperty(boolean value) {
		this.value = value;
	}

	public boolean get() {
		return value;
	}
	public void set(boolean value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BooleanProperty that = (BooleanProperty) o;
		return get() == that.get();
	}

	@Override
	public int hashCode() {
		return Objects.hash(get());
	}

	@Override
	public String toString() {
		return "BooleanProperty{" +
			"value=" + value +
			'}';
	}
}
