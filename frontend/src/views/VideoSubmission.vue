<template>
  <div class="video-submission">
    <h2>视频剪辑与自动投稿</h2>
    
    <!-- 导航链接 -->
    <div style="margin-bottom: 20px;">
      <el-button type="primary" @click="$router.push('/process')">视频处理</el-button>
      <el-button type="success" @click="$router.push('/submission')">视频投稿</el-button>
    </div>
    
    <!-- 任务列表 -->
    <el-card class="task-list-card">
      <div slot="header" class="clearfix">
        <span>投稿任务列表</span>
        <el-button style="float: right; padding: 3px 0" type="primary" @click="showCreateTaskDialog">新增投稿任务</el-button>
      </div>
      
      <el-table :data="taskList" style="width: 100%" border>
        <el-table-column prop="taskId" label="任务ID" width="200"></el-table-column>
        <el-table-column prop="title" label="标题" min-width="200"></el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template slot-scope="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bvid" label="BVID" width="120"></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180"></el-table-column>
        <el-table-column label="操作" width="300">
          <template slot-scope="scope">
            <el-button 
              size="mini" 
              @click="viewTaskDetails(scope.row)">
              查看详情
            </el-button>
            <el-button 
              size="mini" 
              @click="clipVideo(scope.row)">
              视频剪辑
            </el-button>
            <el-button 
              size="mini" 
              @click="mergeVideo(scope.row)">
              视频合并
            </el-button>
            <el-button 
              size="mini" 
              @click="segmentVideo(scope.row)">
              视频分段
            </el-button>
            <el-button 
              size="mini" 
              type="success"
              @click="submitVideo(scope.row)">
              视频投稿
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div style="margin-top: 20px; text-align: center;">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="totalTasks">
        </el-pagination>
      </div>
    </el-card>
    
    <!-- 新增任务对话框 -->
    <el-dialog title="新增投稿任务" :visible.sync="createTaskDialogVisible" width="60%">
      <el-form :model="taskForm" :rules="taskRules" ref="taskForm" label-width="120px">
        <!-- 投稿基本信息 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="投稿标题" prop="title">
              <el-input v-model="taskForm.title" placeholder="请输入投稿标题"></el-input>
            </el-form-item>
          </el-col>
          
          <el-col :span="12">
            <el-form-item label="B站分区" prop="partitionId">
              <el-select v-model="taskForm.partitionId" placeholder="请选择分区">
                <el-option label="生活" value="171"></el-option>
                <el-option label="知识" value="201"></el-option>
                <el-option label="科技" value="181"></el-option>
                <el-option label="游戏" value="191"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <!-- 添加分段前缀输入框 -->
        <el-form-item label="分段前缀" prop="segmentPrefix">
          <el-input v-model="taskForm.segmentPrefix" placeholder="请输入分段前缀（可选）"></el-input>
          <div class="el-form-item-tip">在对合并的视频进行分段时，每个分段视频的文件名都会加上该前缀</div>
        </el-form-item>
        
        <el-form-item label="视频简介" prop="description">
          <el-input 
            type="textarea" 
            :rows="4" 
            v-model="taskForm.description" 
            placeholder="请输入视频简介">
          </el-input>
        </el-form-item>
        
        <el-form-item label="标签" prop="tags">
          <el-input v-model="taskForm.tags" placeholder="请输入标签，用逗号分隔"></el-input>
        </el-form-item>
        
        <el-form-item label="视频类型" prop="videoType">
          <el-radio-group v-model="taskForm.videoType">
            <el-radio label="ORIGINAL">原创</el-radio>
            <el-radio label="REPOST">转载</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <!-- 源视频列表 -->
        <el-form-item label="源视频列表">
          <el-button type="primary" @click="addSourceVideo" size="small">添加视频</el-button>
        </el-form-item>
        
        <el-table :data="taskForm.sourceVideos" style="width: 100%" border>
          <el-table-column label="序号" width="60">
            <template slot-scope="scope">
              {{ scope.$index + 1 }}
            </template>
          </el-table-column>
          
          <el-table-column label="视频文件" min-width="200">
            <template slot-scope="scope">
              <el-input 
                v-model="scope.row.sourceFilePath" 
                placeholder="请输入视频文件路径或点击选择">
                <el-button slot="append" @click="selectVideoFile(scope.$index)">选择</el-button>
              </el-input>
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
          
          <el-table-column label="操作" width="80">
            <template slot-scope="scope">
              <el-button 
                type="danger" 
                icon="el-icon-delete" 
                circle 
                size="mini"
                @click="removeSourceVideo(scope.$index)">
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      
      <span slot="footer" class="dialog-footer">
        <el-button @click="createTaskDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTask">提交</el-button>
      </span>
    </el-dialog>
    
    <!-- 文件选择对话框 -->
    <el-dialog title="选择视频文件" :visible.sync="fileSelectorDialogVisible" width="60%">
      <div style="margin-bottom: 15px;">
        <el-button @click="goToParentDirectory" :disabled="currentPath === basePath">返回上级</el-button>
        <span style="margin-left: 15px;">当前路径: {{ currentPath }}</span>
      </div>
      
      <el-table :data="fileList" height="400" @row-dblclick="handleFileDoubleClick">
        <el-table-column label="名称" min-width="200">
          <template slot-scope="scope">
            <i :class="scope.row.isDirectory ? 'el-icon-folder' : 'el-icon-document'"></i>
            <span style="margin-left: 10px;">{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="size" label="大小" width="120">
          <template slot-scope="scope">
            <span v-if="!scope.row.isDirectory">{{ formatFileSize(scope.row.size) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastModified" label="修改时间" width="180">
          <template slot-scope="scope">
            {{ formatDateTime(scope.row.lastModified) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template slot-scope="scope">
            <el-button 
              size="mini" 
              @click="selectFile(scope.row)"
              :disabled="!scope.row.isDirectory && !isVideoFile(scope.row.name)">
              选择
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <span slot="footer" class="dialog-footer">
        <el-button @click="fileSelectorDialogVisible = false">取消</el-button>
      </span>
    </el-dialog>
    
    <!-- 任务详情对话框 -->
    <el-dialog title="任务详情" :visible.sync="taskDetailDialogVisible" width="60%">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-form label-width="120px">
            <el-form-item label="任务ID">
              <span>{{ currentTask.taskId }}</span>
            </el-form-item>
            <el-form-item label="标题">
              <span>{{ currentTask.title }}</span>
            </el-form-item>
            <el-form-item label="状态">
              <el-tag :type="getStatusTagType(currentTask.status)">
                {{ getStatusText(currentTask.status) }}
              </el-tag>
            </el-form-item>
            <el-form-item label="BVID">
              <span>{{ currentTask.bvid }}</span>
            </el-form-item>
            <el-form-item label="创建时间">
              <span>{{ currentTask.createdAt }}</span>
            </el-form-item>
            <el-form-item label="更新时间">
              <span>{{ currentTask.updatedAt }}</span>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <el-tab-pane label="源视频" name="source">
          <el-table :data="currentTaskSourceVideos" style="width: 100%" border>
            <el-table-column label="序号" width="60">
              <template slot-scope="scope">
                {{ scope.$index + 1 }}
              </template>
            </el-table-column>
            <el-table-column prop="sourceFilePath" label="视频文件路径"></el-table-column>
            <el-table-column prop="startTime" label="开始时间" width="120"></el-table-column>
            <el-table-column prop="endTime" label="结束时间" width="120"></el-table-column>
          </el-table>
        </el-tab-pane>
        
        <el-tab-pane label="合并视频" name="merged">
          <el-table :data="currentTaskMergedVideos" style="width: 100%" border>
            <el-table-column label="序号" width="60">
              <template slot-scope="scope">
                {{ scope.$index + 1 }}
              </template>
            </el-table-column>
            <el-table-column prop="fileName" label="文件名" min-width="200"></el-table-column>
            <el-table-column prop="videoPath" label="文件路径" min-width="300"></el-table-column>
            <el-table-column prop="status" label="状态" width="120">
              <template slot-scope="scope">
                <el-tag :type="getMergedVideoStatusTagType(scope.row.status)">
                  {{ getMergedVideoStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180"></el-table-column>
          </el-table>
        </el-tab-pane>
        
        <el-tab-pane label="输出分段" name="segments">
          <el-table :data="currentTaskOutputSegments" style="width: 100%" border>
            <el-table-column label="序号" width="60">
              <template slot-scope="scope">
                {{ scope.$index + 1 }}
              </template>
            </el-table-column>
            <el-table-column prop="partName" label="P名称" width="100"></el-table-column>
            <el-table-column prop="segmentFilePath" label="文件路径"></el-table-column>
            <el-table-column prop="uploadStatus" label="上传状态" width="120">
              <template slot-scope="scope">
                <el-tag :type="getUploadStatusTagType(scope.row.uploadStatus)">
                  {{ getUploadStatusText(scope.row.uploadStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="cid" label="CID" width="120"></el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
      
      <span slot="footer" class="dialog-footer">
        <el-button @click="taskDetailDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { createTask, getAllTasks, getTasksByStatus, getTaskById, clipVideos, mergeVideos, segmentVideo, getMergedVideos, executeTask } from '@/api/submission'
import { scanPath } from '@/api/fileScanner'

export default {
  name: 'VideoSubmission',
  data() {
    return {
      taskForm: {
        title: '',
        description: '',
        coverUrl: '',
        partitionId: '',
        tags: '',
        videoType: 'ORIGINAL',
        segmentPrefix: '', // 添加分段前缀字段
        sourceVideos: [
          {
            sourceFilePath: '',
            sortOrder: 1,
            startTime: '00:00:00',
            endTime: '00:00:00'
          }
        ]
      },
      taskRules: {
        title: [
          { required: true, message: '请输入投稿标题', trigger: 'blur' }
        ],
        partitionId: [
          { required: true, message: '请选择B站分区', trigger: 'change' }
        ],
        videoType: [
          { required: true, message: '请选择视频类型', trigger: 'change' }
        ]
      },
      taskList: [],
      currentPage: 1,
      pageSize: 10,
      totalTasks: 0,
      taskDetailDialogVisible: false,
      createTaskDialogVisible: false,
      fileSelectorDialogVisible: false,
      currentTask: {},
      currentTaskSourceVideos: [],
      currentTaskMergedVideos: [],
      currentTaskOutputSegments: [],
      activeTab: 'basic',
      fileList: [],
      currentPath: '',
      basePath: '/Users/tbw/Reaction/cut',
      currentVideoIndex: -1
    }
  },
  mounted() {
    this.loadTasks()
  },
  methods: {
    showCreateTaskDialog() {
      this.createTaskDialogVisible = true
      this.$nextTick(() => {
        this.resetForm()
      })
    },
    
    addSourceVideo() {
      this.taskForm.sourceVideos.push({
        sourceFilePath: '',
        sortOrder: this.taskForm.sourceVideos.length + 1,
        startTime: '00:00:00',
        endTime: '00:00:00'
      })
    },
    
    removeSourceVideo(index) {
      this.taskForm.sourceVideos.splice(index, 1)
      // 重新排序
      this.taskForm.sourceVideos.forEach((video, i) => {
        video.sortOrder = i + 1
      })
    },
    
    selectVideoFile(index) {
      this.currentVideoIndex = index
      this.currentPath = this.basePath
      this.loadFileList(this.currentPath)
      this.fileSelectorDialogVisible = true
    },
    
    async loadFileList(path) {
      try {
        this.fileList = await scanPath(path)
        this.currentPath = path
      } catch (error) {
        this.$message.error('加载文件列表失败: ' + error.message)
      }
    },
    
    handleFileDoubleClick(row) {
      if (row.isDirectory) {
        this.loadFileList(row.path)
      } else if (this.isVideoFile(row.name)) {
        this.selectFile(row)
      }
    },
    
    selectFile(file) {
      if (file.isDirectory) {
        this.loadFileList(file.path)
      } else if (this.isVideoFile(file.name)) {
        this.taskForm.sourceVideos[this.currentVideoIndex].sourceFilePath = file.path
        this.fileSelectorDialogVisible = false
      }
    },
    
    goToParentDirectory() {
      if (this.currentPath !== this.basePath) {
        const parentPath = this.currentPath.substring(0, this.currentPath.lastIndexOf('/'))
        this.loadFileList(parentPath || '/')
      }
    },
    
    isVideoFile(fileName) {
      const videoExtensions = ['.mp4', '.avi', '.mov', '.wmv', '.flv', '.mkv', '.webm']
      const lowerFileName = fileName.toLowerCase()
      return videoExtensions.some(ext => lowerFileName.endsWith(ext))
    },
    
    formatFileSize(size) {
      if (size === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(size) / Math.log(k))
      return parseFloat((size / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    },
    
    formatDateTime(timestamp) {
      if (!timestamp) return ''
      const date = new Date(timestamp)
      return date.toLocaleString('zh-CN')
    },
    
    submitTask() {
      this.$refs.taskForm.validate((valid) => {
        if (valid) {
          // 提交任务
          const taskData = {
            task: {
              title: this.taskForm.title,
              description: this.taskForm.description,
              coverUrl: this.taskForm.coverUrl,
              partitionId: parseInt(this.taskForm.partitionId),
              tags: this.taskForm.tags,
              videoType: this.taskForm.videoType,
              segmentPrefix: this.taskForm.segmentPrefix // 添加分段前缀字段
            },
            sourceVideos: this.taskForm.sourceVideos
          }
          
          createTask(taskData).then(response => {
            this.$message.success('任务创建成功')
            this.createTaskDialogVisible = false
            this.resetForm()
            this.loadTasks()
          }).catch(error => {
            this.$message.error('任务创建失败: ' + error.message)
          })
        } else {
          this.$message.error('请填写必填项')
          return false
        }
      })
    },
    
    resetForm() {
      this.$refs.taskForm.resetFields();
      this.taskForm.sourceVideos = [
        {
          sourceFilePath: '',
          sortOrder: 1,
          startTime: '00:00:00',
          endTime: '00:00:00'
        }
      ];
      this.taskForm.segmentPrefix = ''; // 重置分段前缀字段
    },
    
    loadTasks() {
      // 加载所有任务
      getAllTasks().then(response => {
        console.log('response', response)
        if (response.code === 0) {
          this.taskList = response.data
          this.totalTasks = response.data.length
        } else {
          this.$message.error('加载任务列表失败: ' + response.message)
        }
      }).catch(error => {
        this.$message.error('加载任务列表失败: ' + error.message)
      })
    },
    
    handleSizeChange(val) {
      this.pageSize = val
      this.loadTasks()
    },
    
    handleCurrentChange(val) {
      this.currentPage = val
      this.loadTasks()
    },
    
    clipVideo(task) {
      this.$message.info('开始视频剪辑，任务ID: ' + task.taskId)
      // 调用后端接口进行视频剪辑
      clipVideos(task.taskId).then(response => {
        this.$message.success('视频剪辑完成')
        console.log('剪辑后的文件路径:', response)
      }).catch(error => {
        this.$message.error('视频剪辑失败: ' + error.message)
      })
    },
    
    mergeVideo(task) {
      this.$message.info('开始视频合并，任务ID: ' + task.taskId)
      // 调用后端接口进行视频合并，不需要传递clipPaths
      mergeVideos(task.taskId).then(response => {
        this.$message.success('视频合并完成')
        console.log('合并后的文件路径:', response)
      }).catch(error => {
        this.$message.error('视频合并失败: ' + error.message)
      })
    },
    
    segmentVideo(task) {
      this.$message.info('开始视频分段，任务ID: ' + task.taskId)
      // 调用后端接口进行视频分段，不需要传递mergedVideoPath
      segmentVideo(task.taskId).then(response => {
        this.$message.success('视频分段完成')
        console.log('分段后的文件路径:', response)
      }).catch(error => {
        this.$message.error('视频分段失败: ' + error.message)
      })
    },
    
    submitVideo(task) {
      this.$message.info('开始视频投稿，任务ID: ' + task.taskId)
      // 调用后端接口进行视频投稿
      // 这里需要调用后端的执行任务接口
      this.$confirm('确定要投稿这个视频吗？这将开始完整的投稿流程。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 调用后端执行任务接口
        executeTask(task.taskId).then(response => {
          this.$message.success('投稿任务已开始执行')
        }).catch(error => {
          this.$message.error('投稿任务执行失败: ' + error.message)
        })
      }).catch(() => {
        this.$message.info('已取消投稿')
      })
    },
    
    viewTaskDetails(task) {
      // 获取任务详情
      getTaskById(task.taskId).then(response => {
        if (response.code === 0) {
          const taskDetail = response.data;
          this.currentTask = taskDetail.task;
          this.currentTaskSourceVideos = taskDetail.sourceVideos;
          this.currentTaskOutputSegments = taskDetail.outputSegments;
          this.currentTaskMergedVideos = taskDetail.mergedVideos;
        } else {
          this.$message.error('获取任务详情失败: ' + response.message);
        }
      }).catch(error => {
        this.$message.error('获取任务详情失败: ' + error.message);
      });
      
      this.taskDetailDialogVisible = true;
    },
    
    getStatusText(status) {
      const statusMap = {
        'PENDING': '待处理',
        'PROCESSING': '处理中',
        'CLIPPING': '剪辑中',
        'SEGMENTING': '分段中',
        'UPLOADING': '上传中',
        'COMPLETED': '已完成',
        'FAILED': '失败'
      }
      return statusMap[status] || status
    },
    
    getStatusTagType(status) {
      const typeMap = {
        'PENDING': '',
        'PROCESSING': 'primary',
        'CLIPPING': 'primary',
        'SEGMENTING': 'primary',
        'UPLOADING': 'warning',
        'COMPLETED': 'success',
        'FAILED': 'danger'
      }
      return typeMap[status] || ''
    },
    
    getMergedVideoStatusText(status) {
      const statusMap = {
        0: '待处理',
        1: '处理中',
        2: '处理完成',
        3: '处理失败'
      }
      return statusMap[status] || '未知'
    },
    
    getMergedVideoStatusTagType(status) {
      const typeMap = {
        0: 'info',
        1: 'warning',
        2: 'success',
        3: 'danger'
      }
      return typeMap[status] || 'info'
    },
    
    getUploadStatusText(status) {
      const statusMap = {
        'PENDING': '待上传',
        'UPLOADING': '上传中',
        'SUCCESS': '成功',
        'FAILED': '失败'
      }
      return statusMap[status] || status
    },
    
    getUploadStatusTagType(status) {
      const typeMap = {
        'PENDING': '',
        'UPLOADING': 'warning',
        'SUCCESS': 'success',
        'FAILED': 'danger'
      }
      return typeMap[status] || ''
    }
  }
}
</script>

<style scoped>
.video-submission {
  padding: 20px;
}

.task-form-card, .task-list-card {
  margin-bottom: 20px;
}

.el-form-item {
  margin-bottom: 20px;
}
</style>