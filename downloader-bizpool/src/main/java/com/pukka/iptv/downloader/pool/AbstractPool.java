package com.pukka.iptv.downloader.pool;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * @Author: wz
 * @Date: 2021/12/15 11:39
 * @Description: 连接池的抽象实现类　核心实现类　负责对子类提供　获取/归还/动态调整/关闭/开启/定时清理/配置查询等功能
 */
@Slf4j
public abstract class AbstractPool<K extends Key<K>, C, N extends Node<K, C>> implements Pool<K, N, PoolConfig<K, C, N>> {
    //不需要等待时间的标志
    protected final static long NO_TIME = -1L;
    //没有限制的标志
    protected final static int NO_LIMIT = -1;
    //用于多线程传递数据使用的临时变量区
    private final Map<String, N> awaitDataStore = new HashMap<>();
    //没有获取到连接的　等待队列 FIFO队列
    private final Map<K, ConditionNode> awaitQueue = new HashMap<>();
    //暂时不考虑为超量KEY建立索引  private volatile Map<K, K> keyIndex = new HashMap<>();
    //定时任务线程池
    private volatile ScheduledThreadPoolExecutor executor;
    private volatile ScheduledFuture<?> scheduledFuture;
    //连接池的配置，注意：禁止直接在代码里使用this.config 需要使用getConfig()
    //因为在多线程环境下，不能预测外部线程何时调用方法，为保证config单例，只能使用getConfig()
    private volatile PoolConfig<K, C, N> config;
    //对外提供查询配置信息的配置类,为防止外部直接修改配置造成不可预测的异常，在内部维护一个仅供查询的配置类
    private volatile PoolConfig<K, C, N> cacheConfig;
    //内部lock
    private final ReentrantLock INNER_LOCK = new ReentrantLock();

    //空闲节点最大存活时间
    protected abstract PoolConfig<K, C, N> getPoolConfig();

    //获取一个连接
    protected abstract N generalConnect(K k) throws IOException;

    //关闭一个连接
    protected abstract boolean closeConnect(N n) throws IOException;

    //初始化定时任务线程池
    private synchronized ScheduledThreadPoolExecutor initSchedule() {
        if (executor == null) {
            executor = new ScheduledThreadPoolExecutor(1);
            executor.setMaximumPoolSize(1);
        }
        return executor;
    }

    private PoolConfig<K, C, N> getConfig() {
        if (this.config == null) {
            INNER_LOCK.lock();
            try {
                if (this.config == null) {
                    this.config = PoolConfig.buildNewConfig(getPoolConfig());
                }
            } finally {
                INNER_LOCK.unlock();
            }
        }
        return this.config;
    }

    @Setter
    @Getter
    private static class ConditionNode {
        private Condition condition;
        //private int waiterCount = 0;
        private LinkedList<String> queue = new LinkedList<>();

        public ConditionNode(Lock lock) {
            this.condition = lock.newCondition();
        }

        public void addWaiter(Thread thread) {
            queue.add(parseWaiterName(thread));
        }

        public static String parseWaiterName(Thread thread) {
            return thread.getName() + thread.getThreadGroup() + thread.getId();
        }
    }

    protected AbstractPool() {
        log.info("pool init");
        if (getConfig().getEnableSchedule()) {
            if (executor == null) {
                initSchedule();
                if (executor == null) {
                    throw new RuntimeException("scheduledThreadPoolExecutor init error!");
                }
            }
            submitSchedule();
        }
    }

    //提交定时器
    private void submitSchedule() {
        scheduledFuture = executor.scheduleAtFixedRate(this::scheduleClear, 2, 1, TimeUnit.SECONDS);
    }

    /**
     * @param k, timeout
     * @return N
     * @Description 带超时事件的阻塞式获取，如果获取失败，将会进入FIFO中等待分配
     * 1.抢锁超时 2.在FIFO中等待超时 会直接返回null
     * @author wz
     * @date 2022/1/5 11:22
     */
    @Override
    public N pickBlock(K k, long timeout) {
        Lock lock = getConfig().getLock();
        N node = null;
        boolean IOE = false;
        K realKey = null;
        try {
            if (timeout > 0) {
                if (!lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
                    //lock = null;
                    return null;
                }
            } else {
                lock.lock();
            }
            if (!getConfig().getEnable()) {
                log.warn("连接池已经关闭");
                return null;
            }
            Map<K, Collection<N>> pool = getConfig().getPool();
            realKey = generalRealKey(k);
            int limit = realKey.getLimit();
            Collection<N> list = pool.computeIfAbsent(realKey, i -> new ArrayList<>(limit));
            node = getNodeFromPool(realKey, list);
        } catch (InterruptedException | IOException e) {
            log.error(e.getMessage(), e);
            IOE = true;
        } finally {
            try {
                if (!IOE) {
                    //没有异常的情况下，没有拿到node，则加入等待队列
                    if (node == null) {
                        node = addWaitQueue(realKey, lock, timeout);
                    } else {
                        //没有异常的情况下，拿到了node，但是等待队列不为空，则优先等待队列
                        boolean need = needJoinWaitQueue(realKey);
                        if (need) {
                            Thread thread = Thread.currentThread();
                            log.info("线程:{}获取到了node 但优先给了等待队列", thread.getId());
                            //唤醒等待队列的线程
                            notifyWaiter(realKey, node);
                            //自己加入等待队列
                            node = addWaitQueue(realKey, lock, timeout);
                        }
                    }
                }
            } finally {
                unlock(lock);
            }
        }
        return node;
    }

    private void unlock(Lock lock) {
        try {
            if (lock != null) {
                lock.unlock();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    //判断是否需要加入等待队列
    private boolean needJoinWaitQueue(K realKey) {
        ConditionNode node = awaitQueue.get(realKey);
        return node != null && node.getQueue() != null && node.getQueue().size() != 0;
    }

    //加入等待队列
    private N addWaitQueue(K realKey, Lock lock, long timeout) {
        //只有在没有异常的情况下才能将线程加入队列，
        // 否则如果连接失败任加入队列，在网络异常的情况下会造成线程疯狂入队，造成资源耗尽
        String sot = null;
        try {
            ConditionNode conditionNode = awaitQueue.get(realKey);
            if (conditionNode == null) {
                conditionNode = new ConditionNode(lock);
                awaitQueue.put(realKey, conditionNode);
            }
            int maxQueueLen = getConfig().getAwaitQueueLength();
            //等待队列未满
            if (conditionNode.getQueue().size() < maxQueueLen) {
                //加入等待队列
                conditionNode.addWaiter(Thread.currentThread());
                if (timeout > 0) {
                    conditionNode.getCondition().await(timeout, TimeUnit.MILLISECONDS);
                } else {
                    conditionNode.getCondition().await();
                }
                sot = ConditionNode.parseWaiterName(Thread.currentThread());
                //利用volatile唤醒之后再去获取一次
                return awaitDataStore.get(sot);
            } else {
                log.error("等待队列已满maxQueueLen=" + maxQueueLen);
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } finally {
            //help gc
            awaitDataStore.remove(sot);
        }
        return null;
    }

    /**
     * @param key
     * @return N
     * @Description 阻塞式从池中获取连接，如果获取不到将阻塞线程，等到其他线程释放后会唤醒
     * @author wz
     * @date 2022/1/5 11:20
     */
    @Override
    public N pickBlock(K key) {
        return pickBlock(key, NO_TIME);
    }

    //生成真正的key
    private K generalRealKey(K k) {
        K realKey = null;
        Map<K, Collection<N>> pool = getConfig().getPool();
        if (pool.containsKey(k)) {
            realKey = findKeyFromPool(k);
        }
        return realKey == null ? k.cloneMe() : realKey;
    }

    /**
     * @param k
     * @return N
     * @Description 不带阻塞的获取连接，在抢到锁后如果获取不到连接就直接返回
     * @author wz
     * @date 2022/1/5 11:26
     */
    @Override
    public N pickNonBlock(K k) {
        Lock lock = getConfig().getLock();
        N node = null;
        K realKey = null;
        try {
            lock.lock();
            Map<K, Collection<N>> pool = getConfig().getPool();
            realKey = generalRealKey(k);
            int limit = realKey.getLimit();
            Collection<N> list = pool.computeIfAbsent(realKey, i -> new ArrayList<>(limit == NO_LIMIT ? 1 : limit));
            node = getNodeFromPool(realKey, list);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
        //log.info("从池中获取的node={}", node);
        return node;
    }

    //从线程池中获取一个连接 //todo 可以在此处控制最大连接数
    private N getNodeFromPool(K key, Collection<N> list) throws IOException {
        //如果连接池已经关闭则不创建连接
        if (!getConfig().getEnable()) return null;
        //从池中找到空闲的连接并测试连接是否开启
        Iterator<N> iterator = list.iterator();
        while (iterator.hasNext()) {
            N node = iterator.next();
            if (node.free) {
                //提供连接节点前先测试一下是否时开启状态
                if (testNodeIsOpen(node)) {
                    return node;
                } else {
                    if (safeCloseNode(node))
                        iterator.remove();
                }
            } else if (node.close) {
                //如果连接还是开启的状态，则尝试去关闭
                if (testNodeIsOpen(node)) {
                    if (safeCloseNode(node)) iterator.remove();
                } else {
                    //如果连接已经关闭，则直接从连接池中移除即可
                    iterator.remove();
                }
            }
        }
        //检测总连接数
        int max = getConfig().getMaxLiveNodeLimit();
        if (max <= 0 && max != NO_LIMIT) {
            //防止节点数被动态设置成0造成，等待的线程不能得到唤醒和释放
            max = PoolConfig.DEFAULT_MAX_NODE_LIMIT;
        }
        int allNodes = allNodesCount();
        if (allNodes > max && max != NO_LIMIT) {
            log.error("连接池到达了最大值:{} 在旧的连接释放之前，不能再加入新的连接了！", max);
            return null;
        }
        int perLimit = key.getLimit();
        //如果连接池还没满 perLimit<0 不对单个节点进行限制
        //如果动态缩小了keyPool连接池，则当前线程可能会获取不到新的连接
        if (perLimit >= 0 && list.size() >= perLimit) {
            log.error("当前节点key：{}到达了最大值:{} 在旧的连接释放之前，不能再加入新的连接了！", key.getName(), perLimit);
            return null;
        }
        //主动获取一个连接
        N node = generalConnect(key);
        if (node != null) {
            list.add(node);
        }
        return node;
    }

    private boolean testNodeIsOpen(N node) {
        try {
            //如果连接已经关闭，则进行剔除
            //连接是开启的就直接返回
            return nodeIsOpen(node);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * @param node
     * @return void
     * @Description 归还一个连接;如果有等待队列,唤醒队列等待时间最久的线程并分配连接
     * @author wz
     * @date 2022/1/5 11:28
     */
    @Override
    public void back(N node) {
        if (node == null) return;
        Lock lock = getConfig().getLock();
        K key = node.key;
        boolean open;
        try {
            lock.lock();
            open = !node.close;
            if (open) {
                node.free = (true);
                node.close = (false);
                node.lastUsedTime = (new Date().getTime());
                //如果连接池需要关闭,则强制关闭归还的连接并从池中剔除
                if (!getConfig().getEnable()) {
                    node.close = true;
                    node.free = false;
                }
            }
            if (node.close) {
                //如果节点设置了关闭状态，但并未真正关闭
                if (testNodeIsOpen(node)) {
                    safeCloseNode(node);//尝试再次关闭一次
                }
                //如果节点已经关闭，则需要剔除
                Map<K, Collection<N>> pool = this.getConfig().getPool();
                Collection<N> list = pool.get(key);
                list.remove(node);
                node = null;
            }
        } finally {
            try {
                notifyWaiter(key, node);
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * @param node
     * @return void
     * @Description 返回并关闭节点
     * @author wz
     * @date 2022/1/5 11:28
     */
    @Override
    public void backClose(N node) {
        if (node == null) return;
        Lock lock = getConfig().getLock();
        K key = node.key;
        try {
            lock.lock();
            node.close = true;
            node.free = false;
            safeCloseNode(node);//尝试再次关闭一次
            //如果节点已经关闭，则需要剔除
            Map<K, Collection<N>> pool = this.getConfig().getPool();
            Collection<N> list = pool.get(key);
            list.remove(node);
            node = null;
        } finally {
            lock.unlock();
        }
    }

    //唤醒等待队列的线程
    private void notifyWaiter(K key, N node) {
        //如果有等待队列,唤醒队列等待时间最久的线程并把连接给他
        boolean hasWaiter = false;//是否有线程在等待获取当前的连接节点
        ConditionNode conditionNode = awaitQueue.get(key);
        if (conditionNode != null) {
            Condition condition = conditionNode.getCondition();
            //如果连接池已经关闭，则通知所有的等待线程
            if (!getConfig().getEnable()) {
                //如果连接池已经关闭，需要将等待的线程都进行释放
                //通知所有的线程，等待的线程将获取不到连接节点
                condition.signalAll();
                //清空等待队列
                Iterator<String> iterator = conditionNode.getQueue().iterator();
                while (iterator.hasNext()) {
                    iterator.remove();
                }
            } else {
                if (node == null) {
                    //如果节点已经关闭，则尝试再次获取一下
                    if ((node = pickNonBlock(key)) == null) {
                        //如果节点依旧为空，则不唤醒等待的线程，保证唤醒的线程一定是有可用连接的
                        return;
                    }
                }
                int size = conditionNode.getQueue().size();
                if (size != 0) {
                    hasWaiter = true;
                    //方式１：通过volatile中间变量
                    String waiter = conditionNode.getQueue().poll();
                    if (waiter != null) {
                        //设置为占用的状态
                        node.free = (false);
                        //存入数据
                        awaitDataStore.put(waiter, node);
                        //唤醒等待的线程
                        condition.signal();
                        //方式２：通过对象引用
                        // awaitNode.setLastUsedTime(new Date().getTime())
                        //方式3：通过唤醒后的线程再次主动获取
                    }
                } else {
                    log.info("没有等待{}的线程了", key.getName());
                }
            }
        }
        //在没有等待线程的情况下，看下是否需要调整池子大小，
        // 如果需要可能会剔除当前连接节点
        //如果有线程在等待当前连接，优先线程使用在进行连接池调整
        if (node != null && !hasWaiter) {
            closeNodeIfNecessary(key, node);
        }
    }

    @Override
    //定时清理过期不用的连接
    public void scheduleClear() {
        Boolean enable = getConfig().getEnableSchedule();
        if (!enable) return;
        Lock lock = this.getConfig().getLock();
        try {
            lock.lock();
            Map<K, Collection<N>> pool = this.getConfig().getPool();
            //如果定时器关闭
            Boolean enableSchedule = this.getConfig().getEnableSchedule();
            if (!enableSchedule) {
                //关闭定是任务
                cancelSchedule(this.scheduledFuture);
                return;
            }
            //空闲连接最大存活时间
            long maxLiveTime = getConfig().getMaxFreeNodeLiveTime();
            long now = new Date().getTime();
            for (Map.Entry<K, Collection<N>> item : pool.entrySet()) {
                Collection<N> value = item.getValue();
                Iterator<N> iterator = value.iterator();
                while (iterator.hasNext()) {
                    N node = iterator.next();
                    //如果连接没有使用
                    if (node.free) {
                        //如果超时没有线程去使用则关闭 或者　如果连接池已经关闭
                        if ((now - node.lastUsedTime > maxLiveTime) || !getConfig().getEnable()) {
                            node.close = true;
                        }
                    }
                    //尝试关闭连接，如果关闭3次以上还没有关闭成功，则直接从池中剔除
                    if (node.close) {
                        String name = getConfig().getName();
                        log.info("{}  连接池 主动关闭空闲连接 {} ", name, node.getKey());
                        boolean down = safeCloseNode(node);
                        if (down || node.tryCloseCount > 3) {
                            iterator.remove();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param config
     * @return void
     * @Description 刷新连接池的配置
     * @author wz
     * @date 2022/1/5 11:29
     */
    @Override
    public void refreshPoolConfig(PoolConfig<K, C, N> config) {
        Lock lock = this.getConfig().getLock();
        try {
            lock.lock();
            //刷新配置
            PoolConfig.refresh(config, getConfig());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    //获取真实KEY
    private K findKeyFromPool(K key) {
        ReentrantLock lock = getConfig().getLock();
        try {
            lock.lock();
            Map<K, Collection<N>> pool = getConfig().getPool();
            Set<K> list = pool.keySet();
            for (K k : list) {
                if (k.equals(key)) {
                    return k;
                }
            }
        } finally {
            lock.unlock();
        }
        return null;
    }

    //获取当前对应key的池子大小
    public int getPoolRealSize(K key) {
        ReentrantLock lock = getConfig().getLock();
        try {
            lock.lock();
            Map<K, Collection<N>> pool = getConfig().getPool();
            Collection<N> list = pool.get(key);
            return list == null ? 0 : list.size();
        } finally {
            lock.unlock();
        }
    }

    //获取当前对应key的池子大小
    public int getPoolRealBusyNodeSize(K key) {
        ReentrantLock lock = getConfig().getLock();
        try {
            lock.lock();
            Map<K, Collection<N>> pool = getConfig().getPool();
            Collection<N> list = pool.get(key);
            if (list == null) {
                return 0;
            }
            int size = 0;
            for (N n : list) {
                //not free = busy =有使用者
                size += !n.free ? 1 : 0;
            }
            return size;
        } finally {
            lock.unlock();
        }
    }

    //获取key对应的池子配置的最大连接数
    public int getKeyPoolConfigSize(K key) {
        ReentrantLock lock = getConfig().getLock();
        try {
            lock.lock();
            //如果key很多可以采用上面keyIndex建立索引进行快速查找，避免全部遍历
            Map<K, Collection<N>> pool = getConfig().getPool();
            Set<K> list = pool.keySet();
            for (K k : list) {
                if (k.equals(key)) {
                    return k.getLimit();
                }
            }
        } finally {
            lock.unlock();
        }
        return 0;
    }

    //修改指定节点的limit
    protected void forceRefreshKeyPoolLimit(K key, int limit) {
        ReentrantLock lock = getConfig().getLock();
        try {
            lock.lock();
            ArrayList<K> list = new ArrayList<>(1);
            list.add(key);
            forceRefreshKeyPoolLimit(list, limit);
            // not safe key.setLimit(limit);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }


    //强制修改指定节点的limit
    protected void forceRefreshKeyPoolLimit(List<K> keys, int limit) {
        ReentrantLock lock = getConfig().getLock();
        try {
            lock.lock();
            Map<K, Collection<N>> pool = getConfig().getPool();
            List<K> tmpList = new ArrayList<>(keys.size());
            for (K key : keys) {
                boolean contains = pool.containsKey(key);
                if (contains) tmpList.add(key);
            }
            if (tmpList.size() > 0) {
                Set<K> ks = pool.keySet();
                Iterator<K> iterator = tmpList.iterator();
                for (K real : ks) {
                    while (iterator.hasNext()) {
                        K tmp = iterator.next();
                        if (real.equals(tmp)) {
                            real.setLimit(limit);
                            iterator.remove();//help quickly loop
                        }
                    }
                    break;// quickly out
                }
                // not safe key.setLimit(limit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    //获取当前池中所有的key
    public Set<K> getAllKeys() {
        ReentrantLock lock = getConfig().getLock();
        try {
            lock.lock();
            Map<K, Collection<N>> pool = getConfig().getPool();
            return pool.keySet();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
        return null;
    }

    //采用缓存或者复制的方式，防止外部直接修改内部数据，造成未知异常
    public PoolConfig<K, C, N> showConfig() {
        PoolConfig<K, C, N> config = getConfig();
        if (cacheConfig == null) {
            INNER_LOCK.lock();
            try {
                if (cacheConfig == null) {
                    cacheConfig = new PoolConfig<>();
                }
            } finally {
                INNER_LOCK.unlock();
            }
        }
        PoolConfig.copyConfig(cacheConfig, config);
        return cacheConfig;
    }

    //获取当前池中所有的连接数
    public int allNodesCount() {
        ReentrantLock lock = getConfig().getLock();
        try {
            lock.lock();
            int total = 0;
            Map<K, Collection<N>> pool = getConfig().getPool();
            Collection<Collection<N>> values = pool.values();
            for (Collection<N> value : values) {
                total += value.size();
            }
            return total;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
        return 0;
    }

    //清理关闭所有空闲的连接 onlyFreeNode=true 只会关闭空闲的node else 全部强制关闭
    private void clearAllNode(boolean onlyFreeNode) {
        Lock lock = this.getConfig().getLock();
        try {
            lock.lock();
            Map<K, Collection<N>> pool = getConfig().getPool();
            Collection<Collection<N>> values = pool.values();
            if (onlyFreeNode) {
                for (Collection<N> value : values) {
                    clearFreeNode(value, 0);
                }
            } else {
                //限制所有的key 不能创建新的连接
                Set<K> ks = pool.keySet();
                for (K k : ks) {
                    k.setLimit(0);
                }
                //强制关闭所有节点
                for (Collection<N> value : values) {
                    focusCloseNode(value);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    private void focusCloseNode(Collection<N> list) {
        for (N n : list) {
            safeCloseNode(n);
        }
    }

    //如果当前 keyPool的size大于配置的size，则进行关闭和剔除
    protected void closeNodeIfNecessary(K key, N node) {
        //如果实际连接大于配置的数量，则进行剔除
        ReentrantLock lock = this.getConfig().getLock();
        try {
            lock.lock();
            //对连接数进行调整
            Collection<N> pool = this.getConfig().getPool().get(key);
            //连接池的实际数量大于 key当前配置的
            log.info("key= {} pool.size()={} key.getLimit()={} ", key.getName(), pool.size(), key.getLimit());
            if (pool.size() > key.getLimit()) {
                if (node != null) {
                    //设置标记
                    node.close = true;
                    node.free = true;
                    boolean done = safeCloseNode(node);
                    if (done) {
                        pool.remove(node);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    //根据配置进行动态调整连接池大小
    protected void tryRefreshPool() {
        //如果实际连接大于配置的数量，则进行剔除
        ReentrantLock lock = this.getConfig().getLock();
        try {
            lock.lock();
            //对内部定时任务的开关进行处理
            Boolean enableSchedule = this.getConfig().getEnableSchedule();
            if (Boolean.FALSE.equals(enableSchedule)) {
                if (!isNullOrDone(this.scheduledFuture)) {
                    //关闭定时器
                    cancelSchedule(scheduledFuture);
                }
            } else {
                //如果定时器配置开启了，但是定时器并没有跑起来
                if (isNullOrDone(this.scheduledFuture)) {
                    submitSchedule();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    //调整单个key的连接数
    protected void balanceKeyPool(K key) {
        Lock lock = this.getConfig().getLock();
        try {
            lock.lock();
            Map<K, Collection<N>> pool = this.getConfig().getPool();
            Collection<N> list = pool.get(key);
            //当配置数减少时会主动清理空闲的连接
            if (list != null && list.size() > key.getLimit()) {
                clearFreeNode(list, key.getLimit());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    //根据最大连接数，清理空闲的连接
    private void clearFreeNode(Collection<N> list, int limit) {
        String name = getConfig().getName();
        log.info("{} 连接池 根据最大连接数，清理空闲的连接", name);
        Iterator<N> iterator = list.iterator();
        while (list.size() > limit && iterator.hasNext()) {
            N node = iterator.next();
            //如果连接没有使用
            if (node.free) {
                //如果超时没有线程去使用则关闭 或者　如果连接池已经关闭
                node.close = true;
            }
            if (node.close) {
                boolean done = safeCloseNode(node);
                if (done) {
                    iterator.remove();
                    node = null;//help gc
                }
            }
        }
    }


    //没有异常的关闭一个节点
    private boolean safeCloseNode(N node) {
        try {
            String name = getConfig().getName();
            //主动关闭
            log.info("{} 连接池主动关闭连接", name);
            return closeConnect(node);
        } catch (Exception e) {
            log.error("连接已经设置为关闭状态，但关闭失败!{}", node.toString(), e);
        } finally {
            node.tryCloseCount++;
        }
        return node.tryCloseCount > 3;
    }

    @Override
    //注意closePool一定要和openPool配合使用，缺一不可！
    //close 关闭连接池后将做如下事情
    //1.不在接受新的连接请求
    //2.在线程归还连接时，将主动清除等待该连接的线程
    //3.定时器不在等待超时，而直接清除没有使用的连接
    public void closePool() {
        Lock lock = this.getConfig().getLock();
        try {
            String name = getConfig().getName();
            log.info("{} 连接池关闭 close", name);
            if (getConfig().getEnable()) {
                lock.lock();
                //关闭
                if (getConfig().getEnable()) {
                    getConfig().setEnable(false);
                }
                //清理关闭所有空闲的连接，不空闲的连接在归还连接的时候会主动关闭
                clearAllNode(true);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void openPool() {
        Lock lock = this.getConfig().getLock();
        try {
            if (!getConfig().getEnable()) {
                lock.lock();
                String name = getConfig().getName();
                log.info("{} 开启连接池 open", name);
                if (!getConfig().getEnable()) {
                    getConfig().setEnable(true);
                    tryRefreshPool();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }


    private static boolean isNullOrDone(ScheduledFuture<?> scheduledFuture) {
        return scheduledFuture == null || scheduledFuture.isCancelled() || scheduledFuture.isDone();
    }

    //关闭线程任务
    private static void cancelSchedule(ScheduledFuture<?> scheduledFuture) {
        try {
            if (scheduledFuture != null && (!scheduledFuture.isCancelled() || !scheduledFuture.isDone())) {
                scheduledFuture.cancel(false);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    //销毁事件
    @Override
    public void destroy() {
        String name = getConfig().getName();
        log.info("{} 销毁连接池 destroy", name);
        //设置关闭标识
        if (config != null) {//lock
            config.setEnable(false);
        }
        //主动关闭定时任务
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
        //强制关闭所有连接
        clearAllNode(false);
    }
}
