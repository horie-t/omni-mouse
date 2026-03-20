package com.t_horie.omni_mouse;

import com.pi4j.Pi4J;
import com.pi4j.io.gpio.digital.DigitalState;

public class OmniMouseApp {
    private static final int PIN_LED = 25;

    public static void main( String[] args ) {
        var pi4j = Pi4J.newAutoContext();

        try (var led = pi4j.digitalOutput().create(PIN_LED);){
            for (int i = 0; i < 10; i++) {
                if (led.state() == DigitalState.HIGH) {
                    led.low();
                } else {
                    led.high();
                }
                    Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        pi4j.shutdown();
    }
}
