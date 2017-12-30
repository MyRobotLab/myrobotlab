/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.opencv;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/*
 * Basic Grouping algorithm -
 * I am a point.  If my a neighbor is within a color difference I will join her group. 
 * If my neighbors are within a color difference and they belong to different groups, 
 * join those groups together
 * 
 * TODO : 
 * Optimizations:
 * Joins optimized on larger set of points
 * Joins are very expensive - a possible optimization is to do more creation on initialization (throw away) data in summary
 * Final all
 * 
 * Variations on Algorithm:
 * 1. use || instead of && for delta
 * 2. use single channel vs 3 RGB channels
 * 3. use group average for match vs neighbor
 * 4. join Only vertically or horizontally no diagonals
 * 5. use memory and search only only the delta of difference on edges
 * 
 * References:
 * http://ubaa.net/shared/processing/opencv/ <--- Great Site - guy wrote java bridge to opencv - great links too
 * http://www.openframeworks.cc/
 * http://processing.org/discourse/yabb2/YaBB.pl?num=1237253804 - video feed openCV only support a few camera types
 * ... uh wow !.....
 * yum install opencv 
 * http://www.processing.org/ - interesting it appears they wrote the bridge to opencv++
 * http://walkintothefuture.blogspot.com/2009/04/opencv-java-linux.html - face capture guy
 * http://opencv.willowgarage.com/wiki/cvBlobsLib - blob detection in c++
 * http://www-2.cs.cmu.edu/~jbruce/cmvision/ cmu
 * http://www.morethantechnical.com/2009/05/14/combining-javas-bufferedimage-and-opencvs-iplimage/
 */

public final class FilterColorGrouping {

  final public class Group {
    public int number; // externally keyed or put into list - INDEX !!!
    public Color avgColor = null;
    public int intColor = 0;
    public Rectangle boundingBox = new Rectangle();
    public ArrayList<PointReference> points = new ArrayList<PointReference>();
    // ArrayList Map or 2D Array of PointReferences or Points?
  }

  final public class PointReference {
    public Group group = null; // TODO - BACKPOINTER TOO costly to maintain
    // - no worth at the moment
    int x;
    int y;

    PointReference(int x, int y) {
      this.x = x;
      this.y = y;
    }
  }

  public final static Logger log = LoggerFactory.getLogger(FilterColorGrouping.class.getCanonicalName());
  Rectangle target = null;
  PointReference[][] grid = null;
  ArrayList<Group> groupList = null;
  int stepx = 0;
  int stepy = 0;
  int xTotal = 0;
  int yTotal = 0;

  int groupDelta = 0;
  int colorInt = 0;
  int red = 0;
  int green = 0;

  int blue = 0;

  int lastColor = 0;
  int ncolorInt = 0;
  int nred = 0;
  int ngreen = 0;

  int nblue = 0;
  int redGroupDelta = 40;
  int greenGroupDelta = 40;

  int blueGroupDelta = 40;

  public FilterColorGrouping(String CFGRoot, String name) {
    // super(CFGRoot, name);
  }

  public BufferedImage display(IplImage image) {
    // TODO Auto-generated method stub
    return null;
  }

  public String getDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  public void init() {
    // TODO Auto-generated method stub

  }

  public void loadDefaultConfiguration() {

    // initialization
    for (int x = 0; x < xTotal; ++x) {
      for (int y = 0; y < yTotal; ++y) {
        grid[x][y] = new PointReference(x, y);
      }
    }

    groupList = new ArrayList<Group>();
  }

  public Object process(BufferedImage output, BufferedImage image) {
    // TODO pre-allocate in init or constructor (init?)?

    // Graphics2D g = output.createGraphics();
    // g.setColor(new Color(cfg.getInt("target.color")));
    // g.fillRect(cfg.getInt("target.x"), cfg.getInt("target.y"),
    // cfg.getInt("target.width"), cfg.getInt("target.height"));
    boolean isGrouped = false;
    int objectGroupID = 0;

    for (int y = 0; y < yTotal; ++y) {
      for (int x = 0; x < xTotal; ++x) {
        // TODO - use null test vs isGrouped
        // look for group in upper Tao
        // image.setRGB(x * stepx, y * stepy, 0xFFFFFF);

        /*
         * Upper Tao is checked C C C C X O O O O
         */

        lastColor = colorInt;
        colorInt = image.getRGB(x, y);
        red = (colorInt >> 16) & 0xFF; // Isolate the Red offset and
        // assign it to red
        green = (colorInt >> 8) & 0xFF; // Isolate the Green offset and
        // assign it to green
        blue = colorInt & 0xFF; // Isolate the Blue offset and assign it
        // to blue

        /*
         * if (lastColor != colorInt) { log.info("group size " +
         * groupList.size() + " @ (" + x + "," + y + ") color red " + ((colorInt
         * >> 16) & 0xFF) + " green " + ((colorInt >> 8) & 0xFF) + " blue " +
         * ((colorInt) & 0xFF)); for (int i = 0; i < groupList.size(); ++i) {
         * Group group = groupList.get(i); log.info("number " + group.number);
         * log.info("color red " + ((group.intColor >> 16) & 0xFF) + " green " +
         * ((group.intColor >> 8) & 0xFF) + " blue " + ((group.intColor) &
         * 0xFF)); log.info("bounding box " + group.boundingBox); log.info(
         * "points " +group.points.size()); }
         * 
         * }
         */

        isGrouped = false;

        /*
         * O O O C X O O O O
         */
        if (x != 0) {
          ncolorInt = image.getRGB(x - 1, y); // TODO - && vs || -
          // single channel vs 3
          if (Math.abs(((ncolorInt >> 16) & 0xFF) - red) < redGroupDelta && Math.abs(((ncolorInt >> 8) & 0xFF) - green) < greenGroupDelta
              && Math.abs(((ncolorInt) & 0xFF) - blue) < blueGroupDelta) {
            // add to group - first check doesn't have to worry
            // about joins
            Group neighborGroup = grid[x - 1][y].group;
            grid[x][y].group = neighborGroup;
            neighborGroup.points.add(grid[x][y]);
            // adjust bounding box
            if (x > neighborGroup.boundingBox.x + neighborGroup.boundingBox.width) {
              neighborGroup.boundingBox.width = +x - neighborGroup.boundingBox.x;
            }

            isGrouped = true;
          }
        }

        /*
         * C O O O X O O O O
         */
        if (y != 0 && x != 0) {
          ncolorInt = image.getRGB(x - 1, y - 1); // TODO - && vs || -
          // single channel vs
          // 3
          if (Math.abs(((ncolorInt >> 16) & 0xFF) - red) < redGroupDelta && Math.abs(((ncolorInt >> 8) & 0xFF) - green) < greenGroupDelta
              && Math.abs(((ncolorInt) & 0xFF) - blue) < blueGroupDelta) {
            Group myGroup = grid[x][y].group;
            Group neighborGroup = grid[x - 1][y - 1].group;
            if ((isGrouped) && myGroup != neighborGroup) {
              // OPTIMIZATON - THIS WILL NEVER BE ENTERED
              // i'm already grouped - my neighbor is not my group
              // - our groups should join
              for (int i = 0; i < neighborGroup.points.size(); ++i) {
                neighborGroup.points.get(i).group = myGroup; // TODO
                // OPTIMIZE
                // (OUCH)
              }
              if (neighborGroup.boundingBox.x < myGroup.boundingBox.x) {
                myGroup.boundingBox.width += neighborGroup.boundingBox.x - myGroup.boundingBox.x;
                myGroup.boundingBox.x = neighborGroup.boundingBox.x;
              }
              if (neighborGroup.boundingBox.x + neighborGroup.boundingBox.width > myGroup.boundingBox.x + myGroup.boundingBox.width) {
                myGroup.boundingBox.width = neighborGroup.boundingBox.x + neighborGroup.boundingBox.width - myGroup.boundingBox.x;
              }
              if (neighborGroup.boundingBox.y < myGroup.boundingBox.y) {
                myGroup.boundingBox.height += neighborGroup.boundingBox.y - myGroup.boundingBox.y;
                myGroup.boundingBox.y = neighborGroup.boundingBox.y;
              }
              if (neighborGroup.boundingBox.y + neighborGroup.boundingBox.height > myGroup.boundingBox.y + myGroup.boundingBox.height) {
                myGroup.boundingBox.height = neighborGroup.boundingBox.y + neighborGroup.boundingBox.height - myGroup.boundingBox.y;
              }
              myGroup.points.addAll(neighborGroup.points);
              groupList.remove(neighborGroup);
            } else if (!isGrouped) {
              // i will join my neighbor
              grid[x][y].group = neighborGroup;
              neighborGroup.points.add(grid[x][y]);
              // adjust bounding box
              if (x > neighborGroup.boundingBox.x + neighborGroup.boundingBox.width) {
                neighborGroup.boundingBox.width = x - neighborGroup.boundingBox.x;
              }

              if (y > neighborGroup.boundingBox.y + neighborGroup.boundingBox.height) {
                neighborGroup.boundingBox.height = y - neighborGroup.boundingBox.y;
              }

            }
            isGrouped = true;
          }
        }

        /*
         * O C O O X O O O O
         */
        if (y != 0) {
          ncolorInt = image.getRGB(x, y - 1); // TODO - && vs || -
          // single channel vs 3
          if (Math.abs(((ncolorInt >> 16) & 0xFF) - red) < redGroupDelta && Math.abs(((ncolorInt >> 8) & 0xFF) - green) < greenGroupDelta
              && Math.abs(((ncolorInt) & 0xFF) - blue) < blueGroupDelta) {
            Group myGroup = grid[x][y].group;
            Group neighborGroup = grid[x][y - 1].group;
            if ((isGrouped) && myGroup != neighborGroup) {
              // i'm already grouped - my neighbor is not my group
              // - our groups should join
              for (int i = 0; i < neighborGroup.points.size(); ++i) {
                neighborGroup.points.get(i).group = myGroup; // TODO
                // OPTIMIZE
                // (OUCH)
              }
              if (neighborGroup.boundingBox.x < myGroup.boundingBox.x) {
                myGroup.boundingBox.width += neighborGroup.boundingBox.x - myGroup.boundingBox.x;
                myGroup.boundingBox.x = neighborGroup.boundingBox.x;
              }
              if (neighborGroup.boundingBox.x + neighborGroup.boundingBox.width > myGroup.boundingBox.x + myGroup.boundingBox.width) {
                myGroup.boundingBox.width = neighborGroup.boundingBox.x + neighborGroup.boundingBox.width - myGroup.boundingBox.x;
              }
              if (neighborGroup.boundingBox.y < myGroup.boundingBox.y) {
                myGroup.boundingBox.height += neighborGroup.boundingBox.y - myGroup.boundingBox.y;
                myGroup.boundingBox.y = neighborGroup.boundingBox.y;
              }
              if (neighborGroup.boundingBox.y + neighborGroup.boundingBox.height > myGroup.boundingBox.y + myGroup.boundingBox.height) {
                myGroup.boundingBox.height = neighborGroup.boundingBox.y + neighborGroup.boundingBox.height - myGroup.boundingBox.y;
              }

              myGroup.points.addAll(neighborGroup.points);
              groupList.remove(neighborGroup);
            } else if (!isGrouped) {
              // i will join my neighbor
              grid[x][y].group = neighborGroup;
              neighborGroup.points.add(grid[x][y]);
              // adjust bounding box
              if (y > neighborGroup.boundingBox.y + neighborGroup.boundingBox.height) {
                neighborGroup.boundingBox.height = y - neighborGroup.boundingBox.y;
              }

            } // else - i am grouped and my neighbor is in the same
              // broup
            isGrouped = true;
          }
        }

        /*
         * O O C O X O O O O
         */
        if (y != 0 && x != xTotal - 1) {
          ncolorInt = image.getRGB(x + 1, y - 1); // TODO - && vs || -
          // single channel vs
          // 3
          if (Math.abs(((ncolorInt >> 16) & 0xFF) - red) < redGroupDelta && Math.abs(((ncolorInt >> 8) & 0xFF) - green) < greenGroupDelta
              && Math.abs(((ncolorInt) & 0xFF) - blue) < blueGroupDelta) {
            Group myGroup = grid[x][y].group;
            Group neighborGroup = grid[x + 1][y - 1].group;
            if ((isGrouped) && myGroup != neighborGroup) {
              // i'm already grouped - my neighbor is not my group
              // - our groups should join
              for (int i = 0; i < neighborGroup.points.size(); ++i) {
                neighborGroup.points.get(i).group = myGroup; // TODO
                // OPTIMIZE
                // (OUCH)
              }
              if (neighborGroup.boundingBox.x < myGroup.boundingBox.x) {
                myGroup.boundingBox.width += neighborGroup.boundingBox.x - myGroup.boundingBox.x;
                myGroup.boundingBox.x = neighborGroup.boundingBox.x;
              }
              if (neighborGroup.boundingBox.x + neighborGroup.boundingBox.width > myGroup.boundingBox.x + myGroup.boundingBox.width) {
                myGroup.boundingBox.width = neighborGroup.boundingBox.x + neighborGroup.boundingBox.width - myGroup.boundingBox.x;
              }
              if (neighborGroup.boundingBox.y < myGroup.boundingBox.y) {
                myGroup.boundingBox.height += neighborGroup.boundingBox.y - myGroup.boundingBox.y;
                myGroup.boundingBox.y = neighborGroup.boundingBox.y;
              }
              if (neighborGroup.boundingBox.y + neighborGroup.boundingBox.height > myGroup.boundingBox.y + myGroup.boundingBox.height) {
                myGroup.boundingBox.height = neighborGroup.boundingBox.y + neighborGroup.boundingBox.height - myGroup.boundingBox.y;
              }

              myGroup.points.addAll(neighborGroup.points);
              groupList.remove(neighborGroup);
            } else if (!isGrouped) {
              // i will join my neighbor
              grid[x][y].group = neighborGroup;
              neighborGroup.points.add(grid[x][y]);
              // adjust bounding box
              if (x < neighborGroup.boundingBox.x) {
                neighborGroup.boundingBox.width += neighborGroup.boundingBox.x - x;
                neighborGroup.boundingBox.x = x;
              }

              if (y > neighborGroup.boundingBox.y + neighborGroup.boundingBox.height) {
                neighborGroup.boundingBox.height = y - neighborGroup.boundingBox.y;
              }

            } // else - i am grouped and my neighbor is in the same
              // broup
            isGrouped = true;
          }
        }

        // at this point if I have found no group to join - create a new
        // group and join it
        if (!isGrouped) {
          ++objectGroupID;
          Group group = new Group();
          grid[x][y].group = group; // point to new group
          // myGroup = group; // point to new group
          group.points.add(grid[x][y]); // add myself to it
          group.intColor = image.getRGB(x, y); // TODO - join averages
          // and other average
          group.boundingBox.x = x;
          group.boundingBox.y = y;
          group.number = objectGroupID;
          groupList.add(group);
          isGrouped = true;
        }
      } // x

    } // y

    Graphics2D g = output.createGraphics();
    log.info("groupList size {}", groupList.size());
    for (int i = 0; i < groupList.size(); ++i) {
      Group group = groupList.get(i);
      /*
       * log.info("number " + group.number); log.info("color red " +
       * ((group.intColor >> 16) & 0xFF) + " green " + ((group.intColor >> 8) &
       * 0xFF) + " blue " + ((group.intColor) & 0xFF)); log.info("bounding box "
       * + group.boundingBox); log.info("points " +group.points.size());
       */
      g.setColor(Color.yellow);
      g.drawRect(group.boundingBox.x, group.boundingBox.y, group.boundingBox.width, group.boundingBox.height);
      g.drawString("" + group.number, group.boundingBox.x + 4, group.boundingBox.y + 11);

    }

    return null;
  }

  public IplImage process(IplImage image, Object[] data) {
    // TODO Auto-generated method stub
    return null;
  }

}
