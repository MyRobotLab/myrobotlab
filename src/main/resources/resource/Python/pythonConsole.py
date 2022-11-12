import sys

class Console:

  def __init__(self):
    self.stdout = None
    self.stderr = None
    
  def write(self,string):
    if myService is not None:
        myService.invoke(u'publishStdOut', string)
    
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
