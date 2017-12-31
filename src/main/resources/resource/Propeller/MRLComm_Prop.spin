{{
*
* @author Gareth aka Chiprobot.00@gmail.com
* File :- MRLComm_Prop_19.spin  
*
*
* MyRobotLab is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version (subject to the "Classpath" exception
* as provided in the LICENSE.txt file).
*
* MyRobotLab is distributed in the hope that it will be useful or fun,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* All libraries in thirdParty bundle are subject to their own license
* requirements.
*
* Enjoy !
*
* -----------------
* Purpose: translate serial commands into Propeller language commands,
* mostly relating to IO.  This would allow a computer to easily take
* advantage of Propellers great IO capabilities, while cpu number crunching
* could be done on the computer
*****************************************************************************

This is a Propeller version that clones itself as an Arduino Uno.
The advantage of using a Propeller is that it runs @80Mhz with 8 parallel cores.
Easily implemeted  is i2c , composite video and VGA outputs.

Only Basic Proof of concept actions are implemented so far:-
 (1) Digital Inputs/Outputs   
 (2) Analogs
 (3) Servos PWM

When compared to the Arduino the Propeller version in infact ends up with an extra 2 analogs and extra 14 digital I/Os

NB Coding :- DIRB and OUTB registers are a special case (programmers trick) as they are not implemented by the Propeller (Propeller uses DIRA & OUTA to control its IO and thats all it needs)
             As they are free registers they can be used for bit mapping ... its a real easy way to set and clear bits in a 32 bit word , so i have exploited them as flags in the programming
          :- At momment using some binary ie %0000_0101 notations make it easier to see whats happening ... the underscore is ignored by Propeller
Additional Hardware :-
An ADC chip is needed for the analog conversion....
Sigma/delta circuits could be used, however 2 pins per channel are required...so take easy way and choose the....  
Chip version details here :-
   +-------------------------------------------+           +---------+
   ¦      8 Channel 12-Bit Analog>Digital      ¦  Analog 0-?  M   Vdd?----- 3.3 Volt
   ¦ Designed for the MCP3X0X series of ADCs   ¦         1-?  C  VRef?-+
   ¦           Using the MCP3208 chip          ¦         2-?  P  AGnd?-+
   ¦      Channels are mapped from :-          ¦         3-?  3   Clk?-+--- pin  27
   ¦     14 to 21  (ie 20&21 are Bonus)        ¦         4-?  2  DOut?-+-+                              
   ¦      as Arduino has only 6 analogs        ¦         5-?  0  DIn ?-+-?- pin  26     
   +-------------------------------------------+         6-?  8   CS ?-+--- pin  25
                                                         7-?     DGnd?----- 0.0 Volt
                                                           +---------+            }}
CON
  _clkmode = xtal1 + pll16x
  _xinfreq = 5_000_000 ' Set up the clock for 80Mhz

  Vclk_p         = 27  ' clock pin  ' ADC setup requites 3 pins
  Vn_p           = 26  ' data in    ' Yes the data in/out can be tied together
  Vo_p           = 26  ' data out   '
  Vcs_p          = 25  ' CS pin     '

  MRLCommVersion = 9   ' Each update requires this to be ++
OBJ
  PST    : "ExtendedFullDupleSerial"      '      This Serial Handler enables you to put a timeout on the RX data (so no hang ups waiting for lost bytes)
  ADC    : "ADC_INPUT_DRIVER"             '      8 channel analog handler
  SERVO  : "Servo32v9.spin"               '      Gives ability to control 32 Servos with speed control - NB to detach a servo send it a wild out of range value.
VAR
Byte    MRL_Magic                         '      Store Magic number... General declarations, mostly global variables so its easier to exchange data between subroutines/cogs
Byte    MRL_SZ                            '      Size of following data packet ... at moment its set for a variable max of up to 4 bytes
Byte    MRL_FN                            '      Function ....What action to make
Byte    MRL_Data1                         '      1st data byte
Byte    MRL_Data2                         '      2nd data byte
Byte    MRL_Data3                         '      3rd data byte
Byte    RX_IN                             '      Received data buffer
Byte    RXTimeout                         '      Receive data time out, value is in milliseconds (ours is set to 10ms)

Word    Ana1                              '      As ADC chip is 12 bits the data needs to be spanned over two data bytes
Word    Ana2                              '      

Word    DigitalStream                     '      Which Digitals are TXed back
Word    DigitalChannel                    '      Propeller pin0-pin24           pins25>31 are used for analog control and basic RXTX comms
Word    DigitalRead                       '                                     Maybe a couple of pins more will be dedicated to i2c or neopixel control etc

Word    AnalogStream                      '      Which analogs are TXed back
Word    AnalogChannel                     '      %0000_1110  being the first analog    Propeller maps its analogs to these pins ie analog 0 = pin14
Word    AnalogRead                        '                                                                                        analog 1 = pin15 etc
Byte    Magic_Number
PUB Begin
  PST.Start(31,30,0,57600)                                   ' Start the Serial link to MRL  on pins 31(rx),30(tx) @57600 Baud
  ADC.start(Vo_p, Vn_p, Vclk_p, Vcs_p, 8, 8, 12, 1, false)   ' Start ADC = scan 8 channels, 12-bit ADC, mode 1
  SERVO.Start                                                ' Start Servo handler

  'DIRA[22..23]~~      ' debug 2 output watchdogs 23 = connected to MRL    22=data
  'OUTA[22]:=False     ' set debug led off
  'OUTA[23]:=False     ' set debug led off
 
  Magic_Number := %1010_1010                ' Magic control sequence
  RXTimeout    := 10                        ' Set the receive time out to 10ms .... so keeps things in motion (ie waiting for bytes that never come :-)
  repeat
 
    RX_IN :=  PST.RxTime( RXTimeout )       ' Look for the first byte of a payload
    if RX_IN == %10101010                   ' If its a "Magic" byte then process the rest
       MRLMagicFirst                        ' Call Serial IN routine to extract first bytes of data
       
      case MRL_FN
         %0001_1010 :                       ' Yes i am here MRL  so I am sending my HandShake and version back to you.
           PST.tx(Magic_Number)               ' Magic number
           PST.tx(%0000_0010)               ' 2 byte payload
           PST.tx(%0001_1010)               ' My MRL version is......
           PST.tx(MRLCommVersion)           ' current MRL communication version

         %0000_0100 :                       ' Digital pinmode direction cmd    ... without altering the actual output/input
           DIRA[MRL_Data1] := MRL_Data2     ' Set Digital to an input or output
                 
         %0000_0000 :                       ' Digital pinmode direction command ... with switch for digital pin on/off
           DIRA[MRL_Data1] := MRL_Data2     ' Set the DataDirection to OUTPUT   MRL_Data2 should be a logic "1"
           OUTA[MRL_Data1] := MRL_Data2
        
         %0000_1111 :                       ' start Digital Read Polling ..gives  Bi-direction data stream until told to stop.
           DIRA[MRL_Data1]:=MRL_Data2       ' Set the Pin as an INPUT    MRL_Data2 should be a logic "0"
           OUTB[MRL_Data1]:= 1              ' raise flag so this pin is included in the digital polling loop
           DigitalStream := OUTB[13..0]     ' Update the Digital loop stream (starts stream if at least one flag is raised,it no flag then no streaming)
         
         %0001_0000 :                       ' Read polling stop  ........AND (MRL_Data1 < %0000_1110)  PIN mode
           DIRA[MRL_Data1]:=MRL_Data2       ' Stop  Read polling on this pin
           OUTB[MRL_Data1]:= 0              ' remove pin from digital loop
           DigitalStream := OUTB[13..0]    
               
         %0000_1101 :                       ' Enable Analog cmd   can in this sequence 0,1,2,3,4,5,6 or 7
           OUTB[MRL_Data1]~~                ' on
           AnalogStream := OUTB[19..14]
                               
         %0000_1110 :                       ' Disable Analog cmd 0,1,2,3,4,5,6 or 7  
           OUTB[MRL_Data1]~                 ' off
           AnalogStream := OUTB[19..14]
            
         %0000_0110 :                       '  Servo command
           ServoAttach

         %0000_0111 :
           ServoPWM
                                                 
    if DigitalStream  > %0000_0000          '  Only service Digital Stream if needed
         DigitalLoop
     
    if AnalogStream   > %0000_0000          '  Only service Analog Stream if needed
         Analogloop
   'If you need to slow things down then :-      waitcnt(clkfreq/10 + cnt)  .... this may be cmd 27 ... i think...  
PUB MRLMagicFirst                           ' Messy routine.... until I implement Mega version...
  MRL_Magic := RX_IN                        ' Store Magic Byte
  RX_IN     :=  PST.RxTime(RXTimeout)
  MRL_SZ    := RX_IN                        ' Store Size of following Payload
   case MRL_SZ                                        
     %00000001 :                            ' If size = 1 byte call the store 1 byte routine   AA:01:(1A) for example
         MRL_FN    := PST.RxTime(RXTimeout)
     %00000010 :                            ' If size = 2 byte call the store 2 bytes routine  AA:02:(1A):(00) for example
         MRL_FN    := PST.RxTime(RXTimeout)
         MRL_Data1 := PST.RxTime(RXTimeout)
     %00000011 :                            ' If size = 3 byte call the store 3 bytes routine  AA:01:(1A):(00):(00) for example
         MRL_FN    := PST.RxTime(RXTimeout)
         MRL_Data1 := PST.RxTime(RXTimeout)
         MRL_Data2 := PST.RxTime(RXTimeout)
     %00000100 :                            ' If size = 4 byte call the store 4 bytes routine  AA:01:(1A):(00):(00) for example
         MRL_FN    := PST.RxTime(RXTimeout)
         MRL_Data1 := PST.RxTime(RXTimeout)
         MRL_Data2 := PST.RxTime(RXTimeout)
         MRL_Data3 := PST.RxTime(RXTimeout)
 
PUB AnalogLoop
    repeat AnalogChannel from 14 to 21                       ' 8 analogs are implemented as Propeller has 2 extra Bit of a messy loop ... as not sure what to do when converted to a Mega vesrsion
       if  OUTB[AnalogChannel] == 1                          ' Test our special register ...  If set then TX that analog
         AnalogRead := ADC.getval(AnalogChannel-%0000_1110)  ' -14 because Arduino analog starts at 14 and Propeller is starts at 0
         AnalogSend                                          ' So basically only send back an Analog that is Flagged for in the data stream
PUB AnalogSingle                                             ' Sending single Analogs back to MRL can be done by calling this routine.
      AnalogChannel :=MRL_Data1  ' 0E first analog
      AnalogRead := ADC.average(AnalogChannel-%0000_1110,10) '  ADC value of channel 0 (Averageing 100 values)  
      AnalogSend
PUB AnalogSend
           PST.tx(Magic_Number)                         'Send:- Magic number
           PST.tx(%0000_0100)                                ' 4 bata bytes to follow
           PST.tx(%0000_0011)                                ' this is analog data....
           PST.tx(AnalogChannel) ' Analog 0  = 0E            ' for analog number .....
           Ana1:= AnalogRead                                 ' read and store analog value
           Ana2:= ana1>>8                                    ' /256
           PST.tx(Ana2)                                      ' send high byte
           PST.tx(Ana1)                                      ' send low byte
PUB DigitalLoop
   repeat DigitalChannel from 0 to 13                        ' Set up Digital loop input stream  Bit of a messy loop ... as not sure what to do when converted to a Mega vesrsion
      if OUTB[DigitalChannel] == 1                           ' Test our special register ...
        DigitalRead := INA[DigitalChannel]                   ' if bit is set then TX that digital input
        DigitalSend                                          

'PUB DigitalSingle
   ' DigitalChannel := MRL_Data1 ' Inputpin 0
   ' DigitalRead    := INA[DigitalChannel ]     '
   ' DigitalSend
     'if INA[2] == 1
      '       OUTA[23]~~     
      '     else
      '       OUTA[23]~
PUB DigitalSend                                               
           PST.tx(Magic_Number)                          'Send:-   Magic number
           PST.tx(%0000_0011)                                 '   3 byte payload
           PST.tx(%0000_0001)                                 '   this is going to be a digital input
           PST.tx(DigitalChannel)                             '   off this digital pin
           PST.tx(DigitalRead)                                '   with a digital 1 or 0
PUB ServoAttach                                             
           Servo.Set(MRL_Data1+2,1500)                        '   attach a servo to this pin ....and errr center it for start
PUB ServoDeattach
           Servo.set(MRL_Data1+2,50000)                       '   Servo detatach is done by give a wild value
PUB ServoPWM                                                  '   send this pwm signal to selected servo
           Servo.set(MRL_Data1+2,MRL_Data2*20)   ' This *20 if a "fudge as MRL outputs degrees and not milliseconds

PUB Flashlight                                                '   Debug LEDS for development only connect to pins 22 and 23
     repeat 2
             !OUTA[22..23]              ' Invert LED condition on2off off2on a couple of times
              waitcnt(clkfreq/4 + cnt)  ' Blip a few lights to show activity    