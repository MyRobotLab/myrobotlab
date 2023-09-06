package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.config.ServiceConfig;

import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A simple service that acts as a circular FIFO queue.
 * This queue can store a number of items, but once
 * its max capacity is reached, any attempt to add more
 * items ejects the oldest element, i.e. the head.
 * <p></p>
 * This queue is not typed, i.e. it can store any type
 * of object, with the downside that no type checking is
 * performed. This is to allow the fifo to be used
 * in any situation, since we don't currently have a way
 * to create generic services.
 *
 * @author AutonomicPerfectionist
 */
public class AutoEjectFIFO extends Service<ServiceConfig> {
    public static final int DEFAULT_MAX_SIZE = 50;


    /**
     * Lock used to protect the fifo queue,
     * used instead of synchronized block to allow
     * multiple simultaneous readers so long as there
     * is no writer writing to the queue.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * The actual queue, whose initial maximum size is set to
     * {@link #DEFAULT_MAX_SIZE}.
     */
    private BlockingDeque<Object> fifo = new LinkedBlockingDeque<>(DEFAULT_MAX_SIZE);


    /**
     * Constructor of service, reservedkey typically is a services name and inId
     * will be its process id
     *
     * @param reservedKey the service name
     * @param inId        process id
     */
    public AutoEjectFIFO(String reservedKey, String inId) {
        super(reservedKey, inId);
    }


    /**
     * Sets the size at which the FIFO will begin evicting
     * elements. If smaller than the current number of items,
     * then elements will be silently evicted.
     * @param size The new max size
     */
    public void setMaxSize(int size) {
        lock.writeLock().lock();
        BlockingDeque<Object> newFifo = new LinkedBlockingDeque<>(size);
        newFifo.addAll(fifo);
        fifo = newFifo;
        lock.writeLock().unlock();
    }

    /**
     * Add a new element to the FIFO, if
     * it's full then this will trigger an
     * eviction
     * @param item The new item to be added to the tail
     */
    public void add(Object item) {
        lock.writeLock().lock();
        try {
            if (!fifo.offer(item)) {
                Object head = fifo.removeFirst();
                invoke("publishEviction", head);
                fifo.add(item);
            }
            invoke("publishItemAdded", item);
        } catch (Exception e) {
            error(e);
        } finally {
            lock.writeLock().unlock();
        }


    }

    public void clear() {
        lock.writeLock().lock();
        fifo.clear();
        lock.writeLock().unlock();
        invoke("publishClear");
    }

    public List<Object> getAll() {
        lock.readLock().lock();
        List<Object> ret = List.copyOf(fifo);
        lock.readLock().unlock();
        invoke("publishAll", ret);

        return ret;
    }

    public Object getHead() {
        lock.readLock().lock();
        Object head = fifo.peek();
        lock.readLock().unlock();
        invoke("publishHead", head);
        return head;

    }

    public Object getTail() {
        lock.readLock().lock();
        Object tail = fifo.peekLast();
        lock.readLock().unlock();
        invoke("publishTail", tail);
        return tail;
    }

    public Object publishItemAdded(Object item) {
        return item;
    }

    public void publishClear() {
        // Do nothing
    }

    public List<Object> publishAll(List<Object> items) {
        return items;
    }

    public Object publishHead(Object head) {
        return head;
    }

    public Object publishTail(Object tail) {
        return tail;
    }

    public Object publishEviction(Object evicted) {
        return evicted;
    }



}
