import sys
from javax.swing import JFrame
from javax.swing import JPanel
from javax.swing import JLabel
from javax.swing import JTextArea
from javax.swing import JScrollPane
from javax.swing import JTabbedPane
from javax.swing import WindowConstants
from java.awt import BorderLayout
from java.lang import Boolean

# myService is a local variable
# created to point to this Python
# service

class Console:

  def __init__(self):
    self.stdout = None
    self.stderr = None
#    frame = JFrame("Python Console")
#    frame.setSize(400, 300)
#    frame.setLayout(BorderLayout())

#    tabPane = JTabbedPane(JTabbedPane.TOP)

#    label = JLabel("<html><br>This is a tab1</html>")
#    panel1 = JPanel(BorderLayout())
#    panel1.add(label)
#    log = JTextArea(20, 40)
#    log.setEditable(Boolean("false"))
#    self.log = log
#    panel1.add(JScrollPane(log))
#    tabPane.addTab("console", panel1)

    # panel2 = JPanel()
    # panel2.add(label2)
    # tabPane.addTab("errors", panel2)

#    frame.add(tabPane)
    #frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
#    frame.setVisible(True)

  def write(self,string):
    myService.invoke("publishStdOut", string)
    #myService.publishStdOut(string)
    #self.log.append(string)
    
  def attach(self):
    if (self.stdout == None):    
      self.stdout = sys.stdout
      self.stderr = sys.stderr
      sys.stdout = self
      sys.stderr = self
   
  def detach(self):
    if (self.stdout != None):
      sys.stdout = self.stdout
      sys.stderr = self.stderr
      self.stdout = None
      self.stderr = None
    
console = Console()
console.attach()