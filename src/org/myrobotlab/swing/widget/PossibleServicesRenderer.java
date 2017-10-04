package org.myrobotlab.swing.widget;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

// FIXME - checkingForUpdates needs to process ? versus display current
// ServiceTypes
public class PossibleServicesRenderer extends DefaultTableCellRenderer {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(PossibleServicesRenderer.class);
  Runtime runtime;
  
  public PossibleServicesRenderer(Runtime runtime){
    this.runtime = runtime;
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {

    Repo repo = runtime.getRepo();
    setEnabled(table == null || table.isEnabled());
    Boolean availableToInstall = null;

    boolean upgradeAvailable = false;

    String upgradeString = "<html><h6>upgrade<br>";
    
    if (value == null) {
      return this;
    }

    // select by class being published by JTable on how to display
    if (value.getClass().equals(ServiceType.class)) {
      ServiceType entry = (ServiceType) table.getValueAt(row, 0);
      setHorizontalAlignment(SwingConstants.LEFT);
      setIcon(Util.getScaledIcon(Util.getImage((entry.getSimpleName() + ".png"), "unknown.png"), 0.30));
      setText(entry.getSimpleName());
      // setToolTipText("<html><body bgcolor=\"#E6E6FA\">" +
      // entry.type+
      // " <a href=\"http://myrobotlab.org\">blah</a></body></html>");

    } else if (value instanceof ServiceInterface) {
      ServiceInterface entry = (ServiceInterface) table.getValueAt(row, 0);
      setHorizontalAlignment(SwingConstants.LEFT);
      setIcon(Util.getScaledIcon(Util.getImage((entry.getSimpleName() + ".png"), "unknown.png"), 0.50));
      setText(entry.getSimpleName());

    } else if (value.getClass().equals(String.class)) {
      ServiceType entry = (ServiceType) table.getValueAt(row, 0);
      availableToInstall = repo.isServiceTypeInstalled(entry.getName());

      setIcon(null);
      setHorizontalAlignment(SwingConstants.LEFT);

      if (!availableToInstall) {
        setText("<html><h6>not<br>installed&nbsp;</h6></html>");
      } else {
        if (upgradeAvailable) {
          setText(upgradeString);
        } else {
          setText("<html><h6>latest&nbsp;</h6></html>");
        }
      }

    } else {
      log.error("unknown class");
    }

    if (table.isRowSelected(row)) {
      setBackground(Style.listHighlight);
      setForeground(Style.listForeground);
    } else {

      ServiceType entry = (ServiceType) table.getValueAt(row, 0);
      availableToInstall = repo.isServiceTypeInstalled(entry.getName());

      if (!availableToInstall) {
        setForeground(Style.listForeground);
        setBackground(Style.possibleServicesNotInstalled);
      } else {
        if (upgradeAvailable) {
          setForeground(Style.listForeground);
          setBackground(Style.possibleServicesUpdate);
        } else {
          setForeground(Style.listForeground);
          setBackground(Style.possibleServicesStable);
        }
      }
    }

    // setBorder(BorderFactory.createEmptyBorder());

    return this;
  }
}
