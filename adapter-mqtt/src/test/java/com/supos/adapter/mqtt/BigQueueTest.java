package com.supos.adapter.mqtt;

import cn.hutool.core.thread.ThreadUtil;
import com.bluejeans.bigqueue.BigQueue;
import com.supos.common.Constants;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class BigQueueTest {

    public static void main(String[] args) throws Exception {
        printPid();
        BigQueue queue = new BigQueue(Constants.ROOT_PATH + File.separator + "queue", "test_cache", 32 * 1024 * 1024);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                queue.removeAll();
                queue.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
        final int TOTAL = 100000;
        for (int i = 0; i < TOTAL; i++) {
            final int I = i;
            ThreadUtil.execAsync(() -> {
                StringBuilder s = new StringBuilder(1024);
                s.append(String.format("%05d", I));
                int k = 1024 - 5;
                while (k-- > 0) {
                    s.append('k');
                }
                queue.enqueue(s.toString().getBytes());
            });
        }
        CountDownLatch latch = new CountDownLatch(TOTAL);
        AtomicInteger count = new AtomicInteger(0);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("进！");
                for (int i = 0; i < TOTAL; ) {
                    byte[] data = queue.dequeue();
                    if (data == null) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                        continue;
                    }
                    i++;
                    try {
//                        System.out.printf("%s , left: %d\n", new String(data,0,5), queue.size());
                    } finally {
                        if (data != null) {
                            count.incrementAndGet();
                            latch.countDown();
                        }
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

        System.out.printf("等待1...%d , %d\n", latch.getCount(), count.get());
        Thread.sleep(5 * 1000);
        System.out.printf(new Date() + " 等待2...%d , %d\n", latch.getCount(), count.get());
        latch.await();
        System.out.printf(new Date() + " 等待3...%d , %d\n", latch.getCount(), count.get());
        printPid();
        System.out.println("~~~");
        Thread.sleep(15 * 1000);
//        queue.gc();
//        System.out.println("after gc");
//        Thread.sleep(15 * 1000);
        printPid();
        Thread.sleep(1500 * 1000);
        System.out.println(queue.size());
    }

    static void printPid() {
        // 获取当前Java虚拟机的pid
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        String pid = jvmName.split("@")[0]; // 分割字符串获取pid
        System.out.println("当前进程的PID是: " + pid);
    }
}
