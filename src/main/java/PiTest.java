import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class PiTest {

  public static void main(String[] args) throws InterruptedException {
    // Create a GPIO controller
    GpioController gpio = GpioFactory.getInstance();

    // Provision a GPIO pin as a digital multipurpose pin
//    GpioPinDigitalMultipurpose pin = gpio.provisionDigitalMultipurposePin(
//            RaspiPin.GPIO_18, PinMode.DIGITAL_INPUT);
    
//    GpioPinDigitalMultipurpose pin = gpio.provisionDigitalMultipurposePin(
//        RaspiPin.GPIO_18, PinMode.DIGITAL_OUTPUT);
    
  GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin( RaspiPin.GPIO_18);

    
    // Read initial state from the pin
    PinState initialState = pin.getState();
    System.out.println("Initial state: " + initialState);

    // Switch the pin to output mode
    // pin.setMode(PinMode.DIGITAL_OUTPUT);
    System.out.println("Switched to output mode");

    for (int i = 0; i < 100; ++i) {
    // Write a HIGH value to the pin
    pin.high();
    System.out.println("Pin set to HIGH x");

    // Pause for a while
    Thread.sleep(5000);

    // Write a LOW value to the pin
    pin.low();
    System.out.println("Pin set to LOW x");

    // Pause for a while
    Thread.sleep(5000);
    }

    
    // Switch the pin back to input mode
//    pin.setMode(PinMode.DIGITAL_INPUT);
//    System.out.println("Switched back to input mode");

    
    // Cleanup
    gpio.shutdown();
}
}
