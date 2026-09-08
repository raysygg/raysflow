package com.acme.agentstudio.infrastructure.realtime;

import com.acme.agentstudio.config.RealtimeProperties;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 实时事件按 Key 分片/条带化 (Striped) 顺序投递执行器（Realtime Dispatch Executor）。
 * 固定大小的分片发送执行器，同一资源始终由同一线程顺序投递。
 */
@Component
public class RealtimeDispatchExecutor {
    private final ThreadPoolExecutor[] stripes;

    public RealtimeDispatchExecutor(RealtimeProperties properties) {
        int stripeCount = Math.max(2, Runtime.getRuntime().availableProcessors());
        int queueCapacity = Math.max(100, properties.getDispatchQueueCapacity() / stripeCount);
        this.stripes = new ThreadPoolExecutor[stripeCount];
        for (int index = 0; index < stripeCount; index++) {
            stripes[index] = createStripe(index, queueCapacity);
        }
    }

        /**
         * 执行execute 业务逻辑处理。
         *
         * @param orderingKey orderingKey 参数
         * @param task task 参数
         */
    public void execute(String orderingKey, Runnable task) {
        try {
            stripe(orderingKey).execute(task);
        } catch (RejectedExecutionException exception) {
            throw new IllegalStateException("实时事件发送队列已满，请稍后重连", exception);
        }
    }

    private ThreadPoolExecutor stripe(String orderingKey) {
        int hash = orderingKey == null ? 0 : orderingKey.hashCode();
        return stripes[Math.floorMod(hash, stripes.length)];
    }

    private ThreadPoolExecutor createStripe(int stripeIndex, int queueCapacity) {
        return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity), new NamedThreadFactory(stripeIndex),
                new ThreadPoolExecutor.AbortPolicy());
    }

        /**
         * shutdown 方法。
         */
    @PreDestroy
    public void shutdown() {
        for (ThreadPoolExecutor stripe : stripes) stripe.shutdownNow();
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final int stripeIndex;
        private final AtomicInteger index = new AtomicInteger();

        private NamedThreadFactory(int stripeIndex) {
            this.stripeIndex = stripeIndex;
        }

            /**
             * newThread 方法。
             *
             * @param runnable runnable 参数
             * @return Thread 返回对象
             */
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable,
                    "realtime-dispatch-" + stripeIndex + "-" + index.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
