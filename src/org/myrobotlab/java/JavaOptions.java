package org.myrobotlab.java;

import edu.rice.cs.dynamicjava.Options;

public class JavaOptions extends Options {
	@Override
	public boolean requireVariableType() {
		return false;
	}

	@Override
	public boolean enforceAllAccess() {
		return false;
	}

	@Override
	public boolean enforcePrivateAccess() {
		return false;
	}

	@Override
	public boolean prohibitUncheckedCasts() {
		return false;
	}

	@Override
	public boolean prohibitBoxing() {
		return false;
	}

	@Override
	public boolean requireSemicolon() {
		return false;
	}

}
