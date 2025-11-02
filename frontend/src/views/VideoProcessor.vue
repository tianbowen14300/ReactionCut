<template>
  <div class="video-processor">
    <h2>视频处理与投稿</h2>
    
    <el-card class="input-card">
      <div slot="header">
        <span>处理任务配置</span>
      </div>
      <el-form :model="processForm" ref="processForm" label-width="100px">
        <el-form-item label="任务名称">
          <el-input 
            v-model="processForm.taskName" 
            placeholder="请输入任务名称"
            style="width: 300px;"
          ></el-input>
        </el-form-item>
        
        <el-form-item label="视频文件">
          <el-upload
            action="#"
            :auto-upload="false"
            :file-list="fileList"
            :on-change="handleFileChange"
            multiple
          >
            <el-button slot="trigger" size="small" type="primary">选择文件</el-button>
            <div slot="tip" class="el-upload__tip">请选择要处理的视频文件</div>
          </el-upload>
        </el-form-item>
      </el-form>
    </el-card>
    
    <el-card class="clip-card" v-if="clips.length > 0">
      <div slot="header">
        <span>视频片段设置</span>
      </div>
      <el-table :data="clips" style="width: 100%">
        <el-table-column prop="fileName" label="文件名" width="200"></el-table-column>
        <el-table-column prop="sequence" label="序号" width="80">
          <template slot-scope="scope">
            <el-input-number v-model="scope.row.sequence" :min="1" size="small"></el-input-number>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="150">
          <template slot-scope="scope">
            <el-time-picker
              v-model="scope.row.startTime"
              format="HH:mm:ss"
              value-format="HH:mm:ss"
              placeholder="选择开始时间">
            </el-time-picker>
          </template>
        </el-table-column>
        <el-table-column label="结束时间" width="150">
          <template slot-scope="scope">
            <el-time-picker
              v-model="scope.row.endTime"
              format="HH:mm:ss"
              value-format="HH:mm:ss"
              placeholder="选择结束时间">
            </el-time-picker>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template slot-scope="scope">
            <el-button @click="removeClip(scope.$index)" type="danger" size="mini">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 20px; text-align: center;">
        <el-button type="primary" @click="submitProcessTask" :loading="processing">提交处理任务</el-button>
      </div>
    </el-card>
    
    <el-card class="task-card" v-if="taskId">
      <div slot="header">
        <span>处理任务状态</span>
      </div>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="任务ID">{{ taskId }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ taskInfo.taskName || '获取中...' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getProcessStatusType(taskInfo.status)">
            {{ getProcessStatusText(taskInfo.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="进度">
          <el-progress :percentage="taskInfo.progress || 0"></el-progress>
        </el-descriptions-item>
        <el-descriptions-item label="投稿状态">
          <el-tag :type="getUploadStatusType(taskInfo.uploadStatus)">
            {{ getUploadStatusText(taskInfo.uploadStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="视频链接" v-if="taskInfo.bilibiliUrl">
          <a :href="taskInfo.bilibiliUrl" target="_blank">{{ taskInfo.bilibiliUrl }}</a>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script>
import { createProcessTask, getProcessStatus } from '@/api/video'

export default {
  name: 'VideoProcessor',
  data() {
    return {
      processForm: {
        taskName: ''
      },
      fileList: [],
      clips: [],
      processing: false,
      taskId: null,
      taskInfo: {}
    }
  },
  watch: {
    taskId: {
      handler(newVal) {
        if (newVal) {
          this.startPolling()
        }
      }
    }
  },
  methods: {
    // 处理文件变化
    handleFileChange(file, fileList) {
      this.fileList = fileList
      // 添加到片段列表
      const clip = {
        fileName: file.name,
        sequence: this.clips.length + 1,
        startTime: '00:00:00',
        endTime: '00:00:00'
      }
      this.clips.push(clip)
    },
    
    // 删除片段
    removeClip(index) {
      this.clips.splice(index, 1)
      // 重新排序序号
      this.clips.forEach((clip, i) => {
        clip.sequence = i + 1
      })
    },
    
    // 提交处理任务
    async submitProcessTask() {
      if (!this.processForm.taskName) {
        this.$message.warning('请输入任务名称')
        return
      }
      
      if (this.clips.length === 0) {
        this.$message.warning('请至少选择一个视频文件')
        return
      }
      
      this.processing = true
      try {
        // 转换时间格式
        const clips = this.clips.map(clip => ({
          ...clip,
          startTime: clip.startTime || '00:00:00',
          endTime: clip.endTime || '00:00:00'
        }))
        
        const response = await createProcessTask({
          taskName: this.processForm.taskName,
          clips: clips
        })
        
        if (response.code === 0) {
          this.$message.success('处理任务创建成功')
          this.taskId = response.data
          this.taskInfo = {}
        } else {
          this.$message.error(response.message)
        }
      } catch (error) {
        this.$message.error('创建处理任务失败: ' + error.message)
      } finally {
        this.processing = false
      }
    },
    
    // 开始轮询任务状态
    startPolling() {
      this.pollingTimer = setInterval(async () => {
        try {
          const response = await getProcessStatus(this.taskId)
          if (response.code === 0) {
            this.taskInfo = response.data
            // 如果任务完成或失败，停止轮询
            if (this.taskInfo.status === 2 || this.taskInfo.status === 3) {
              clearInterval(this.pollingTimer)
            }
          }
        } catch (error) {
          console.error('获取任务状态失败:', error)
        }
      }, 3000) // 每3秒查询一次
    },
    
    // 获取处理状态文本
    getProcessStatusText(status) {
      const statusMap = {
        0: '待处理',
        1: '处理中',
        2: '处理完成',
        3: '处理失败'
      }
      return statusMap[status] || '未知'
    },
    
    // 获取处理状态类型
    getProcessStatusType(status) {
      const typeMap = {
        0: 'info',
        1: 'warning',
        2: 'success',
        3: 'danger'
      }
      return typeMap[status] || 'info'
    },
    
    // 获取投稿状态文本
    getUploadStatusText(status) {
      const statusMap = {
        0: '未投稿',
        1: '投稿中',
        2: '投稿成功',
        3: '投稿失败'
      }
      return statusMap[status] || '未知'
    },
    
    // 获取投稿状态类型
    getUploadStatusType(status) {
      const typeMap = {
        0: 'info',
        1: 'warning',
        2: 'success',
        3: 'danger'
      }
      return typeMap[status] || 'info'
    }
  },
  
  beforeDestroy() {
    // 组件销毁前清除定时器
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer)
    }
  }
}
</script>

<style scoped>
.video-processor {
  padding: 20px;
}

.input-card, .clip-card, .task-card {
  margin-bottom: 20px;
}
</style>