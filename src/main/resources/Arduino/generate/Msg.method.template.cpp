
void Msg::%name%(%cppMethodParameters%) {
  write(MAGIC_NUMBER);
  write(%cppMsgSize%); // size
%cppWrite%  flush();
  reset();
}
