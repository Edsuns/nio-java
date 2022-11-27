package io.github.edsuns.nio;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Objects.requireNonNull;

/**
 * @author edsuns@qq.com
 * @since 2022/11/27 15:09
 */
public class Blocking<E> {

    /**
     * Number of elements in the queue
     */
    E value;

    /*
     * Concurrency control uses the classic two-condition algorithm
     * found in any textbook.
     */

    /**
     * Main lock guarding all access
     */
    final ReentrantLock lock;

    /**
     * Condition for waiting takes
     */
    private final Condition notEmpty;

    /**
     * Condition for waiting puts
     */
    private final Condition notFull;

    public Blocking() {
        this(false);
    }

    public Blocking(boolean fair) {
        this.value = null;
        this.lock = new ReentrantLock(fair);
        this.notEmpty = lock.newCondition();
        this.notFull = lock.newCondition();
    }

    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (value == null) ? null : dequeue();
        } finally {
            lock.unlock();
        }
    }

    public E take() throws InterruptedException {
        return peek(true);
    }

    public E peek() throws InterruptedException {
        return peek(false);
    }

    public E peek(boolean remove) throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (value == null)
                notEmpty.await();
            return remove ? dequeue() : value;
        } finally {
            lock.unlock();
        }
    }

    public void put(E val) throws InterruptedException {
        requireNonNull(val);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (value != null)
                notFull.await();
            enqueue(val);
        } finally {
            lock.unlock();
        }
    }

    private void enqueue(E val) {
        value = val;
        notEmpty.signal();
    }

    private E dequeue() {
        E val = value;
        value = null;
        notFull.signal();
        return val;
    }
}
