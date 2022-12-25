package edu.umich.soar.visualsoar.parser;

import java.util.LinkedList;
import java.util.List;

/**
 * this class essentially does nothing. It only exists to support the parser
 */
public final class ValueMake {
    // Data Members
    private final RHSValue d_rhsValue;
    private final List<PreferenceSpecifier> preferenceSpecifiers = new LinkedList<>();

    public ValueMake(RHSValue rhsValue) {
        d_rhsValue = rhsValue;
    }

    public void add(PreferenceSpecifier ps) {
        preferenceSpecifiers.add(ps);
    }

    // Accessors
    public RHSValue getRHSValue() {
        return d_rhsValue;
    }

}
