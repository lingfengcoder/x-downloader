package com.pukka.iptv.downloader.pool.threadpool;


import java.util.List;
import java.util.concurrent.*;

public class BizThreadPool extends AbstractExecutorService {

    private BizLinkedBlockingQueue<Runnable> queue;
    private ThreadPoolExecutor threadPoolExecutor;

    public static BizThreadPool buildThreadPool(int corePoolSize,
                                                int maxPoolSize,
                                                long keepAliveTime,
                                                int queueCapacity, ThreadFactory factory, RejectedExecutionHandler rejectedExecutionHandler) {
        BizThreadPool bizThreadPool = newInstance();
        bizThreadPool.createQueue(queueCapacity);
        bizThreadPool.preparedThreadPool(corePoolSize, maxPoolSize, keepAliveTime, factory, rejectedExecutionHandler);
        return bizThreadPool;
    }

    private static BizThreadPool newInstance() {
        return new BizThreadPool();
    }

    private void preparedThreadPool(int corePoolSize,
                                    int maxPoolSize,
                                    long keepAliveTime, ThreadFactory factory, RejectedExecutionHandler rejectedExecutionHandler) {
        ThreadPoolExecutor executor =
                new ThreadPoolExecutor(corePoolSize, maxPoolSize,
                        keepAliveTime, TimeUnit.MILLISECONDS, this.queue);
        executor.setRejectedExecutionHandler(rejectedExecutionHandler);
        executor.setThreadFactory(factory);
        this.threadPoolExecutor = executor;
    }

    private void createQueue(int queueCapacity) {
        this.queue = new BizLinkedBlockingQueue<>(queueCapacity);
        // return new SynchronousQueue<>();
    }

    //修改队列长队
    public void reSizeQueue(int queueCapacity) {
        this.queue.setCapacity(queueCapacity);
    }

    public void setCoreSize(int size) {
        this.threadPoolExecutor.setCorePoolSize(size);
    }

    public void setMaxSize(int size) {
        this.threadPoolExecutor.setMaximumPoolSize(size);
    }

    public void setKeepAliveTime(long time) {
        this.threadPoolExecutor.setKeepAliveTime(time, TimeUnit.MILLISECONDS);
    }

    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        this.threadPoolExecutor.setRejectedExecutionHandler(handler);
    }

    public void setThreadFactory(ThreadFactory factory) {
        this.threadPoolExecutor.setThreadFactory(factory);
    }

    public void preStartCoreThread() {
        this.threadPoolExecutor.prestartCoreThread();
    }

    public void preStartAllCoreThreads() {
        this.threadPoolExecutor.prestartAllCoreThreads();
    }

    public void allowCoreThreadTimeOut(boolean allow) {
        this.threadPoolExecutor.allowCoreThreadTimeOut(allow);
    }


    public ThreadPoolExecutor getRealPool() {
        return threadPoolExecutor;
    }

    @Override
    public void shutdown() {
        this.threadPoolExecutor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return this.threadPoolExecutor.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return this.threadPoolExecutor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return this.threadPoolExecutor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.threadPoolExecutor.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        this.threadPoolExecutor.execute(command);
    }
}
