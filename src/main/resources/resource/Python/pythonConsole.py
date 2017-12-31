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
    
  def write(self,string):
    myService.invoke("publishStdOut", string)
    
  def attach(self):
    if (self.stdout == None):    
      self.stdout = sys.stdout
      self.stderr = sys.stderr
      sys.stdout = self
      sys.stderr = self
   
  def flush(self):
    pass
    
  def detach(self):
    if (self.stdout != None):
      sys.stdout = self.stdout
      sys.stderr = self.stderr
      self.stdout = None
      self.stderr = None
    
console = Console()
console.attach()