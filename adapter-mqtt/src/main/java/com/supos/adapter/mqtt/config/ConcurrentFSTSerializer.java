//package com.supos.adapter.mqtt.config;
//
//
//import java.io.IOException;
//import java.util.concurrent.LinkedBlockingDeque;
//import java.util.concurrent.TimeUnit;
//
//public class ConcurrentFSTSerializer<T> {
//
//    private static final FSTConfiguration FST_CONFIG = FSTConfiguration.createDefaultConfiguration();
//    static {
//        FST_CONFIG.setShareReferences(false); // 关闭引用共享，提升性能
//    }
//
//    // 对象池复用 FSTObjectOutput（避免频繁创建 OutputStream）
//    private LinkedBlockingDeque<FSTObjectOutput> outputPool;
//    // 对象池复用 FSTObjectInput（避免频繁创建 InputStream）
//    private LinkedBlockingDeque<FSTObjectInput> inputPool;
//
//    public ConcurrentFSTSerializer(int poolSize) {
//        this.outputPool = new LinkedBlockingDeque<>(poolSize);
//        this.inputPool = new LinkedBlockingDeque<>(poolSize);
//        // 初始化对象池
//        for (int i = 0; i < poolSize; i++) {
//            outputPool.offer(FST_CONFIG.getObjectOutput());
//            inputPool.offer(FST_CONFIG.getObjectInput());
//        }
//    }
//
//    // 序列化（对象池优化）
//    public byte[] serialize(T obj) throws Exception {
//        FSTObjectOutput out = borrowOutput();
//        try {
//            out.resetForReUse(); // 重置内部状态（避免创建新 byte[]）
//            out.writeObject(obj);
//            return out.getCopyOfWrittenBuffer(); // 返回字节数组副本
//        } finally {
//            returnOutput(out); // 归还对象到池
//        }
//    }
//
//    // 反序列化（对象池优化）
//    public T deserialize(byte[] bytes) throws Exception {
//        FSTObjectInput in = borrowInput();
//        try {
//            in.resetForReuseUseArray(bytes); // 复用 byte[] 数组
//            return (T) in.readObject();
//        } finally {
//            returnInput(in); // 归还对象到池
//        }
//    }
//
//    // 从池中借出 Output
//    private FSTObjectOutput borrowOutput() throws InterruptedException {
//        FSTObjectOutput out = outputPool.poll(10, TimeUnit.MILLISECONDS);
//        return (out != null) ? out : FST_CONFIG.getObjectOutput();
//    }
//
//    // 归还 Output 到池
//    private void returnOutput(FSTObjectOutput out) throws IOException {
//        if (!outputPool.offer(out)) {
//            out.close(); // 池满时直接关闭
//        }
//    }
//
//    // 从池中借出 Input
//    private FSTObjectInput borrowInput() throws InterruptedException {
//        FSTObjectInput in = inputPool.poll(10, TimeUnit.MILLISECONDS);
//        return (in != null) ? in : FST_CONFIG.getObjectInput();
//    }
//
//    // 归还 Input 到池
//    private void returnInput(FSTObjectInput in) throws IOException {
//        if (!inputPool.offer(in)) {
//            in.close(); // 池满时直接关闭
//        }
//    }
//}
