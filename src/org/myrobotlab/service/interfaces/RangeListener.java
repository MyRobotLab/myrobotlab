package org.myrobotlab.service.interfaces;

public interface RangeListener extends NameProvider {

	public void onRange(Long range);

	public void setUnitCm();

	public void setUnitInches();

}
