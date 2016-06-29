/*
 *  Simbad - Robot Simulator
 *  Copyright (C) 2004 Louis Hugues
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 -----------------------------------------------------------------------------
 * $Author: sioulseuguh $ 
 * $Date: 2005/02/05 11:05:27 $
 * $Revision: 1.7 $
 * $Source: /cvsroot/simbad/src/simbad/gui/AgentFollower.java,v $
 */
package org.myrobotlab.mapper.gui;

import org.myrobotlab.mapper.sim.SimpleAgent;
import org.myrobotlab.mapper.sim.World;

/**
 * The AgentFollower class is a helper class for the AgentInspector. It updates
 * cyclically the world view point. in order to follow agent movements.
 * 
 */
class AgentFollower implements Runnable {

  World world;
  SimpleAgent agent;
  Thread thread;
  boolean stopped;
  boolean changed;
  int viewPointType;

  AgentFollower(World world, SimpleAgent agent) {
    this.agent = agent;
    this.world = world;
    viewPointType = World.VIEW_ABOVE_AGENT;
    stopped = true;
    thread = new Thread(this);
    thread.start();
  }

  protected void resume() {
    stopped = false;
    changed = true;
  }

  @Override
  public void run() {
    while (true) {
      changed = false;
      if (!stopped)
        world.changeViewPoint(viewPointType, agent);
      try {
        // don't need precise time tick
        // do multiple sleep to diminish ui latency
        for (int i = 0; i < 30; i++) {
          Thread.sleep(100);
          if (changed)
            break;
        }

      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  protected void setViewPointType(int type) {
    viewPointType = type;
  }

  protected void suspend() {
    stopped = true;
    changed = true;
  }
}