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
        <el-table-column prop="status" label="任务状态" width="120">
          <template slot-scope="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <!-- 工作流状态列 -->
        <el-table-column label="工作流状态" width="200">
          <template slot-scope="scope">
            <div v-if="scope.row.workflowStatus">
              <div class="workflow-status-container">
                <el-tag 
                  :type="getWorkflowStatusTagType(scope.row.workflowStatus.status)"
                  size="mini">
                  {{ scope.row.workflowStatus.statusDescription }}
                </el-tag>
                
                <!-- 进度条 -->
                <el-progress 
                  v-if="scope.row.workflowStatus.status === 'RUNNING'"
                  :percentage="scope.row.workflowStatus.progress"
                  :stroke-width="6"
                  :show-text="false"
                  class="workflow-progress">
                </el-progress>
                
                <!-- 当前步骤指示器 -->
                <div v-if="scope.row.workflowStatus.steps && scope.row.workflowStatus.steps.length > 0" 
                     class="workflow-steps">
                  <el-tooltip 
                    v-for="(step, index) in scope.row.workflowStatus.steps" 
                    :key="step.stepId"
                    :content="getStepTooltipContent(step)"
                    placement="top">
                    <span 
                      :class="getStepIndicatorClass(step, index, scope.row.workflowStatus.currentStepIndex)"
                      class="step-indicator">
                      {{ getStepIcon(step.type) }}
                    </span>
                  </el-tooltip>
                </div>
              </div>
            </div>
            <span v-else class="no-workflow">无工作流</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="bvid" label="BVID" width="120"></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180"></el-table-column>
        <el-table-column label="操作" width="400">
          <template slot-scope="scope">
            <!-- 工作流控制按钮 -->
            <div v-if="scope.row.workflowStatus" class="workflow-controls">
              <el-button-group>
                <el-button 
                  v-if="scope.row.workflowStatus.status === 'RUNNING'"
                  size="mini" 
                  type="warning"
                  icon="el-icon-video-pause"
                  @click="pauseTaskWorkflow(scope.row)">
                  暂停
                </el-button>
                <el-button 
                  v-if="scope.row.workflowStatus.status === 'PAUSED'"
                  size="mini" 
                  type="success"
                  icon="el-icon-video-play"
                  @click="resumeTaskWorkflow(scope.row)">
                  恢复
                </el-button>
                <el-button 
                  v-if="['RUNNING', 'PAUSED'].includes(scope.row.workflowStatus.status)"
                  size="mini" 
                  type="danger"
                  icon="el-icon-close"
                  @click="cancelTaskWorkflow(scope.row)">
                  取消
                </el-button>
                <el-button 
                  size="mini" 
                  icon="el-icon-refresh"
                  @click="refreshWorkflowStatus(scope.row)">
                  刷新
                </el-button>
              </el-button-group>
            </div>
            
            <!-- 传统操作按钮 -->
            <div class="traditional-controls">
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
            </div>
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
    <el-dialog title="新增投稿任务" :visible.sync="createTaskDialogVisible" width="80%">
      <el-tabs v-model="createTaskActiveTab" type="card">
        <!-- 工作流配置标签页 -->
        <el-tab-pane label="工作流配置" name="workflow">
          <el-form :model="workflowConfig" label-width="120px">
            <el-form-item label="处理模式">
              <el-radio-group v-model="workflowConfig.enableDirectSubmission">
                <el-radio :label="true">直接投稿</el-radio>
                <el-radio :label="false">分段处理后投稿</el-radio>
              </el-radio-group>
              <div style="font-size: 12px; color: #999; margin-top: 5px;">
                直接投稿：处理完成后直接进行投稿<br>
                分段处理：将视频分割为多个片段后再投稿
              </div>
            </el-form-item>
            
            <!-- 分段配置 -->
            <div v-if="!workflowConfig.enableDirectSubmission" class="segmentation-config">
              <el-form-item label="分段时长">
                <el-input-number
                  v-model="workflowConfig.segmentationConfig.segmentDurationSeconds"
                  :min="30"
                  :max="600"
                  :step="1"
                  controls-position="right"
                  style="width: 200px;"
                ></el-input-number>
                <span style="margin-left: 10px; color: #666;">秒</span>
                <div style="font-size: 12px; color: #999; margin-top: 5px;">
                  推荐：133秒（2分13秒），范围：30-600秒
                </div>
              </el-form-item>
              
              <el-form-item label="最大分段数">
                <el-input-number
                  v-model="workflowConfig.segmentationConfig.maxSegmentCount"
                  :min="1"
                  :max="100"
                  :step="1"
                  controls-position="right"
                  style="width: 200px;"
                ></el-input-number>
                <span style="margin-left: 10px; color: #666;">个</span>
                <div style="font-size: 12px; color: #999; margin-top: 5px;">
                  超过此数量的分段将被合并
                </div>
              </el-form-item>
              
              <el-form-item label="保留原文件">
                <el-switch
                  v-model="workflowConfig.segmentationConfig.preserveOriginal"
                  active-text="保留"
                  inactive-text="删除"
                ></el-switch>
                <div style="font-size: 12px; color: #999; margin-top: 5px;">
                  是否在分段完成后保留原始合并文件
                </div>
              </el-form-item>
            </div>
            
            <!-- 处理步骤配置 -->
            <el-form-item label="处理步骤">
              <el-checkbox-group v-model="enabledProcessingSteps">
                <el-checkbox label="clipping" :disabled="true">视频剪辑</el-checkbox>
                <el-checkbox label="merging" :disabled="true">视频合并</el-checkbox>
                <el-checkbox 
                  label="segmentation" 
                  :disabled="workflowConfig.enableDirectSubmission"
                >
                  视频分段
                </el-checkbox>
              </el-checkbox-group>
              <div style="font-size: 12px; color: #999; margin-top: 5px;">
                剪辑和合并步骤始终启用，分段步骤根据处理模式自动控制
              </div>
            </el-form-item>
            
            <!-- 配置预览 -->
            <el-form-item label="配置预览">
              <div class="workflow-preview">
                <div class="preview-item">
                  <span class="preview-label">处理流程：</span>
                  <span class="preview-value">
                    剪辑 → 合并
                    <span v-if="!workflowConfig.enableDirectSubmission"> → 分段</span>
                    → 投稿
                  </span>
                </div>
                <div v-if="!workflowConfig.enableDirectSubmission" class="preview-item">
                  <span class="preview-label">分段设置：</span>
                  <span class="preview-value">
                    每段{{ workflowConfig.segmentationConfig.segmentDurationSeconds }}秒，
                    最多{{ workflowConfig.segmentationConfig.maxSegmentCount }}段
                  </span>
                </div>
              </div>
            </el-form-item>
            
            <!-- 配置模板 -->
            <el-form-item label="配置模板">
              <el-button-group>
                <el-button size="small" @click="saveWorkflowTemplate">保存为模板</el-button>
                <el-button size="small" @click="loadWorkflowTemplate">加载模板</el-button>
                <el-button size="small" @click="resetWorkflowConfig">重置配置</el-button>
              </el-button-group>
              <div style="font-size: 12px; color: #999; margin-top: 5px;">
                可以保存常用的工作流配置作为模板，方便下次使用
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <!-- 投稿配置标签页 -->
        <el-tab-pane label="投稿配置" name="submission">
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
                    <el-option
                      v-for="partition in partitions"
                      :key="partition.tid"
                      :label="partition.name"
                      :value="partition.tid">
                    </el-option>
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="视频合集" prop="collectionId">
                  <el-select v-model="taskForm.collectionId" placeholder="请选择合集" clearable>
                    <el-option
                      v-for="collection in collections"
                      :key="collection.seasonId"
                      :label="collection.name"
                      :value="collection.seasonId">
                    </el-option>
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
          </el-form>
        </el-tab-pane>
        
        <!-- 源视频配置标签页 -->
        <el-tab-pane label="源视频配置" name="sourceVideos">
          <div class="source-videos-config">
            <div class="source-videos-header">
              <span>源视频列表</span>
              <el-button type="primary" @click="addSourceVideo" size="small">添加视频</el-button>
            </div>
            
            <el-table :data="taskForm.sourceVideos" style="width: 100%" border>
              <el-table-column label="序号" width="60">
                <template slot-scope="scope">
                  {{ scope.$index + 1 }}
                </template>
              </el-table-column>
              
              <el-table-column label="视频文件" min-width="300">
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
          </div>
        </el-tab-pane>
      </el-tabs>
      
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
import { getUserCollections, getAllPartitions } from '@/api/bilibili'
import { getWorkflowStatus, pauseWorkflow, resumeWorkflow, cancelWorkflow } from '@/api/workflow'

export default {
  name: 'VideoSubmission',
  data() {
    return {
      taskForm: {
        title: '',
        description: '',
        coverUrl: '',
        partitionId: '',
        collectionId: '', // 添加合集ID字段
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
      // 工作流配置
      workflowConfig: {
        enableDirectSubmission: true,
        segmentationConfig: {
          enabled: false,
          segmentDurationSeconds: 133,
          maxSegmentCount: 50,
          preserveOriginal: true
        },
        enableClipping: true,
        enableMerging: true
      },
      enabledProcessingSteps: ['clipping', 'merging'],
      workflowTemplates: [], // 工作流模板
      createTaskActiveTab: 'workflow', // 新增任务对话框的活动标签页
      workflowStatusRefreshTimer: null, // 工作流状态刷新定时器
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
      basePath: '/Users/tbw/Reaction',
      currentVideoIndex: -1,
      // 添加合集和分区数据
      collections: [], // 合集列表
      partitions: []   // 分区列表
    }
  },
  watch: {
    // 监听工作流配置变化
    'workflowConfig.enableDirectSubmission': function(newVal) {
      // 更新分段配置的启用状态
      this.workflowConfig.segmentationConfig.enabled = !newVal
      
      // 更新处理步骤
      this.updateEnabledProcessingSteps()
      
      // 验证配置
      this.validateWorkflowConfig()
    },
    
    'workflowConfig.segmentationConfig.segmentDurationSeconds': function(newVal) {
      this.validateSegmentationConfig()
    },
    
    'workflowConfig.segmentationConfig.maxSegmentCount': function(newVal) {
      this.validateSegmentationConfig()
    }
  },
  mounted() {
    this.loadTasks()
    this.loadCollectionsAndPartitions() // 加载合集和分区数据
    
    // 启动工作流状态定时刷新（每30秒刷新一次）
    this.startWorkflowStatusRefresh()
  },
  
  beforeDestroy() {
    // 清理定时器
    this.stopWorkflowStatusRefresh()
  },
  methods: {
    // 加载合集和分区数据
    async loadCollectionsAndPartitions() {
      try {
        // 加载合集数据（这里使用默认的用户ID 37737161，实际应用中应该动态获取）
        const collectionsResponse = await getUserCollections(37737161);
        this.collections = collectionsResponse;
        
        // 加载分区数据
        const partitionsResponse = await getAllPartitions();
        // 处理嵌套的分区结构，将子分区也展平到一级列表中
        const flatPartitions = [];
        partitionsResponse.forEach(partition => {
          // 添加主分区
          flatPartitions.push({
            tid: partition.tid,
            name: partition.name
          });
          
          // 添加子分区（如果有）
          if (partition.children && partition.children.length > 0) {
            partition.children.forEach(child => {
              flatPartitions.push({
                tid: child.tid,
                name: `${partition.name} - ${child.name}`
              });
            });
          }
        });
        this.partitions = flatPartitions;
      } catch (error) {
        this.$message.error('加载合集和分区数据失败: ' + error.message);
      }
    },
    
    showCreateTaskDialog() {
      this.createTaskDialogVisible = true
      this.createTaskActiveTab = 'workflow' // 默认显示工作流配置标签页
      this.$nextTick(() => {
        this.resetForm()
        this.initializeWorkflowConfig()
      })
    },
    
    // 初始化工作流配置
    initializeWorkflowConfig() {
      // 使用投稿任务的默认配置
      this.workflowConfig = {
        enableDirectSubmission: false, // 投稿任务默认启用分段处理
        segmentationConfig: {
          enabled: true,
          segmentDurationSeconds: 133,
          maxSegmentCount: 50,
          preserveOriginal: true
        },
        enableClipping: true,
        enableMerging: true
      }
      
      // 更新处理步骤
      this.updateEnabledProcessingSteps()
    },
    
    // 更新启用的处理步骤
    updateEnabledProcessingSteps() {
      this.enabledProcessingSteps = ['clipping', 'merging']
      
      if (!this.workflowConfig.enableDirectSubmission) {
        this.enabledProcessingSteps.push('segmentation')
      }
    },
    
    // 验证工作流配置
    validateWorkflowConfig() {
      if (!this.workflowConfig.enableDirectSubmission) {
        return this.validateSegmentationConfig()
      }
      return { valid: true }
    },
    
    // 验证分段配置
    validateSegmentationConfig() {
      const config = this.workflowConfig.segmentationConfig
      
      if (config.segmentDurationSeconds < 30 || config.segmentDurationSeconds > 600) {
        return {
          valid: false,
          message: '分段时长必须在30-600秒之间'
        }
      }
      
      if (config.maxSegmentCount < 1 || config.maxSegmentCount > 100) {
        return {
          valid: false,
          message: '最大分段数量必须在1-100之间'
        }
      }
      
      return { valid: true }
    },
    
    // 保存工作流模板
    saveWorkflowTemplate() {
      this.$prompt('请输入模板名称', '保存工作流模板', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /\S+/,
        inputErrorMessage: '模板名称不能为空'
      }).then(({ value }) => {
        const template = {
          name: value,
          config: JSON.parse(JSON.stringify(this.workflowConfig)),
          createdAt: new Date().toISOString()
        }
        
        // 保存到本地存储
        const templates = JSON.parse(localStorage.getItem('workflowTemplates') || '[]')
        templates.push(template)
        localStorage.setItem('workflowTemplates', JSON.stringify(templates))
        this.workflowTemplates = templates
        
        this.$message.success('工作流模板保存成功')
      }).catch(() => {
        this.$message.info('已取消保存')
      })
    },
    
    // 加载工作流模板
    loadWorkflowTemplate() {
      const templates = JSON.parse(localStorage.getItem('workflowTemplates') || '[]')
      this.workflowTemplates = templates
      
      if (templates.length === 0) {
        this.$message.info('暂无保存的工作流模板')
        return
      }
      
      const templateOptions = templates.map(t => ({ key: t.name, value: t.name }))
      
      this.$prompt('请选择要加载的模板', '加载工作流模板', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'select',
        inputOptions: templateOptions
      }).then(({ value }) => {
        const template = templates.find(t => t.name === value)
        if (template) {
          this.workflowConfig = JSON.parse(JSON.stringify(template.config))
          this.updateEnabledProcessingSteps()
          this.$message.success('工作流模板加载成功')
        }
      }).catch(() => {
        this.$message.info('已取消加载')
      })
    },
    
    // 重置工作流配置
    resetWorkflowConfig() {
      this.$confirm('确定要重置工作流配置吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.initializeWorkflowConfig()
        this.$message.success('工作流配置已重置')
      }).catch(() => {
        this.$message.info('已取消重置')
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
      // 验证工作流配置
      const workflowValidation = this.validateWorkflowConfig()
      if (!workflowValidation.valid) {
        this.$message.error(workflowValidation.message)
        this.createTaskActiveTab = 'workflow'
        return
      }
      
      this.$refs.taskForm.validate((valid) => {
        if (valid) {
          // 提交任务
          const taskData = {
            task: {
              title: this.taskForm.title,
              description: this.taskForm.description,
              coverUrl: this.taskForm.coverUrl,
              partitionId: parseInt(this.taskForm.partitionId),
              collectionId: this.taskForm.collectionId ? parseInt(this.taskForm.collectionId) : null, // 添加合集ID
              tags: this.taskForm.tags,
              videoType: this.taskForm.videoType,
              segmentPrefix: this.taskForm.segmentPrefix // 添加分段前缀字段
            },
            sourceVideos: this.taskForm.sourceVideos,
            workflowConfig: {
              userId: 'current_user', // TODO: 从用户会话获取真实用户ID
              enableDirectSubmission: this.workflowConfig.enableDirectSubmission,
              enableClipping: this.workflowConfig.enableClipping,
              enableMerging: this.workflowConfig.enableMerging,
              segmentationConfig: {
                enabled: !this.workflowConfig.enableDirectSubmission,
                segmentDurationSeconds: this.workflowConfig.segmentationConfig.segmentDurationSeconds,
                maxSegmentCount: this.workflowConfig.segmentationConfig.maxSegmentCount,
                segmentNamingPattern: '{title}_Part{index}',
                preserveOriginal: this.workflowConfig.segmentationConfig.preserveOriginal
              }
            }
          }
          
          createTask(taskData).then(response => {
            this.$message.success('任务创建成功')
            this.createTaskDialogVisible = false
            this.resetForm()
            this.loadTasks()
            
            // 显示任务创建结果
            this.showTaskCreationResult(response)
          }).catch(error => {
            this.$message.error('任务创建失败: ' + error.message)
          })
        } else {
          this.$message.error('请填写必填项')
          this.createTaskActiveTab = 'submission'
          return false
        }
      })
    },
    
    // 显示任务创建结果
    showTaskCreationResult(response) {
      let message = '任务创建成功！\n'
      
      if (response.taskId) {
        message += `任务ID: ${response.taskId}\n`
      }
      
      if (response.hasWorkflowInstance && response.hasWorkflowInstance()) {
        message += `工作流实例ID: ${response.workflowInstanceId}\n`
        message += `工作流状态: ${response.workflowStatus}\n`
      }
      
      if (response.hasWorkflowError && response.hasWorkflowError()) {
        message += `\n⚠️ 工作流启动失败: ${response.workflowError}\n`
        message += '任务已创建，但需要手动执行处理步骤\n'
      }
      
      // 显示工作流配置信息
      if (this.workflowConfig) {
        message += '\n工作流配置:\n'
        message += `处理模式: ${this.workflowConfig.enableDirectSubmission ? '直接投稿' : '分段处理后投稿'}\n`
        
        if (!this.workflowConfig.enableDirectSubmission) {
          message += `分段时长: ${this.workflowConfig.segmentationConfig.segmentDurationSeconds}秒\n`
          message += `最大分段数: ${this.workflowConfig.segmentationConfig.maxSegmentCount}个\n`
        }
      }
      
      const alertType = (response.hasWorkflowError && response.hasWorkflowError()) ? 'warning' : 'success'
      
      this.$alert(message, '任务创建结果', {
        confirmButtonText: '确定',
        type: alertType
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
      
      // 重置工作流配置
      this.initializeWorkflowConfig();
    },
    
    loadTasks() {
      // 加载所有任务
      getAllTasks().then(response => {
        console.log('response', response)
        if (response.code === 0) {
          this.taskList = response.data
          this.totalTasks = response.data.length
          
          // 为每个任务加载工作流状态
          this.loadWorkflowStatusForTasks()
        } else {
          this.$message.error('加载任务列表失败: ' + response.message)
        }
      }).catch(error => {
        this.$message.error('加载任务列表失败: ' + error.message)
      })
    },
    
    // ==================== 工作流状态监控方法 ====================
    
    /**
     * 为所有任务加载工作流状态
     */
    async loadWorkflowStatusForTasks() {
      for (let task of this.taskList) {
        try {
          const response = await getWorkflowStatus(task.taskId)
          if (response.code === 0) {
            // 使用Vue.set确保响应式更新
            this.$set(task, 'workflowStatus', response.data)
          }
        } catch (error) {
          // 如果任务没有工作流，忽略错误
          console.log(`任务 ${task.taskId} 没有工作流状态`)
        }
      }
    },
    
    /**
     * 刷新单个任务的工作流状态
     */
    async refreshWorkflowStatus(task) {
      try {
        const response = await getWorkflowStatus(task.taskId)
        if (response.code === 0) {
          this.$set(task, 'workflowStatus', response.data)
          this.$message.success('工作流状态已刷新')
        } else {
          this.$message.error('刷新工作流状态失败: ' + response.message)
        }
      } catch (error) {
        this.$message.error('刷新工作流状态失败: ' + error.message)
      }
    },
    
    /**
     * 暂停任务工作流
     */
    async pauseTaskWorkflow(task) {
      try {
        const response = await pauseWorkflow(task.taskId)
        if (response.code === 0) {
          this.$message.success('工作流已暂停')
          // 刷新工作流状态
          await this.refreshWorkflowStatus(task)
        } else {
          this.$message.error('暂停工作流失败: ' + response.message)
        }
      } catch (error) {
        this.$message.error('暂停工作流失败: ' + error.message)
      }
    },
    
    /**
     * 恢复任务工作流
     */
    async resumeTaskWorkflow(task) {
      try {
        const response = await resumeWorkflow(task.taskId)
        if (response.code === 0) {
          this.$message.success('工作流已恢复')
          // 刷新工作流状态
          await this.refreshWorkflowStatus(task)
        } else {
          this.$message.error('恢复工作流失败: ' + response.message)
        }
      } catch (error) {
        this.$message.error('恢复工作流失败: ' + error.message)
      }
    },
    
    /**
     * 取消任务工作流
     */
    async cancelTaskWorkflow(task) {
      try {
        await this.$confirm('确定要取消这个工作流吗？取消后无法恢复。', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const response = await cancelWorkflow(task.taskId)
        if (response.code === 0) {
          this.$message.success('工作流已取消')
          // 刷新工作流状态
          await this.refreshWorkflowStatus(task)
        } else {
          this.$message.error('取消工作流失败: ' + response.message)
        }
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('取消工作流失败: ' + error.message)
        }
      }
    },
    
    /**
     * 获取工作流状态标签类型
     */
    getWorkflowStatusTagType(status) {
      const typeMap = {
        'PENDING': 'info',
        'RUNNING': 'primary',
        'PAUSED': 'warning',
        'COMPLETED': 'success',
        'FAILED': 'danger',
        'CANCELLED': 'info'
      }
      return typeMap[status] || 'info'
    },
    
    /**
     * 获取步骤图标
     */
    getStepIcon(stepType) {
      const iconMap = {
        'CLIPPING': '✂️',
        'MERGING': '🔗',
        'SEGMENTATION': '📦',
        'SUBMISSION': '📤'
      }
      return iconMap[stepType] || '⚙️'
    },
    
    /**
     * 获取步骤指示器样式类
     */
    getStepIndicatorClass(step, stepIndex, currentStepIndex) {
      const baseClass = 'step-indicator'
      
      if (step.status === 'COMPLETED') {
        return `${baseClass} step-completed`
      } else if (step.status === 'RUNNING') {
        return `${baseClass} step-running`
      } else if (step.status === 'FAILED') {
        return `${baseClass} step-failed`
      } else if (stepIndex === currentStepIndex) {
        return `${baseClass} step-current`
      } else {
        return `${baseClass} step-pending`
      }
    },
    
    /**
     * 获取步骤提示内容
     */
    getStepTooltipContent(step) {
      let content = `${step.typeDescription}: ${step.statusDescription}`
      
      if (step.startTime) {
        content += `\n开始时间: ${this.formatDateTime(step.startTime)}`
      }
      
      if (step.endTime) {
        content += `\n结束时间: ${this.formatDateTime(step.endTime)}`
      }
      
      if (step.durationMs) {
        content += `\n执行时长: ${this.formatDuration(step.durationMs)}`
      }
      
      if (step.errorMessage) {
        content += `\n错误信息: ${step.errorMessage}`
      }
      
      return content
    },
    
    /**
     * 格式化时长（毫秒转为可读格式）
     */
    formatDuration(durationMs) {
      if (!durationMs) return ''
      
      const seconds = Math.floor(durationMs / 1000)
      const minutes = Math.floor(seconds / 60)
      const hours = Math.floor(minutes / 60)
      
      if (hours > 0) {
        return `${hours}小时${minutes % 60}分${seconds % 60}秒`
      } else if (minutes > 0) {
        return `${minutes}分${seconds % 60}秒`
      } else {
        return `${seconds}秒`
      }
    },
    
    /**
     * 启动工作流状态定时刷新
     */
    startWorkflowStatusRefresh() {
      // 每30秒刷新一次工作流状态
      this.workflowStatusRefreshTimer = setInterval(() => {
        this.refreshActiveWorkflowStatus()
      }, 30000)
    },
    
    /**
     * 停止工作流状态定时刷新
     */
    stopWorkflowStatusRefresh() {
      if (this.workflowStatusRefreshTimer) {
        clearInterval(this.workflowStatusRefreshTimer)
        this.workflowStatusRefreshTimer = null
      }
    },
    
    /**
     * 刷新活跃工作流的状态（只刷新正在运行或暂停的工作流）
     */
    async refreshActiveWorkflowStatus() {
      const activeWorkflowTasks = this.taskList.filter(task => 
        task.workflowStatus && 
        ['RUNNING', 'PAUSED'].includes(task.workflowStatus.status)
      )
      
      for (let task of activeWorkflowTasks) {
        try {
          const response = await getWorkflowStatus(task.taskId)
          if (response.code === 0) {
            this.$set(task, 'workflowStatus', response.data)
          }
        } catch (error) {
          console.log(`刷新任务 ${task.taskId} 工作流状态失败:`, error)
        }
      }
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

/* 工作流配置样式 */
.segmentation-config {
  background-color: #f8f9fa;
  padding: 15px;
  border-radius: 4px;
  margin: 15px 0;
  border-left: 4px solid #409eff;
}

.workflow-preview {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.preview-item {
  display: flex;
  margin-bottom: 8px;
}

.preview-item:last-child {
  margin-bottom: 0;
}

.preview-label {
  font-weight: 500;
  color: #606266;
  min-width: 80px;
}

.preview-value {
  color: #303133;
  flex: 1;
}

/* 源视频配置样式 */
.source-videos-config {
  min-height: 300px;
}

.source-videos-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.source-videos-header span {
  font-weight: bold;
  color: #303133;
}

/* 工作流配置表单样式 */
.el-radio-group .el-radio {
  margin-right: 20px;
}

.el-checkbox-group .el-checkbox {
  margin-right: 20px;
  margin-bottom: 10px;
}

.el-input-number {
  width: 150px;
}

/* 对话框标签页样式 */
.el-dialog .el-tabs__header {
  margin-bottom: 20px;
}

.el-dialog .el-tabs__content {
  padding-top: 0;
}

/* 表单验证错误样式 */
.el-form-item.is-error .el-input__inner,
.el-form-item.is-error .el-textarea__inner {
  border-color: #f56c6c;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .el-dialog {
    width: 95% !important;
  }
  
  .source-videos-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .source-videos-header span {
    margin-bottom: 10px;
  }
}

/* 工作流状态样式 */
.workflow-status-container {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.workflow-progress {
  margin-top: 5px;
}

.workflow-steps {
  display: flex;
  gap: 3px;
  margin-top: 5px;
}

.step-indicator {
  display: inline-block;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  text-align: center;
  line-height: 20px;
  font-size: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.step-pending {
  background-color: #f5f7fa;
  color: #909399;
  border: 1px solid #e4e7ed;
}

.step-current {
  background-color: #409eff;
  color: white;
  border: 1px solid #409eff;
  animation: pulse 1.5s infinite;
}

.step-running {
  background-color: #409eff;
  color: white;
  border: 1px solid #409eff;
  animation: spin 2s linear infinite;
}

.step-completed {
  background-color: #67c23a;
  color: white;
  border: 1px solid #67c23a;
}

.step-failed {
  background-color: #f56c6c;
  color: white;
  border: 1px solid #f56c6c;
}

.no-workflow {
  color: #909399;
  font-size: 12px;
}

.workflow-controls {
  margin-bottom: 8px;
}

.traditional-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

/* 动画效果 */
@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0.7);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(64, 158, 255, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0);
  }
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>