package org.myrobotlab.client;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

public class ExampleServiceDiscovery {

    private static class SampleListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
            System.out.println("Service added: " + event.getInfo());
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            System.out.println("Service removed: " + event.getInfo());
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            System.out.println("Service resolved: " + event.getInfo());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        try {
            // Create a JmDNS instance
          // JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
          // JmDNS jmdns = JmDNS.create();
          JmDNS jmdns = JmDNS.create(InetAddress.getByName("192.168.0.102"),InetAddress.getLocalHost().getHostName());

            // Add a service listener
            // jmdns.addServiceListener(type, listener);
          jmdns.addServiceListener("_myrobotlab._tcp.local.", new SampleListener());
          // jmdns.addServiceListener("_googlecast._tcp.local.", new SampleListener());

            // Wait a bit
            Thread.sleep(30000);
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}