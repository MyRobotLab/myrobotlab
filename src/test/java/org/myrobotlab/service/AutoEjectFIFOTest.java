package org.myrobotlab.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.test.AbstractTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class AutoEjectFIFOTest extends AbstractTest {

    private AutoEjectFIFO fifo;
    private TestCatcher catcher;


    @Before
    public void createService() throws Exception {
        fifo = (AutoEjectFIFO) Runtime.start("fifo", "AutoEjectFIFO");
        catcher = (TestCatcher) Runtime.start("catcher", "TestCatcher");
        catcher.clear();
        fifo.clear();
    }

    @After
    public void releaseService() {
        Runtime.release(fifo.getFullName());
        Runtime.release(catcher.getFullName());
    }

    @Test
    public void testAdd10() throws IOException, InterruptedException {
        catcher.subscribe(fifo.getFullName(), "publishItemAdded", "onInteger");
        sleep(50);
        List<Integer> ints = new ArrayList<>();
        for (int i = 0; i < 10; i ++) {
            fifo.add(i);
            ints.add(i);
        }
        catcher.waitForMsgs(10, 2000);
        assertEquals(10, catcher.integers.size());
        assertEquals(0, fifo.getHead());
        // Last element was 9 since we added 0-9, not 1-10
        assertEquals(9, fifo.getTail());
        assertArrayEquals(ints.toArray(), fifo.getAll().toArray());
    }

    @Test
    public void testAddMax() throws IOException, InterruptedException {
        catcher.subscribe(fifo.getFullName(), "publishItemAdded", "onInteger");
        sleep(50);
        for (int i = 0; i < AutoEjectFIFO.DEFAULT_MAX_SIZE; i ++) {
            fifo.add(i);
        }
        catcher.waitForMsgs(AutoEjectFIFO.DEFAULT_MAX_SIZE, 2000);
        assertEquals(AutoEjectFIFO.DEFAULT_MAX_SIZE, catcher.integers.size());
        assertEquals(0, fifo.getHead());
        assertEquals(AutoEjectFIFO.DEFAULT_MAX_SIZE - 1, fifo.getTail());
    }

    @Test
    public void testAddMaxPlusOne() throws IOException, InterruptedException {
        catcher.subscribe(fifo.getFullName(), "publishItemAdded", "onInteger");
        catcher.subscribe(fifo.getFullName(), "publishEviction", "onObject");
        sleep(50);
        for (int i = 0; i < AutoEjectFIFO.DEFAULT_MAX_SIZE + 1; i ++) {
            fifo.add(i);
        }
        catcher.waitForMsgs(AutoEjectFIFO.DEFAULT_MAX_SIZE + 2, 2000);
        assertEquals(AutoEjectFIFO.DEFAULT_MAX_SIZE + 1, catcher.integers.size());
        assertEquals(1, catcher.objects.size());

        assertEquals(1, fifo.getHead());
        assertEquals(AutoEjectFIFO.DEFAULT_MAX_SIZE, fifo.getTail());
    }

    @Test
    public void testAddMaxPlusTwo() throws IOException, InterruptedException {
        catcher.subscribe(fifo.getFullName(), "publishItemAdded", "onInteger");
        catcher.subscribe(fifo.getFullName(), "publishEviction", "onObject");
        sleep(50);
        for (int i = 0; i < AutoEjectFIFO.DEFAULT_MAX_SIZE + 2; i ++) {
            fifo.add(i);
        }

        // Two more adds plus 2 evictions
        catcher.waitForMsgs(AutoEjectFIFO.DEFAULT_MAX_SIZE + 4, 2000);
        assertEquals(AutoEjectFIFO.DEFAULT_MAX_SIZE + 2, catcher.integers.size());
        assertEquals(2, catcher.objects.size());

        assertEquals(2, fifo.getHead());
        assertEquals(AutoEjectFIFO.DEFAULT_MAX_SIZE + 1, fifo.getTail());
    }
}
