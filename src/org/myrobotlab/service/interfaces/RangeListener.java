package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;

public interface RangeListener extends NameProvider {
	public void onRange(Double range);
}
