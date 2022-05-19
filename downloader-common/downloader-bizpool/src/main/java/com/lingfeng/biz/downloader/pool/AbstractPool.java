package com.lingfeng.biz.downloader.pool;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

        public boolean removeFromQueue(Thread thread) {
            return queue.remove(parseWaiterName(thread));
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

    private boolean stateAccess() {
        if (!getConfig().getEnable()) {
            log.warn("{}连接池已经关闭", getConfig().getName());
            return false;
        }
        return true;
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
        Collection<N> pool = null;
        try {
            if (!stateAccess()) return null;
            //抢锁
            if (timeout > 0) {
                if (!lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
                    log.error("请求获取锁超时{}", k.getName());
                    return null;
                }
            } else {
                lock.lock();
            }
            realKey = getOrGeneralRealKey(k);
            pool = getOrGeneralPool(realKey);
            //从池子中获取一个空闲节点
            node = getNodeFromPool(realKey, pool);
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
                            log.info("线程:{}获取到了node 但优先给了等待队列", getThreadInfo(thread));
                            //唤醒等待队列的线程
                            notifyWaiter(realKey, node);
                            //自己加入等待队列
                            node = addWaitQueue(realKey, lock, timeout);
                        }
                    }
                }
                if (node != null) {
                    //测试节点是否已经关闭
                    if (!testNodeIsAlive(node)) {
                        log.info("{}获取到节点,但节点已经不可用,再次获取", getThreadInfo(Thread.currentThread()));
                        //关闭节点
                        releaseNode(node);
                        //从池中剔除节点
                        if (pool != null) {
                            pool.remove(node);
                            node.clear();
                        }
                        //如果节点已经关闭，再次进入循环
                        node = pickBlock(k, timeout);
                    }
                }
            } finally {
                if (node != null) {
                    //设置node的持有者
                    holdNode(node);
                }
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

    /**
     * @Description 从获取或者生成池子
     * @author wz
     */
    private Collection<N> getOrGeneralPool(K realKey) {
        //获取或者初始化池子
        Map<K, Collection<N>> pool = getConfig().getPool();
        int limit = realKey.getLimit() == NO_LIMIT ? 0 : realKey.getLimit();
        return pool.computeIfAbsent(realKey, i -> new ArrayList<>(limit));
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
                Thread thread = Thread.currentThread();
                sot = ConditionNode.parseWaiterName(thread);
                //利用volatile唤醒之后再去获取一次
                //conditionNode.getQueue() 在唤醒的时候会poll，等待队列就减少了
                log.info("thread={} 被唤醒", thread);
                return awaitDataStore.get(sot);
            } else {
                log.error("等待队列已满 maxQueueLen={} key={}", maxQueueLen, realKey.getName());
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            //如果线程等待被中断了，需要从队列中移除
            ConditionNode conditionNode = awaitQueue.get(realKey);
            if (conditionNode != null) {
                conditionNode.removeFromQueue(Thread.currentThread());
            }
        } finally {
            if (sot != null) {
                awaitDataStore.remove(sot);
            }
        }
        return null;
    }

    /**
     * @Description 阻塞式从池中获取连接，如果获取不到将阻塞线程，等到其他线程释放后会唤醒
     * @author wz
     * @date 2022/1/5 11:20
     */
    @Override
    public N pickBlock(K key) {
        return pickBlock(key, NO_TIME);
    }

    //生成真正的key
    private K getOrGeneralRealKey(K k) {
        K realKey = findKeyFromPool(k);
        return realKey == null ? k.cloneMe() : realKey;
    }

    /**
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
            if (!stateAccess()) return null;
            realKey = getOrGeneralRealKey(k);
            Collection<N> pool = getOrGeneralPool(realKey);
            node = getNodeFromPool(realKey, pool);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (node != null) {
                //如果可以使用
                if (testNodeIsAlive(node)) {
                    holdNode(node);
                } else {
                    //递归再次获取 两种结果 1.获取到可用的node 2.获取到null
                    node = pickNonBlock(k);
                }
            }
            unlock(lock);
        }
        //log.info("从池中获取的node={}", node);
        return node;
    }


    //从线程池中获取一个连接 //todo 可以在此处控制最大连接数
    private N getNodeFromPool(K key, Collection<N> list) throws IOException {
        N node = findOneFromPool(list);
        //如果在现有的池子中找不到一个空闲的节点，则生成新的节点
        if (node == null) {
            //判断是否所有节点数达到阈值
            boolean maxAccess = maxPoolLimitAccess();
            //判断单个key的节点数是否达到阈值
            boolean keyLimitAccess = keyLimitAccess(key, list);
            if (maxAccess && keyLimitAccess) {
                //主动从子类中生成一个新的连接，并加入池中
                node = generalConnect(key);
                if (node != null) {
                    list.add(node);
                }
            }
        }
        return node;
    }

    //最多连接数判断
    private boolean maxPoolLimitAccess() {
        //检测总连接数
        int max = getConfig().getMaxLiveNodeLimit();
        if (max <= 0 && max != NO_LIMIT) {
            //防止节点数被动态设置成0造成，等待的线程不能得到唤醒和释放
            max = PoolConfig.DEFAULT_MAX_NODE_LIMIT;
        }
        int allNodes = allNodesCount();
        if (allNodes > max && max != NO_LIMIT) {
            log.error("连接池到达了最大值:{} 在旧的连接释放之前，不能再加入新的连接了！", max);
            return false;
        }
        return true;
    }

    //单个key允许存放的最大连接数
    private boolean keyLimitAccess(K realKey, Collection<N> list) {
        int limit = realKey.getLimit();
        //如果连接池还没满 perLimit<0 不对单个节点进行限制
        //如果动态缩小了keyPool连接池，则当前线程可能会获取不到新的连接
        if (limit >= 0 && list.size() >= limit) {
            log.error("当前节点key：{}到达了最大值:{} 在旧的连接释放之前，不能再加入新的连接了！", realKey.getName(), limit);
            return false;
        }
        return true;
    }

    private N findOneFromPool(Collection<N> list) {
        //从池中找到空闲的连接并测试连接是否开启
        Iterator<N> iterator = list.iterator();
        while (iterator.hasNext()) {
            N node = iterator.next();
            if (node.free && !node.close) {
                //改成在外部测试可用性
                return node;
            } else if (node.close) {
                //如果连接还是开启的状态，则尝试去关闭
                if (testNodeIsAlive(node)) {
                    if (releaseNode(node)) iterator.remove();
                } else {
                    //如果连接已经关闭，则直接从连接池中移除即可
                    iterator.remove();
                }
            }
        }
        return null;
    }

    //测试节点是否仍然开启的状态
    private boolean testNodeIsAlive(N node) {
        try {
            //如果连接已经关闭，则进行剔除
            //连接是开启的就直接返回
            return nodeIsAlive(node);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * @Description 归还一个连接;如果有等待队列,唤醒队列等待时间最久的线程并分配连接 支持重复归还
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
            if (!holderCharge(node)) {
                node = null;
                return;
            }
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
                //在holder不为空的情况下 如果节点设置了关闭状态，但并未真正关闭
                if (node.holder != null && testNodeIsAlive(node)) {
                    safeCloseNode(node);//尝试再次关闭一次
                }
                //如果节点已经关闭，则需要剔除
                Map<K, Collection<N>> pool = this.getConfig().getPool();
                Collection<N> list = pool.get(key);
                list.remove(node);
                node.clear();
                node = null;
            }
        } finally {
            try {
                // 在线程第一次归还的时候，清除线程持有者
                if (node != null && node.holder != null) {
                    node.holder.clear();
                    node.holder = null;
                }
                //此处如果node已经关闭或者node已经为null，也要去尝试唤醒
                //在notifyWaiter内部会尝试给等待的线程 寻找新的有效node
                notifyWaiter(key, node);
            } finally {
                lock.unlock();
            }
        }
    }

    //判断 线程的持有者
    private boolean holderCharge(N node) {
        if (node.holder != null) {
            Thread holder = node.holder.get();
            Thread thread = Thread.currentThread();
            if (holder == null || !holder.equals(thread)) {
                log.info("holder不同 不进行重复归还 holder={} current={}", getThreadInfo(holder), getThreadInfo(thread));
                //如果没有持有者
                //或者持有者非当前的持有者(重复归还的情况下，第一次归还后已经被其他线程获取了，此时再次归还将不做处理)
                return false;
            }
        }
        return true;
    }

    /**
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
            //node持有者判断
            if (!holderCharge(node)) {
                node = null;
                return;
            }
            releaseNode(node);
            //如果节点已经关闭，则需要剔除
            Map<K, Collection<N>> pool = this.getConfig().getPool();
            Collection<N> list = pool.get(key);
            if (list != null) {
                list.remove(node);
            }
            node.clear();
        } finally {
            unlock(lock);
        }
    }

    /**
     * @Description 唤醒等待队列(FIFO)中等待获取节点的线程, 如果没有任何等待的线程，则根据配置情况尝试释放node
     * @author wz
     */
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
                    // releaseNode(node);
                    log.info("没有等待{}的线程了", key.getName());
                }
            }
        }
        //在没有等待线程的情况下，看下是否需要调整池子大小，
        // 如果需要可能会剔除当前连接节点
        //如果有线程在等待当前连接，优先线程使用在进行连接池调整
        if (node != null && !hasWaiter) {
            tryReleaseNode(key, node);
        }
    }


    //如果key对应的连接都被清空了，则将key也剔除
    private void clearNullNodeKey(Map.Entry<K, Collection<N>> item) {
        Collection<N> value = item.getValue();
        if (value.isEmpty()) {
            K key = item.getKey();
            ConditionNode conditionNode = awaitQueue.get(key);
            //并且没有任何等待连接的线程了
            if (conditionNode != null && conditionNode.getQueue().isEmpty()) {
                awaitQueue.remove(key);
                Map<K, Collection<N>> pool = this.getConfig().getPool();
                pool.remove(key);
            }
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
                //如果key对应的连接都被清空了，则将key也剔除
                clearNullNodeKey(item);
                //
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
                        node.free = false;
                        String name = getConfig().getName();
                        showRunStation(node.key);
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

    /**
     * @Description 获取在连接池中真实的KEY
     * @author wz
     */
    public K findKeyFromPool(K key) {
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
            unlock(lock);
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

    //获取key的等待线程数
    public int getKeyWaiter(K key) {
        ReentrantLock lock = getConfig().getLock();
        try {
            lock.lock();
            K realKey = findKeyFromPool(key);
            ConditionNode conditionNode = awaitQueue.get(realKey);
            if (conditionNode != null) {
                return conditionNode.getQueue().size();
            }
            return 0;
        } finally {
            lock.unlock();
        }
    }

    public void showRunStation(K key) {
        if (key == null) return;
        ReentrantLock lock = getConfig().getLock();
        try {
            lock.lock();
            log.info("[ KEY-POOL-RUN-STATION ] key-nodes={} running-node={} waiter={}  key-name={}",
                    getPoolRealSize(key), getPoolRealBusyNodeSize(key), getKeyWaiter(key), key.getName());
        } finally {
            unlock(lock);
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
            unlock(lock);
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
    protected void modifyKeyPoolLimit(K key, int limit) {
        ReentrantLock lock = getConfig().getLock();
        try {
            lock.lock();
            K realKey = findKeyFromPool(key);
            if (realKey != null) {
                realKey.setLimit(limit);
            } else {
                log.error("real key is null -> {}", key.getName());
            }
            // not safe key.setLimit(limit);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }


    //强制修改指定节点的limit
    protected void modifyKeyPoolLimit(List<K> keys, int limit) {
        ReentrantLock lock = getConfig().getLock();
        try {
            lock.lock();
            keys.forEach(i -> modifyKeyPoolLimit(i, limit));
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

    /**
     * @Description: 尝试释放节点, 当limit变小时，如果node持有者主动调用此方法将会主动放弃node
     * @author wz
     */
    protected void tryReleaseNode(K key, N node) {
        //如果实际连接大于配置的数量，则进行剔除
        ReentrantLock lock = this.getConfig().getLock();
        try {
            lock.lock();
            K realKey = findKeyFromPool(key);
            //如果key不进行限制 不进行关闭
            if (realKey == null || realKey.getLimit() == NO_LIMIT) return;
            //对连接数进行调整
            Collection<N> pool = this.getConfig().getPool().get(realKey);
            //连接池的实际数量大于 key当前配置的
            log.info("realKey= {} pool.size()={} realKey.getLimit()={} ", realKey.getName(), pool.size(), realKey.getLimit());
            //去掉核心线程的限制
            //if (pool.size() > realKey.getLimit()) {
            {
                log.info("没有等待{}的线程了 尝试释放空闲连接{}", key, node.hashCode());
                if (releaseNode(node)) {
                    pool.remove(node);
                    node.clear();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            unlock(lock);
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
        //如果limit变小
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

    private void holdNode(N node) {
        if (node != null) {
            showRunStation(node.key);
            node.free = false;
            node.close = false;
            node.holder = new WeakReference<>(Thread.currentThread());
        }
    }

    private boolean releaseNode(N node) {
        if (node != null) {
            showRunStation(node.key);
            log.info("释放空闲node={}", node.hashCode());
            //设置标记
            node.close = true;
            //free=true 不忙碌标记，如果本次safeCloseNode关闭失败，
            node.free = false;
            //主动关闭节点
            safeCloseNode(node);
            return true;
        }
        return false;
    }

    //没有异常的关闭一个节点
    private boolean safeCloseNode(N node) {
        ReentrantLock lock = getConfig().getLock();
        try {
            lock.lock();
            //String name = getConfig().getName();
            return closeConnect(node);
        } catch (Exception e) {
            log.error("连接已经设置为关闭状态，但关闭失败!{}", node.toString(), e);
        } finally {
            ++node.tryCloseCount;
            unlock(lock);
        }
        return node.tryCloseCount > 3;
    }

    //注意closePool一定要和openPool配合使用，缺一不可！
    //close 关闭连接池后将做如下事情
    //1.不在接受新的连接请求
    //2.在线程归还连接时，将主动清除等待该连接的线程
    //3.定时器不在等待超时，而直接清除没有使用的连接
    @Override
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

    private String getThreadInfo(Thread thread) {
        if (thread == null) return null;
        return thread.getThreadGroup() + "-" + thread.getId() + "-" + thread.getName();
    }
}
