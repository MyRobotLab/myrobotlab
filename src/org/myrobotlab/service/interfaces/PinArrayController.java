package org.myrobotlab.service.interfaces;

import java.util.List;

public interface PinArrayController extends DeviceController {

	public List<PinDefinition> pinArrayGetPinList();
}
