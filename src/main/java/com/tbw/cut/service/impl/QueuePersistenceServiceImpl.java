package com.tbw.cut.service.impl;

import com.tbw.cut.entity.QueuedTask;
import com.tbw.cut.service.QueuePersistenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于内存的队列持久化服务实现
 */
@Slf4j
@Service
public class QueuePersistenceServiceImpl implements QueuePersistenceService {
    
    // 内存队列存储 - 队列ID -> 任务队列
    private final Map<String, Queue<String>> queues = new ConcurrentHashMap<>();
    
    // 任务详细信息存储 - 任务ID -> 任务对象
    private final Map<String, QueuedTask> tasks = new ConcurrentHashMap<>();
    
    // 队列位置计数器 - 队列ID -> 位置计数器
    private final Map<String, AtomicInteger> positionCounters = new ConcurrentHashMap<>();
    
    @Override
    public boolean enqueue(QueuedTask queuedTask) {
        try {
            String queueId = queuedTask.getQueueId();
            String taskId = queuedTask.getTaskId();
            
            log.info("任务入队，任务ID: {}, 队列ID: {}", taskId, queueId);
            
            // 初始化队列（如果不存在）
            queues.computeIfAbsent(queueId, k -> new ConcurrentLinkedQueue<>());
            positionCounters.computeIfAbsent(queueId, k -> new AtomicInteger(0));
            
            // 设置队列位置
            int position = positionCounters.get(queueId).incrementAndGet();
            queuedTask.setPosition(position);
            queuedTask.setQueuedAt(LocalDateTime.now());
            
            // 添加到队列和任务存储
            queues.get(queueId).offer(taskId);
            tasks.put(taskId, queuedTask);
            
            log.info("任务入队成功，任务ID: {}, 队列ID: {}, 位置: {}", taskId, queueId, position);
            return true;
            
        } catch (Exception e) {
            log.error("任务入队失败，任务ID: {}", queuedTask.getTaskId(), e);
            return false;
        }
    }
    
    @Override
    public Optional<QueuedTask> dequeue(String queueId) {
        try {
            Queue<String> queue = queues.get(queueId);
            if (queue == null || queue.isEmpty()) {
                return Optional.empty();
            }
            
            String taskId = queue.poll();
            if (taskId == null) {
                return Optional.empty();
            }
            
            QueuedTask task = tasks.get(taskId);
            if (task != null) {
                // 更新剩余任务的位置
                updateAllTaskPositions(queueId);
                log.info("任务出队成功，任务ID: {}, 队列ID: {}", taskId, queueId);
                return Optional.of(task);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.error("任务出队失败，队列ID: {}", queueId, e);
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<QueuedTask> peek(String queueId) {
        try {
            Queue<String> queue = queues.get(queueId);
            if (queue == null || queue.isEmpty()) {
                return Optional.empty();
            }
            
            String taskId = queue.peek();
            if (taskId == null) {
                return Optional.empty();
            }
            
            QueuedTask task = tasks.get(taskId);
            return Optional.ofNullable(task);
        } catch (Exception e) {
            log.error("查看队列头部任务失败，队列ID: {}", queueId, e);
            return Optional.empty();
        }
    }
    
    @Override
    public int getQueueLength(String queueId) {
        try {
            Queue<String> queue = queues.get(queueId);
            return queue != null ? queue.size() : 0;
        } catch (Exception e) {
            log.error("获取队列长度失败，队列ID: {}", queueId, e);
            return 0;
        }
    }
    
    @Override
    public List<QueuedTask> getAllQueuedTasks(String queueId) {
        try {
            Queue<String> queue = queues.get(queueId);
            if (queue == null || queue.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<QueuedTask> tasks = new ArrayList<>();
            int position = 1;
            
            for (String taskId : queue) {
                QueuedTask task = this.tasks.get(taskId);
                if (task != null) {
                    // 创建副本以避免修改原对象
                    QueuedTask taskCopy = copyTask(task);
                    taskCopy.setPosition(position++);
                    tasks.add(taskCopy);
                }
            }
            
            return tasks;
        } catch (Exception e) {
            log.error("获取队列所有任务失败，队列ID: {}", queueId, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean updateTask(String taskId, QueuedTask queuedTask) {
        try {
            if (tasks.containsKey(taskId)) {
                tasks.put(taskId, queuedTask);
                log.debug("任务更新成功，任务ID: {}", taskId);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("任务更新失败，任务ID: {}", taskId, e);
            return false;
        }
    }
    
    @Override
    public boolean removeTask(String taskId, String queueId) {
        try {
            Queue<String> queue = queues.get(queueId);
            if (queue != null) {
                boolean removed = queue.remove(taskId);
                tasks.remove(taskId);
                
                if (removed) {
                    // 更新剩余任务的位置
                    updateAllTaskPositions(queueId);
                    log.info("任务移除成功，任务ID: {}, 队列ID: {}", taskId, queueId);
                }
                
                return removed;
            }
            return false;
        } catch (Exception e) {
            log.error("任务移除失败，任务ID: {}, 队列ID: {}", taskId, queueId, e);
            return false;
        }
    }
    
    @Override
    public Optional<QueuedTask> getTask(String taskId) {
        try {
            QueuedTask task = tasks.get(taskId);
            return Optional.ofNullable(task);
        } catch (Exception e) {
            log.error("获取任务失败，任务ID: {}", taskId, e);
            return Optional.empty();
        }
    }
    
    @Override
    public int getTaskPosition(String taskId, String queueId) {
        try {
            Queue<String> queue = queues.get(queueId);
            if (queue == null) {
                return -1;
            }
            
            int position = 1;
            for (String id : queue) {
                if (id.equals(taskId)) {
                    return position;
                }
                position++;
            }
            
            return -1; // 任务不在队列中
        } catch (Exception e) {
            log.error("获取任务位置失败，任务ID: {}, 队列ID: {}", taskId, queueId, e);
            return -1;
        }
    }
    
    @Override
    public boolean clearQueue(String queueId) {
        try {
            Queue<String> queue = queues.get(queueId);
            if (queue != null) {
                // 清理任务存储中的相关任务
                for (String taskId : queue) {
                    tasks.remove(taskId);
                }
                
                // 清空队列
                queue.clear();
                
                // 重置位置计数器
                AtomicInteger counter = positionCounters.get(queueId);
                if (counter != null) {
                    counter.set(0);
                }
                
                log.info("队列清空成功，队列ID: {}", queueId);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("队列清空失败，队列ID: {}", queueId, e);
            return false;
        }
    }
    
    @Override
    public boolean queueExists(String queueId) {
        try {
            return queues.containsKey(queueId);
        } catch (Exception e) {
            log.error("检查队列存在性失败，队列ID: {}", queueId, e);
            return false;
        }
    }
    
    @Override
    public int restoreQueue(String queueId) {
        try {
            // 内存队列不需要恢复，直接返回当前队列长度
            int queueLength = getQueueLength(queueId);
            log.info("队列恢复完成（内存队列），队列ID: {}, 当前任务数: {}", queueId, queueLength);
            return queueLength;
        } catch (Exception e) {
            log.error("队列恢复失败，队列ID: {}", queueId, e);
            return 0;
        }
    }
    
    @Override
    public boolean persistQueue(String queueId) {
        try {
            // 内存队列不需要持久化操作
            log.debug("队列持久化完成（内存队列），队列ID: {}", queueId);
            return true;
        } catch (Exception e) {
            log.error("队列持久化失败，队列ID: {}", queueId, e);
            return false;
        }
    }
    
    /**
     * 更新所有任务的位置
     */
    private void updateAllTaskPositions(String queueId) {
        try {
            Queue<String> queue = queues.get(queueId);
            if (queue != null) {
                int position = 1;
                for (String taskId : queue) {
                    QueuedTask task = tasks.get(taskId);
                    if (task != null) {
                        task.setPosition(position++);
                    }
                }
            }
        } catch (Exception e) {
            log.error("更新所有任务位置失败，队列ID: {}", queueId, e);
        }
    }
    
    /**
     * 复制任务对象
     */
    private QueuedTask copyTask(QueuedTask original) {
        QueuedTask copy = new QueuedTask();
        copy.setTaskId(original.getTaskId());
        copy.setQueueId(original.getQueueId());
        copy.setQueuedAt(original.getQueuedAt());
        copy.setPosition(original.getPosition());
        copy.setStatus(original.getStatus());
        copy.setRetryCount(original.getRetryCount());
        copy.setLastRetryAt(original.getLastRetryAt());
        copy.setErrorMessage(original.getErrorMessage());
        copy.setStartedAt(original.getStartedAt());
        copy.setCompletedAt(original.getCompletedAt());
        return copy;
    }
}