
	public synchronized void %name%(%javaMethodParameters%) {
		try {
		  if (ackEnabled){
		    waitForAck();
		  }		  
			write(MAGIC_NUMBER);
			write(%javaWriteMsgSize%); // size
%javaWrite% 
     if (ackEnabled){
       // we just wrote - block threads sending
       // until they get an ack
       ackRecievedLock.acknowledged = false;
     }
%javaSendRecord%
	  } catch (Exception e) {
	  			log.error("%name% threw",e);
	  }
	}
