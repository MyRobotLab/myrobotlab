
	public void %name%(%javaMethodParameters%) {
		try {
			write(MAGIC_NUMBER);
			write(%javaWriteMsgSize%); // size
%javaWrite% 
%javaSendRecord%
	  } catch (Exception e) {
	  			serial.error(e);
	  }
	}
