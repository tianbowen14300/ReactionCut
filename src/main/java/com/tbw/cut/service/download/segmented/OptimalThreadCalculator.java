package com.tbw.cut.service.download.segmented;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 最优线程数计算器
 * 基于文件大小、带宽、系统资源计算最优线程数
 */
@Slf4j
@Component
public class OptimalThreadCalculator {
    
    @Value("${download.thread.min:1}")
    private int minThreads;
    
    @Value("${download.thread.max:16}")
    private int maxThreads;
    
    @Value("${download.thread.cpu-factor:2.0}")
    private double cpuFactor;
    
    @Value("${download.thread.memory-threshold:0.8}")
    private double memoryThreshold;
    
    // 历史性能数据存储
    private final ConcurrentHashMap<String, PerformanceData> performanceHistory = new ConcurrentHashMap<>();
    
    // 系统资源监控
    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    
    /**
     * 计算最优线程数
     * @param fileSize 文件大小（字节）
     * @param estimatedBandwidth 预估带宽（字节/秒）
     * @param serverUrl 服务器URL（用于历史数据查询）
     * @return 最优线程数
     */
    public int calculateOptimalThreads(long fileSize, long estimatedBandwidth, String serverUrl) {
        log.debug("Calculating optimal threads for file size: {}, bandwidth: {}, server: {}", 
            fileSize, estimatedBandwidth, serverUrl);
        
        // 1. 基于CPU核心数的基础线程数
        int cpuBasedThreads = calculateCpuBasedThreads();
        
        // 2. 基于内存的线程数限制
        int memoryBasedThreads = calculateMemoryBasedThreads(fileSize);
        
        // 3. 基于带宽的线程数
        int bandwidthBasedThreads = calculateBandwidthBasedThreads(estimatedBandwidth);
        
        // 4. 基于文件大小的线程数
        int fileSizeBasedThreads = calculateFileSizeBasedThreads(fileSize);
        
        // 5. 基于历史性能数据的调整
        int historyAdjustedThreads = adjustBasedOnHistory(serverUrl, 
            Math.min(Math.min(cpuBasedThreads, memoryBasedThreads), 
                    Math.min(bandwidthBasedThreads, fileSizeBasedThreads)));
        
        // 6. 应用最终限制
        int optimalThreads = Math.max(minThreads, Math.min(maxThreads, historyAdjustedThreads));
        
        log.info("Optimal threads calculation - CPU: {}, Memory: {}, Bandwidth: {}, FileSize: {}, History: {}, Final: {}", 
            cpuBasedThreads, memoryBasedThreads, bandwidthBasedThreads, 
            fileSizeBasedThreads, historyAdjustedThreads, optimalThreads);
        
        return optimalThreads;
    }
    
    /**
     * 基于CPU核心数计算线程数
     * @return CPU基础线程数
     */
    private int calculateCpuBasedThreads() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        
        // 尝试获取CPU负载
        double cpuLoad = 0.5; // 默认假设50%负载
        try {
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                double load = sunOsBean.getProcessCpuLoad();
                if (load >= 0) {
                    cpuLoad = load;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get CPU load, using default", e);
        }
        
        // 根据CPU负载调整线程数
        double loadFactor = Math.max(0.3, 1.0 - cpuLoad);
        int cpuThreads = (int) Math.ceil(availableProcessors * cpuFactor * loadFactor);
        
        log.debug("CPU calculation - processors: {}, load: {:.2f}, factor: {:.2f}, threads: {}", 
            availableProcessors, cpuLoad, loadFactor, cpuThreads);
        
        return cpuThreads;
    }
    
    /**
     * 基于内存计算线程数限制
     * @param fileSize 文件大小
     * @return 内存限制的线程数
     */
    private int calculateMemoryBasedThreads(long fileSize) {
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long availableMemory = (long) ((maxMemory - usedMemory) * memoryThreshold);
        
        // 估算每个线程需要的内存（缓冲区 + 对象开销）
        long memoryPerThread = Math.max(1024 * 1024, fileSize / 100); // 至少1MB，最多文件大小的1%
        
        int memoryThreads = (int) Math.max(1, availableMemory / memoryPerThread);
        
        log.debug("Memory calculation - max: {}, used: {}, available: {}, per thread: {}, threads: {}", 
            maxMemory, usedMemory, availableMemory, memoryPerThread, memoryThreads);
        
        return memoryThreads;
    }
    
    /**
     * 基于带宽计算线程数
     * @param estimatedBandwidth 预估带宽
     * @return 带宽优化的线程数
     */
    private int calculateBandwidthBasedThreads(long estimatedBandwidth) {
        if (estimatedBandwidth <= 0) {
            return maxThreads / 2; // 默认值
        }
        
        // 基于带宽的启发式算法
        // 低带宽：更少线程避免竞争
        // 高带宽：更多线程充分利用
        long mbps = estimatedBandwidth / (1024 * 1024);
        
        int bandwidthThreads;
        if (mbps < 10) {
            bandwidthThreads = 2; // 低带宽
        } else if (mbps < 50) {
            bandwidthThreads = 4; // 中等带宽
        } else if (mbps < 100) {
            bandwidthThreads = 8; // 高带宽
        } else {
            bandwidthThreads = 12; // 超高带宽
        }
        
        log.debug("Bandwidth calculation - bandwidth: {} Mbps, threads: {}", mbps, bandwidthThreads);
        
        return bandwidthThreads;
    }
    
    /**
     * 基于文件大小计算线程数
     * @param fileSize 文件大小
     * @return 文件大小优化的线程数
     */
    private int calculateFileSizeBasedThreads(long fileSize) {
        if (fileSize <= 0) {
            return 1;
        }
        
        long mb = fileSize / (1024 * 1024);
        
        int fileSizeThreads;
        if (mb < 50) {
            fileSizeThreads = 2; // 小文件
        } else if (mb < 200) {
            fileSizeThreads = 4; // 中等文件
        } else if (mb < 1000) {
            fileSizeThreads = 8; // 大文件
        } else {
            fileSizeThreads = 12; // 超大文件
        }
        
        log.debug("File size calculation - size: {} MB, threads: {}", mb, fileSizeThreads);
        
        return fileSizeThreads;
    }
    
    /**
     * 基于历史性能数据调整线程数
     * @param serverUrl 服务器URL
     * @param baseThreads 基础线程数
     * @return 调整后的线程数
     */
    private int adjustBasedOnHistory(String serverUrl, int baseThreads) {
        if (serverUrl == null) {
            return baseThreads;
        }
        
        String serverKey = extractServerKey(serverUrl);
        PerformanceData history = performanceHistory.get(serverKey);
        
        if (history == null || history.getSampleCount() < 3) {
            // 历史数据不足，使用基础值
            return baseThreads;
        }
        
        // 基于历史性能调整
        double avgEfficiency = history.getAverageEfficiency();
        
        int adjustedThreads;
        if (avgEfficiency > 0.8) {
            // 高效率，可以增加线程数
            adjustedThreads = Math.min(maxThreads, (int) (baseThreads * 1.2));
        } else if (avgEfficiency < 0.5) {
            // 低效率，减少线程数
            adjustedThreads = Math.max(minThreads, (int) (baseThreads * 0.8));
        } else {
            // 中等效率，保持不变
            adjustedThreads = baseThreads;
        }
        
        log.debug("History adjustment - server: {}, efficiency: {:.2f}, base: {}, adjusted: {}", 
            serverKey, avgEfficiency, baseThreads, adjustedThreads);
        
        return adjustedThreads;
    }
    
    /**
     * 记录下载性能数据
     * @param serverUrl 服务器URL
     * @param threadCount 使用的线程数
     * @param fileSize 文件大小
     * @param downloadTimeMs 下载耗时（毫秒）
     * @param success 是否成功
     */
    public void recordPerformance(String serverUrl, int threadCount, long fileSize, 
                                 long downloadTimeMs, boolean success) {
        if (serverUrl == null || downloadTimeMs <= 0) {
            return;
        }
        
        String serverKey = extractServerKey(serverUrl);
        
        // 计算效率指标
        double throughput = success ? (double) fileSize / downloadTimeMs * 1000 : 0; // 字节/秒
        double efficiency = success ? Math.min(1.0, throughput / (threadCount * 1024 * 1024)) : 0; // 相对于1MB/s/thread
        
        performanceHistory.compute(serverKey, (key, existing) -> {
            if (existing == null) {
                existing = new PerformanceData();
            }
            existing.addSample(threadCount, efficiency, throughput);
            return existing;
        });
        
        log.debug("Recorded performance - server: {}, threads: {}, efficiency: {:.2f}, throughput: {:.2f} MB/s", 
            serverKey, threadCount, efficiency, throughput / (1024 * 1024));
    }
    
    /**
     * 从URL提取服务器标识
     * @param url URL
     * @return 服务器标识
     */
    private String extractServerKey(String url) {
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            return parsedUrl.getHost();
        } catch (Exception e) {
            return url.hashCode() + "";
        }
    }
    
    /**
     * 性能数据内部类
     */
    private static class PerformanceData {
        private final AtomicLong totalSamples = new AtomicLong(0);
        private volatile double totalEfficiency = 0.0;
        private volatile double totalThroughput = 0.0;
        private final Object lock = new Object();
        
        public void addSample(int threadCount, double efficiency, double throughput) {
            synchronized (lock) {
                totalSamples.incrementAndGet();
                totalEfficiency += efficiency;
                totalThroughput += throughput;
                
                // 保持最近100个样本
                if (totalSamples.get() > 100) {
                    totalEfficiency *= 0.99;
                    totalThroughput *= 0.99;
                    totalSamples.set(99);
                }
            }
        }
        
        public double getAverageEfficiency() {
            long samples = totalSamples.get();
            return samples > 0 ? totalEfficiency / samples : 0.0;
        }
        
        public double getAverageThroughput() {
            long samples = totalSamples.get();
            return samples > 0 ? totalThroughput / samples : 0.0;
        }
        
        public long getSampleCount() {
            return totalSamples.get();
        }
    }
}