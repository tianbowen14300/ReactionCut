<template>
  <div class="video-download">
    <h2>视频下载</h2>
    
    <!-- 主Tabs -->
    <el-tabs v-model="activeMainTab" type="card" @tab-click="handleMainTabClick">
      <!-- 视频下载Tab -->
      <el-tab-pane label="视频下载" name="videoDownload">
        <!-- 搜索区域 -->
        <el-card class="search-card">
          <div slot="header">
            <span>搜索视频</span>
          </div>
          <el-row :gutter="20">
            <el-col :span="18">
              <el-input 
                v-model="searchInput" 
                placeholder="请输入BV号或视频链接"
                size="large"
                @keyup.enter.native="searchVideo"
              >
                <i slot="prefix" class="el-input__icon el-icon-search"></i>
              </el-input>
            </el-col>
            <el-col :span="6">
              <el-button 
                type="primary" 
                size="large" 
                @click="searchVideo"
                :loading="searching"
                style="width: 100%"
              >
                搜索
              </el-button>
            </el-col>
          </el-row>
        </el-card>
        
        <!-- 视频信息展示 -->
        <el-card class="video-info-card" v-if="videoInfo">
          <div class="video-header">
            <div class="video-cover">
              <img 
                :src="getProxyImageUrl(videoInfo.pic) || getDefaultCover()" 
                alt="视频封面" 
                class="cover-img"
                @error="handleCoverError"
              >
            </div>
            <div class="video-details">
              <div class="video-title">{{ videoInfo.title }}</div>
              <div class="video-stats">
                <span class="stat-item">播放: {{ formatNumber(videoInfo.stat.view) }}</span>
                <span class="stat-item">弹幕: {{ formatNumber(videoInfo.stat.danmaku) }}</span>
                <span class="stat-item">评论: {{ formatNumber(videoInfo.stat.reply) }}</span>
                <span class="stat-item">收藏: {{ formatNumber(videoInfo.stat.favorite) }}</span>
                <span class="stat-item">投币: {{ formatNumber(videoInfo.stat.coin) }}</span>
                <span class="stat-item">点赞: {{ formatNumber(videoInfo.stat.like) }}</span>
                <span class="stat-item">分享: {{ formatNumber(videoInfo.stat.share) }}</span>
              </div>
              <div class="video-desc">{{ videoInfo.desc }}</div>
            </div>
            <div class="uploader-info">
              <img 
                :src="getProxyImageUrl(videoInfo.owner.face) || getDefaultAvatar()" 
                alt="UP主头像" 
                class="uploader-avatar"
                @error="handleAvatarError"
              >
              <div class="uploader-name">{{ videoInfo.owner.name }}</div>
            </div>
          </div>
          
          <!-- 分P列表 -->
          <div class="video-parts">
            <div class="parts-header">
              <div class="parts-title">分P列表 (共{{ videoParts.length }}个)</div>
              <div class="parts-actions">
                <el-button 
                  size="small" 
                  @click="toggleSelectAll"
                >
                  {{ selectedParts.length === videoParts.length ? '取消全选' : '全选' }}
                </el-button>
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="showDownloadDialog"
                  :disabled="selectedParts.length === 0"
                >
                  常规下载
                </el-button>
                <el-button 
                  type="success" 
                  size="small" 
                  @click="showIntegrationDialog"
                  :disabled="selectedParts.length === 0"
                >
                  下载+投稿
                </el-button>
              </div>
            </div>
            
            <el-table 
              :data="videoParts" 
              style="width: 100%" 
              @selection-change="handleSelectionChange"
              ref="multipleTable"
            >
              <el-table-column type="selection" width="55"></el-table-column>
              <el-table-column prop="part" label="分P标题"></el-table-column>
              <el-table-column prop="duration" label="时长" width="120">
                <template slot-scope="scope">
                  {{ formatDuration(scope.row.duration) }}
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
        
        <!-- 下载配置弹窗 -->
        <el-dialog
          title="下载配置"
          :visible.sync="downloadDialogVisible"
          width="500px"
        >
          <el-form :model="downloadConfig" label-width="100px">
            <el-form-item label="下载名称">
              <el-input 
                v-model="downloadConfig.downloadName" 
                placeholder="请输入下载文件夹名称"
              ></el-input>
              <div style="font-size: 12px; color: #999; margin-top: 5px;">
                用于指定下载视频的主文件夹名称
              </div>
            </el-form-item>
            <el-form-item label="分辨率">
              <el-select v-model="downloadConfig.resolution" placeholder="请选择分辨率">
                <el-option 
                  v-for="resolution in availableResolutions" 
                  :key="resolution.value" 
                  :label="resolution.label" 
                  :value="resolution.value">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="编码格式">
              <el-select v-model="downloadConfig.codec" placeholder="请选择编码格式">
                <el-option 
                  v-for="codec in availableCodecs" 
                  :key="codec.value" 
                  :label="codec.label" 
                  :value="codec.value">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="流媒体格式">
              <el-select v-model="downloadConfig.format" placeholder="请选择格式">
                <el-option 
                  v-for="format in availableFormats" 
                  :key="format.value" 
                  :label="format.label" 
                  :value="format.value">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="下载内容">
              <el-select v-model="downloadConfig.content" placeholder="请选择下载内容">
                <el-option label="音视频" value="audio_video"></el-option>
                <el-option label="仅视频" value="video_only"></el-option>
                <el-option label="仅音频" value="audio_only"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="下载路径">
              <el-input v-model="downloadConfig.downloadPath" placeholder="请输入下载路径">
              </el-input>
              <div style="font-size: 12px; color: #999; margin-top: 5px;">
                默认路径: /Users/tbw/Reaction
              </div>
            </el-form-item>
          </el-form>
          <span slot="footer" class="dialog-footer">
            <el-button @click="downloadDialogVisible = false">取 消</el-button>
            <el-button type="primary" @click="startDownload">确 定</el-button>
          </span>
        </el-dialog>
        
        <!-- 集成投稿弹窗 -->
        <el-dialog
          title="下载+投稿配置"
          :visible.sync="integrationDialogVisible"
          width="1000px"
          :close-on-click-modal="false"
        >
          <el-tabs v-model="integrationActiveTab" type="card">
            <!-- 工作流配置标签页 -->
            <el-tab-pane label="工作流配置" name="workflow">
              <el-form :model="workflowConfig" label-width="120px">
                <el-form-item label="处理模式">
                  <el-radio-group v-model="workflowConfig.enableDirectSubmission">
                    <el-radio :label="true">直接投稿</el-radio>
                    <el-radio :label="false">分段处理后投稿</el-radio>
                  </el-radio-group>
                  <div style="font-size: 12px; color: #999; margin-top: 5px;">
                    直接投稿：下载完成后直接进行投稿<br>
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
                        下载 → 剪辑 → 合并
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
              </el-form>
            </el-tab-pane>
            
            <!-- 下载配置标签页 -->
            <el-tab-pane label="下载配置" name="download">
              <el-form :model="downloadConfig" label-width="100px">
                <el-form-item label="下载名称">
                  <el-input 
                    v-model="downloadConfig.downloadName" 
                    placeholder="请输入下载文件夹名称"
                  ></el-input>
                  <div style="font-size: 12px; color: #999; margin-top: 5px;">
                    用于指定下载视频的主文件夹名称
                  </div>
                </el-form-item>
                <el-form-item label="分辨率">
                  <el-select v-model="downloadConfig.resolution" placeholder="请选择分辨率">
                    <el-option 
                      v-for="resolution in availableResolutions" 
                      :key="resolution.value" 
                      :label="resolution.label" 
                      :value="resolution.value">
                    </el-option>
                  </el-select>
                </el-form-item>
                <el-form-item label="编码格式">
                  <el-select v-model="downloadConfig.codec" placeholder="请选择编码格式">
                    <el-option 
                      v-for="codec in availableCodecs" 
                      :key="codec.value" 
                      :label="codec.label" 
                      :value="codec.value">
                    </el-option>
                  </el-select>
                </el-form-item>
                <el-form-item label="流媒体格式">
                  <el-select v-model="downloadConfig.format" placeholder="请选择格式">
                    <el-option 
                      v-for="format in availableFormats" 
                      :key="format.value" 
                      :label="format.label" 
                      :value="format.value">
                    </el-option>
                  </el-select>
                </el-form-item>
                <el-form-item label="下载内容">
                  <el-select v-model="downloadConfig.content" placeholder="请选择下载内容">
                    <el-option label="音视频" value="audio_video"></el-option>
                    <el-option label="仅视频" value="video_only"></el-option>
                    <el-option label="仅音频" value="audio_only"></el-option>
                  </el-select>
                </el-form-item>
                <el-form-item label="下载路径">
                  <el-input v-model="downloadConfig.downloadPath" placeholder="请输入下载路径">
                  </el-input>
                  <div style="font-size: 12px; color: #999; margin-top: 5px;">
                    默认路径: /Users/tbw/Reaction
                  </div>
                </el-form-item>
              </el-form>
            </el-tab-pane>
            
            <!-- 投稿配置标签页 -->
            <el-tab-pane label="投稿配置" name="submission">
              <el-form :model="submissionConfig" label-width="100px" :rules="submissionRules" ref="submissionForm">
                <el-form-item label="视频标题" prop="title">
                  <el-input 
                    v-model="submissionConfig.title" 
                    placeholder="请输入视频标题"
                    maxlength="80"
                    show-word-limit
                  ></el-input>
                </el-form-item>
                <el-form-item label="视频描述" prop="description">
                  <el-input 
                    type="textarea" 
                    v-model="submissionConfig.description" 
                    placeholder="请输入视频描述"
                    :rows="4"
                    maxlength="2000"
                    show-word-limit
                  ></el-input>
                </el-form-item>
                <el-form-item label="视频分区" prop="partitionId">
                  <el-select v-model="submissionConfig.partitionId" placeholder="请选择视频分区">
                    <el-option 
                      v-for="partition in availablePartitions" 
                      :key="partition.value" 
                      :label="partition.label" 
                      :value="partition.value">
                    </el-option>
                  </el-select>
                </el-form-item>
                <el-form-item label="视频标签">
                  <el-input 
                    v-model="submissionConfig.tags" 
                    placeholder="请输入标签，用逗号分隔"
                    maxlength="200"
                    show-word-limit
                  ></el-input>
                  <div style="font-size: 12px; color: #999; margin-top: 5px;">
                    多个标签用逗号分隔，如：游戏,娱乐,搞笑
                  </div>
                </el-form-item>
                <el-form-item label="视频类型" prop="videoType">
                  <el-radio-group v-model="submissionConfig.videoType">
                    <el-radio label="ORIGINAL">自制原创</el-radio>
                    <el-radio label="REPOST">转载</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-form>
            </el-tab-pane>
            
            <!-- 分P配置标签页 -->
            <el-tab-pane label="分P配置" name="parts">
              <div class="parts-config">
                <div class="parts-header">
                  <span>已选择 {{ selectedParts.length }} 个分P</span>
                </div>
                <el-table :data="selectedPartsWithConfig" style="width: 100%">
                  <el-table-column prop="originalTitle" label="分P标题" width="200"></el-table-column>
                  <el-table-column label="视频文件" width="300">
                    <template slot-scope="scope">
                      <span class="file-path">{{ scope.row.filePath }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="开始时间" width="120">
                    <template slot-scope="scope">
                      <el-input 
                        v-model="scope.row.startTime" 
                        placeholder="00:00:00"
                        size="small"
                      ></el-input>
                    </template>
                  </el-table-column>
                  <el-table-column label="结束时间" width="120">
                    <template slot-scope="scope">
                      <el-input 
                        v-model="scope.row.endTime" 
                        placeholder="00:00:00"
                        size="small"
                      ></el-input>
                    </template>
                  </el-table-column>
                  <el-table-column prop="duration" label="原时长" width="100">
                    <template slot-scope="scope">
                      {{ formatDuration(scope.row.duration) }}
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-tab-pane>
          </el-tabs>
          
          <span slot="footer" class="dialog-footer">
            <el-button @click="integrationDialogVisible = false">取 消</el-button>
            <el-button type="primary" @click="startIntegrationDownload">开始下载+投稿</el-button>
          </span>
        </el-dialog>
      </el-tab-pane>
      
      <!-- 下载记录Tab -->
      <el-tab-pane label="下载记录" name="downloadRecord">
        <el-tabs v-model="activeRecordTab" type="card" @tab-click="handleRecordTabClick">
          <el-tab-pane label="待下载" name="pending">
            <div class="download-list">
              <el-card 
                v-for="item in pendingDownloads" 
                :key="item.id" 
                class="download-item"
              >
                <div class="download-item-content">
                  <div class="item-header">
                    <div class="part-title">{{ item.partTitle || '未知分P' }}</div>
                    <div class="item-actions">
                      <el-button 
                        type="danger" 
                        size="mini" 
                        icon="el-icon-delete" 
                        @click="deleteDownloadRecord(item.id)"
                      >
                        删除
                      </el-button>
                    </div>
                  </div>
                  <div class="item-config">
                    <div class="config-item">
                      <span class="config-label">分辨率:</span>
                      <span class="config-value">{{ item.resolution || '未知' }}</span>
                    </div>
                    <div class="config-item">
                      <span class="config-label">编码格式:</span>
                      <span class="config-value">{{ item.codec || '未知' }}</span>
                    </div>
                    <div class="config-item">
                      <span class="config-label">流媒体格式:</span>
                      <span class="config-value">{{ item.format || '未知' }}</span>
                    </div>
                  </div>
                  <div class="item-progress">
                    <el-progress :percentage="item.progress || 0"></el-progress>
                  </div>
                  <div class="item-time">
                    创建时间: {{ formatTime(item.createTime) }}
                  </div>
                </div>
              </el-card>
              <div v-if="pendingDownloads.length === 0" class="no-data">
                暂无待下载任务
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="下载中" name="downloading">
            <div class="download-list">
              <el-card 
                v-for="item in downloadingDownloads" 
                :key="item.id" 
                class="download-item"
              >
                <div class="download-item-content">
                  <div class="item-header">
                    <div class="part-title">{{ item.partTitle || '未知分P' }}</div>
                    <div class="item-actions">
                      <el-button 
                        type="danger" 
                        size="mini" 
                        icon="el-icon-delete" 
                        @click="deleteDownloadRecord(item.id)"
                      >
                        删除
                      </el-button>
                    </div>
                  </div>
                  <div class="item-config">
                    <div class="config-item">
                      <span class="config-label">分辨率:</span>
                      <span class="config-value">{{ item.resolution || '未知' }}</span>
                    </div>
                    <div class="config-item">
                      <span class="config-label">编码格式:</span>
                      <span class="config-value">{{ item.codec || '未知' }}</span>
                    </div>
                    <div class="config-item">
                      <span class="config-label">流媒体格式:</span>
                      <span class="config-value">{{ item.format || '未知' }}</span>
                    </div>
                  </div>
                  <div class="item-progress">
                    <el-progress :percentage="item.progress || 0"></el-progress>
                  </div>
                  <div class="item-time">
                    创建时间: {{ formatTime(item.createTime) }}
                  </div>
                </div>
              </el-card>
              <div v-if="downloadingDownloads.length === 0" class="no-data">
                暂无下载中任务
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="已下载" name="completed">
            <div class="download-list">
              <el-card 
                v-for="item in completedDownloads" 
                :key="item.id" 
                class="download-item"
              >
                <div class="download-item-content">
                  <div class="item-header">
                    <div class="part-title">{{ item.partTitle || '未知分P' }}</div>
                    <div class="item-actions">
                      <el-button 
                        type="danger" 
                        size="mini" 
                        icon="el-icon-delete" 
                        @click="deleteDownloadRecord(item.id)"
                      >
                        删除
                      </el-button>
                    </div>
                  </div>
                  <div class="item-config">
                    <div class="config-item">
                      <span class="config-label">分辨率:</span>
                      <span class="config-value">{{ item.resolution || '未知' }}</span>
                    </div>
                    <div class="config-item">
                      <span class="config-label">编码格式:</span>
                      <span class="config-value">{{ item.codec || '未知' }}</span>
                    </div>
                    <div class="config-item">
                      <span class="config-label">流媒体格式:</span>
                      <span class="config-value">{{ item.format || '未知' }}</span>
                    </div>
                  </div>
                  <div class="item-progress">
                    <el-progress 
                      :percentage="item.progress || 0" 
                      :status="item.status === 2 ? 'success' : 'exception'"
                    ></el-progress>
                  </div>
                  <div class="item-time">
                    创建时间: {{ formatTime(item.createTime) }}
                  </div>
                </div>
              </el-card>
              <div v-if="completedDownloads.length === 0" class="no-data">
                暂无已完成下载任务
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { 
  downloadVideo, 
  getDownloadStatus, 
  getPendingDownloads, 
  getDownloadingDownloads, 
  getCompletedDownloads,
  deleteDownloadRecord
} from '@/api/video'
import { getVideoDetail, getVideoDetailByAid, getVideoPlayUrl, getVideoPlayUrlByAid } from '@/api/video'

export default {
  name: 'VideoDownload',
  data() {
    return {
      searchInput: '',
      searching: false,
      videoInfo: null,
      videoParts: [],
      selectedParts: [],
      downloadDialogVisible: false,
      downloadConfig: {
        downloadName: '', // 新增下载名称配置
        resolution: '',
        codec: '',
        format: '',
        content: 'audio_video',
        downloadPath: '/Users/tbw/Reaction/cut' // 设置默认下载路径
      },
      availableResolutions: [],
      availableCodecs: [],
      availableFormats: [],
      downloadTaskId: null,
      pollingTimer: null,
      // Tabs相关数据
      activeMainTab: 'videoDownload',
      activeRecordTab: 'pending',
      pendingDownloads: [],
      downloadingDownloads: [],
      completedDownloads: [],
      // WebSocket连接
      websocket: null,
      // 集成投稿相关数据
      integrationDialogVisible: false,
      integrationActiveTab: 'download',
      submissionConfig: {
        title: '',
        description: '',
        partitionId: null,
        tags: '',
        videoType: 'ORIGINAL'
      },
      submissionRules: {
        title: [
          { required: true, message: '请输入视频标题', trigger: 'blur' },
          { max: 80, message: '标题不能超过80个字符', trigger: 'blur' }
        ],
        partitionId: [
          { required: true, message: '请选择视频分区', trigger: 'change' }
        ],
        videoType: [
          { required: true, message: '请选择视频类型', trigger: 'change' }
        ]
      },
      availablePartitions: [
        { value: 1, label: '动画' },
        { value: 3, label: '音乐' },
        { value: 4, label: '游戏' },
        { value: 5, label: '娱乐' },
        { value: 36, label: '知识' },
        { value: 188, label: '科技' },
        { value: 160, label: '生活' },
        { value: 119, label: '鬼畜' },
        { value: 155, label: '时尚' },
        { value: 165, label: '广告' },
        { value: 167, label: '国创' },
        { value: 177, label: '纪录片' },
        { value: 181, label: '影视' },
        { value: 217, label: '动物圈' }
      ],
      selectedPartsWithConfig: [],
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
      enabledProcessingSteps: ['clipping', 'merging']
    }
  },
  computed: {
    // 计算属性：格式化后的选中分P列表
    formattedSelectedParts() {
      return this.selectedPartsWithConfig.map(partConfig => ({
        ...partConfig,
        expectedFilePath: this.generateExpectedFilePath({
          part: partConfig.originalTitle,
          id: partConfig.cid
        })
      }))
    }
  },
  watch: {
    // 监听下载名称变化，同步更新分P配置中的文件路径
    'downloadConfig.downloadName': function(newVal, oldVal) {
      if (this.selectedPartsWithConfig && this.selectedPartsWithConfig.length > 0) {
        this.selectedPartsWithConfig.forEach(partConfig => {
          partConfig.filePath = this.generateExpectedFilePath({
            part: partConfig.originalTitle,
            id: partConfig.cid
          })
        })
      }
    },
    
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
    // 组件挂载后检查refs
    this.$nextTick(() => {
      console.log('组件挂载完成，检查refs:', this.$refs);
    });
    
    // 加载下载记录
    this.loadDownloadRecords();
    
    // 建立WebSocket连接
    this.connectWebSocket();
  },
  beforeDestroy() {
    // 组件销毁前关闭WebSocket连接
    this.closeWebSocket();
  },
  methods: {
    // 建立WebSocket连接
    connectWebSocket() {
      try {
        // 修改WebSocket连接URL，使用正确的端口
        // 前端运行在8100端口，但WebSocket应该连接到后端的8080端口
        const wsUrl = `ws://localhost:8080/ws/download-progress`;
        console.log('尝试建立WebSocket连接到:', wsUrl);
        this.websocket = new WebSocket(wsUrl);
        
        this.websocket.onopen = () => {
          console.log('WebSocket连接已建立');
        };
        
        this.websocket.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            console.log('收到WebSocket消息:', data);
            if (data.type === 'progress') {
              this.updateDownloadProgress(data.taskId, data.progress);
            } else if (data.type === 'status') {
              this.updateDownloadStatus(data.taskId, data.status);
            }
          } catch (e) {
            console.error('解析WebSocket消息失败:', e);
          }
        };
        
        this.websocket.onclose = () => {
          console.log('WebSocket连接已关闭');
        };
        
        this.websocket.onerror = (error) => {
          console.error('WebSocket连接错误:', error);
        };
      } catch (e) {
        console.error('建立WebSocket连接失败:', e);
      }
    },
    
    // 关闭WebSocket连接
    closeWebSocket() {
      if (this.websocket) {
        this.websocket.close();
        this.websocket = null;
      }
    },
    
    // 更新下载进度
    updateDownloadProgress(taskId, progress) {
      // 更新下载中列表的进度
      const downloadingIndex = this.downloadingDownloads.findIndex(item => item.id === taskId);
      if (downloadingIndex !== -1) {
        this.$set(this.downloadingDownloads[downloadingIndex], 'progress', progress);
        console.log('更新下载中任务进度:', taskId, progress);
      }
      
      // 更新已完成列表的进度（如果任务已完成）
      const completedIndex = this.completedDownloads.findIndex(item => item.id === taskId);
      if (completedIndex !== -1) {
        this.$set(this.completedDownloads[completedIndex], 'progress', progress);
        console.log('更新已完成任务进度:', taskId, progress);
      }
    },
    
    // 更新下载状态
    updateDownloadStatus(taskId, status) {
      // 更新下载中列表的状态
      const downloadingIndex = this.downloadingDownloads.findIndex(item => item.id === taskId);
      if (downloadingIndex !== -1) {
        this.$set(this.downloadingDownloads[downloadingIndex], 'status', status);
      }
      
      // 更新已完成列表的状态
      const completedIndex = this.completedDownloads.findIndex(item => item.id === taskId);
      if (completedIndex !== -1) {
        this.$set(this.completedDownloads[completedIndex], 'status', status);
      }
    },
    
    // 搜索视频
    async searchVideo() {
      if (!this.searchInput) {
        this.$message.warning('请输入BV号或视频链接')
        return
      }
      
      this.searching = true
      try {
        // 从输入中提取视频ID
        const videoId = this.extractVideoIdFromInput(this.searchInput)
        
        let response
        if (videoId.bvid) {
          response = await getVideoDetail(videoId.bvid)
        } else if (videoId.aid) {
          response = await getVideoDetailByAid(videoId.aid)
        } else {
          throw new Error('无法识别的视频ID格式')
        }
        
        if (response.code === 0) {
          this.videoInfo = response.data
          // 处理分P列表
          if (this.videoInfo.pages && this.videoInfo.pages.length > 0) {
            this.videoParts = this.videoInfo.pages.map((page, index) => ({
              id: page.cid,
              part: page.part || `Part ${index + 1}`,
              duration: page.duration || 0
            }))
          } else {
            // 如果没有分P信息，创建一个默认分P
            this.videoParts = [{
              id: this.videoInfo.cid || 0,
              part: this.videoInfo.title || '默认分P',
              duration: this.videoInfo.duration || 0
            }]
          }
          
          // 清空已选择的分P
          this.selectedParts = []
          this.$refs.multipleTable && this.$refs.multipleTable.clearSelection()
          
          // 获取第一个分P的视频流信息用于解析分辨率等配置
          if (this.videoParts.length > 0) {
            const firstPart = this.videoParts[0]
            await this.fetchVideoStreamInfo(videoId, firstPart.id)
          } else {
            // 如果没有分P信息，使用默认配置
            this.setupDefaultConfigs()
          }
          
          // 设置默认下载名称为视频标题
          if (this.videoInfo.title) {
            this.downloadConfig.downloadName = this.videoInfo.title
          }
        } else {
          this.$message.error(response.message)
        }
      } catch (error) {
        this.$message.error('搜索失败: ' + error.message)
      } finally {
        this.searching = false
      }
    },
    
    // 获取代理图片URL
    getProxyImageUrl(imageUrl) {
      if (!imageUrl) return null
      // 使用后端代理接口加载图片
      return `/api/video/proxy-image?imageUrl=${encodeURIComponent(imageUrl)}`
    },
    
    // 从输入中提取视频ID
    extractVideoIdFromInput(input) {
      // BV号格式
      const bvMatch = input.match(/BV([A-Za-z0-9]+)/)
      if (bvMatch && bvMatch[1]) {
        return { bvid: 'BV' + bvMatch[1] }
      }
      
      // 如果是BV号格式，直接返回
      if (/^BV[A-Za-z0-9]+$/.test(input)) {
        return { bvid: input }
      }
      
      // aid格式
      const avMatch = input.match(/av(\d+)/) || input.match(/aid=(\d+)/)
      if (avMatch && avMatch[1]) {
        return { aid: parseInt(avMatch[1]) }
      }
      
      // 如果是纯数字，当作aid处理
      if (/^\d+$/.test(input)) {
        return { aid: parseInt(input) }
      }
      
      return { bvid: null, aid: null }
    },
    
    // 解析可用的分辨率
    parseAvailableResolutions(data) {
      console.log('=== 开始解析分辨率选项 ===')
      console.log('输入数据:', data)
      
      this.availableResolutions = []
      
      // 从DASH视频流中获取分辨率信息
      if (data.dash && data.dash.video && data.dash.video.length > 0) {
        console.log('发现DASH数据，视频流数量:', data.dash.video.length)
        console.log('DASH视频流详细信息:', data.dash.video)
        
        const uniqueResolutions = new Set()
        data.dash.video.forEach((video, index) => {
          console.log(`视频流[${index}]:`, {
            id: video.id,
            codecs: video.codecs,
            width: video.width,
            height: video.height,
            bandwidth: video.bandwidth
          })
          
          if (video.id) {
            uniqueResolutions.add(video.id)
            console.log(`添加分辨率ID: ${video.id}`)
          } else {
            console.warn(`视频流[${index}]缺少id字段`)
          }
        })
        
        console.log('去重后的分辨率ID集合:', Array.from(uniqueResolutions))
        
        // 将唯一分辨率转换为选项
        Array.from(uniqueResolutions).sort((a, b) => b - a).forEach(id => {
          let label = id + 'P'
          // 根据常见的分辨率ID映射标签
          switch (id) {
            case 16:
              label = '360P 流畅'
              break
            case 32:
              label = '480P 清晰'
              break
            case 64:
              label = '720P 高清'
              break
            case 74:
              label = '720P60 高清'
              break
            case 80:
              label = '1080P 高清'
              break
            case 112:
              label = '1080P+ 高码率'
              break
            case 116:
              label = '1080P60 高帧率'
              break
            case 120:
              label = '4K 超清'
              break
            case 125:
              label = 'HDR 真彩色'
              break
            case 126:
              label = '杜比视界'
              break
            case 127:
              label = '8K 超高清'
              break
          }
          
          console.log(`分辨率映射: ID ${id} -> ${label}`)
          
          this.availableResolutions.push({
            value: id.toString(),
            label: label
          })
        })
        
        console.log('解析完成的分辨率选项:', this.availableResolutions)
      } else {
        console.warn('未找到DASH数据或视频流为空')
        console.log('数据结构检查:')
        console.log('- data.dash存在:', !!data.dash)
        if (data.dash) {
          console.log('- data.dash.video存在:', !!data.dash.video)
          console.log('- data.dash.video类型:', typeof data.dash.video)
          console.log('- data.dash.video长度:', data.dash.video ? data.dash.video.length : 'N/A')
        }
        
        // 检查是否有其他格式的数据
        if (data.durl) {
          console.log('发现DURL数据（MP4格式）:', data.durl)
        }
        if (data.accept_quality) {
          console.log('发现accept_quality数据:', data.accept_quality)
        }
        if (data.accept_description) {
          console.log('发现accept_description数据:', data.accept_description)
        }
      }
      
      // 如果仍然没有分辨率信息，设置默认值
      if (this.availableResolutions.length === 0) {
        console.warn('没有解析到分辨率信息，使用默认配置')
        this.availableResolutions = [
          { value: '64', label: '720P 高清' },
          { value: '80', label: '1080P 高清' },
          { value: '112', label: '1080P+ 高码率' }
        ]
        console.log('默认分辨率选项:', this.availableResolutions)
      }
      
      console.log('=== 分辨率解析完成 ===')
    },
    
    // 解析可用的编码格式
    parseAvailableCodecs(data) {
      this.availableCodecs = []
      
      // 从DASH视频流中获取编码格式信息
      if (data.dash && data.dash.video && data.dash.video.length > 0) {
        const uniqueCodecs = new Map()
        data.dash.video.forEach(video => {
          if (video.codecs) {
            // 根据codecs值映射到编码格式标签
            let label = video.codecs
            if (video.codecs.includes('avc1')) {
              label = 'H.264'
            } else if (video.codecs.includes('hev1') || video.codecs.includes('hvc1')) {
              label = 'H.265'
            } else if (video.codecs.includes('vp09') || video.codecs.includes('vp9')) {
              label = 'VP9'
            } else if (video.codecs.includes('av01')) {
              label = 'AV1'
            }
            
            // 修复：使用label作为键来去重，避免相同编码格式的重复显示
            // 如果已经存在相同的label，优先保留更常见的codecs值
            if (!uniqueCodecs.has(label)) {
              uniqueCodecs.set(label, {
                value: video.codecs,
                label: label
              })
            }
          }
        })
        
        this.availableCodecs = Array.from(uniqueCodecs.values())
      }
      
      // 如果没有编码格式信息，设置默认值
      if (this.availableCodecs.length === 0) {
        this.availableCodecs = [
          { value: 'avc1.640032', label: 'H.264' },
          { value: 'hev1.1.6.L150.90', label: 'H.265' },
          { value: 'vp09.00.41.08.01.01.01.01', label: 'VP9' }
        ]
      }
    },
    
    // 解析可用的流媒体格式
    parseAvailableFormats(data) {
      this.availableFormats = []
      
      // 根据返回的数据判断支持的格式
      if (data.dash) {
        this.availableFormats.push({ value: 'dash', label: 'DASH' })
      }
      
      if (data.durl) {
        this.availableFormats.push({ value: 'mp4', label: 'MP4' })
      }
      
      // 如果没有格式信息，设置默认值
      if (this.availableFormats.length === 0) {
        this.availableFormats = [
          { value: 'dash', label: 'DASH' },
          { value: 'mp4', label: 'MP4' }
        ]
      }
    },
    
    // 获取视频流信息
    async fetchVideoStreamInfo(videoId, cid) {
      try {
        let streamResponse
        const params = {
          fnval: 4048, // 获取所有DASH格式
          fourk: 1     // 允许4K
        }
        
        // 添加前端请求日志
        console.log('=== 前端请求视频流信息 ===')
        console.log('videoId:', videoId)
        console.log('cid:', cid)
        console.log('请求参数:', params)
        
        if (videoId.bvid) {
          console.log('使用BVID方式请求:', videoId.bvid)
          streamResponse = await getVideoPlayUrl(videoId.bvid, cid, params)
        } else if (videoId.aid) {
          console.log('使用AID方式请求:', videoId.aid)
          streamResponse = await getVideoPlayUrlByAid(videoId.aid, cid, params)
        }
        
        // 添加响应数据日志
        console.log('=== 前端接收到的响应数据 ===')
        console.log('完整响应:', streamResponse)
        
        if (streamResponse && streamResponse.code === 0) {
          console.log('API调用成功，开始解析数据')
          console.log('响应数据:', streamResponse.data)
          
          // 解析可用的下载配置
          this.parseAvailableResolutions(streamResponse.data)
          this.parseAvailableCodecs(streamResponse.data)
          this.parseAvailableFormats(streamResponse.data)
          
          // 设置默认下载配置（仅当还没有设置默认值时）
          if (!this.downloadConfig.resolution && this.availableResolutions.length > 0) {
            this.downloadConfig.resolution = this.availableResolutions[0].value
          }
          if (!this.downloadConfig.codec && this.availableCodecs.length > 0) {
            this.downloadConfig.codec = this.availableCodecs[0].value
          }
          if (!this.downloadConfig.format && this.availableFormats.length > 0) {
            this.downloadConfig.format = this.availableFormats[0].value
          }
          
          // 输出最终解析结果
          console.log('=== 解析结果 ===')
          console.log('可用分辨率:', this.availableResolutions)
          console.log('可用编码格式:', this.availableCodecs)
          console.log('可用流媒体格式:', this.availableFormats)
        } else {
          console.error('获取视频流信息失败:', streamResponse ? streamResponse.message : '无响应')
          console.error('错误响应:', streamResponse)
          // 如果获取流信息失败，使用默认配置
          this.setupDefaultConfigs()
        }
      } catch (error) {
        console.error('获取视频流信息异常:', error)
        // 如果获取流信息异常，使用默认配置
        this.setupDefaultConfigs()
      }
    },
    
    // 设置默认下载配置
    setupDefaultConfigs() {
      this.availableResolutions = [
        { value: '64', label: '720P 高清' },
        { value: '80', label: '1080P 高清' },
        { value: '112', label: '1080P+ 高码率' }
      ]
      
      this.availableCodecs = [
        { value: 'avc1.640032', label: 'H.264' },
        { value: 'hev1.1.6.L150.90', label: 'H.265' },
        { value: 'vp09.00.41.08.01.01.01.01', label: 'VP9' }
      ]
      
      this.availableFormats = [
        { value: 'dash', label: 'DASH' },
        { value: 'mp4', label: 'MP4' }
      ]
      
      // 设置默认值
      if (!this.downloadConfig.resolution) {
        this.downloadConfig.resolution = '64'
      }
      if (!this.downloadConfig.codec) {
        this.downloadConfig.codec = 'avc1.640032'
      }
      if (!this.downloadConfig.format) {
        this.downloadConfig.format = 'dash'
      }
    },
    
    // 处理选择变化
    handleSelectionChange(selection) {
      this.selectedParts = selection
    },
    
    // 全选/取消全选
    toggleSelectAll() {
      if (this.selectedParts.length === this.videoParts.length) {
        // 取消全选
        this.$refs.multipleTable.clearSelection()
      } else {
        // 全选
        this.videoParts.forEach(row => {
          this.$refs.multipleTable.toggleRowSelection(row, true)
        })
      }
    },
    
    // 显示下载对话框
    showDownloadDialog() {
      if (this.selectedParts.length === 0) {
        this.$message.warning('请至少选择一个分P')
        return
      }
      this.downloadDialogVisible = true
      
      // 确保DOM更新后再检查refs
      this.$nextTick(() => {
        console.log('对话框打开后检查refs:', this.$refs);
      });
    },
    
    // 开始下载
    async startDownload() {
      try {
        const downloadParams = {
          videoUrl: this.videoInfo.bvid ? 
            `https://www.bilibili.com/video/${this.videoInfo.bvid}` : 
            `https://www.bilibili.com/video/av${this.videoInfo.aid}`,
          parts: this.selectedParts.map(part => ({
            cid: part.id,
            title: part.part
          })),
          config: this.downloadConfig
        }
        
        // 调用下载API
        const response = await downloadVideo(downloadParams)
        if (response.code === 0) {
          this.$message.success('下载任务已创建')
          this.downloadTaskId = response.data
          this.downloadDialogVisible = false
          
          // 重新加载下载记录
          this.loadDownloadRecords();
        } else {
          this.$message.error(response.message)
        }
      } catch (error) {
        this.$message.error('下载失败: ' + error.message)
      }
    },
    
    // 格式化数字
    formatNumber(num) {
      if (num >= 10000) {
        return (num / 10000).toFixed(1) + '万'
      }
      return num
    },
    
    // 格式化时长
    formatDuration(seconds) {
      const hours = Math.floor(seconds / 3600)
      const minutes = Math.floor((seconds % 3600) / 60)
      const secs = seconds % 60
      
      if (hours > 0) {
        return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
      } else {
        return `${minutes}:${secs.toString().padStart(2, '0')}`
      }
    },
    
    // 获取默认封面
    getDefaultCover() {
      return 'https://i0.hdslb.com/bfs/archive/5242750857121e05146d5d1b5574913bad4410f7.jpg'
    },
    
    // 获取默认头像
    getDefaultAvatar() {
      return 'https://i0.hdslb.com/bfs/face/member/noface.jpg'
    },
    
    // 处理封面加载错误
    handleCoverError(e) {
      e.target.src = this.getDefaultCover()
    },
    
    // 处理头像加载错误
    handleAvatarError(e) {
      e.target.src = this.getDefaultAvatar()
    },
    
    // 格式化时间
    formatTime(time) {
      if (!time) return ''
      return new Date(time).toLocaleString('zh-CN')
    },
    
    // 加载下载记录
    async loadDownloadRecords() {
      try {
        // 加载待下载任务
        const pendingResponse = await getPendingDownloads()
        if (pendingResponse.code === 0) {
          this.pendingDownloads = pendingResponse.data || []
        }
        
        // 加载下载中任务
        const downloadingResponse = await getDownloadingDownloads()
        if (downloadingResponse.code === 0) {
          this.downloadingDownloads = downloadingResponse.data || []
        }
        
        // 加载已完成任务
        const completedResponse = await getCompletedDownloads()
        if (completedResponse.code === 0) {
          this.completedDownloads = completedResponse.data || []
        }
      } catch (error) {
        console.error('加载下载记录失败:', error)
      }
    },
    
    // 删除下载记录
    async deleteDownloadRecord(taskId) {
      try {
        await deleteDownloadRecord(taskId)
        this.$message.success('删除成功')
        // 重新加载下载记录
        this.loadDownloadRecords()
      } catch (error) {
        this.$message.error('删除失败: ' + error.message)
      }
    },
    
    // 处理主标签页点击
    handleMainTabClick(tab) {
      if (tab.name === 'downloadRecord') {
        // 切换到下载记录标签页时，刷新数据
        this.loadDownloadRecords()
      }
    },
    
    // 处理记录标签页点击
    handleRecordTabClick(tab) {
      // 每次切换记录标签页时都刷新对应的数据
      switch (tab.name) {
        case 'pending':
          this.loadPendingDownloads()
          break
        case 'downloading':
          this.loadDownloadingDownloads()
          break
        case 'completed':
          this.loadCompletedDownloads()
          break
      }
    },
    
    // 加载待下载任务
    async loadPendingDownloads() {
      try {
        const response = await getPendingDownloads()
        if (response.code === 0) {
          this.pendingDownloads = response.data || []
        }
      } catch (error) {
        console.error('加载待下载任务失败:', error)
      }
    },
    
    // 加载下载中任务
    async loadDownloadingDownloads() {
      try {
        const response = await getDownloadingDownloads()
        if (response.code === 0) {
          this.downloadingDownloads = response.data || []
        }
      } catch (error) {
        console.error('加载下载中任务失败:', error)
      }
    },
    
    // 加载已完成任务
    async loadCompletedDownloads() {
      try {
        const response = await getCompletedDownloads()
        if (response.code === 0) {
          this.completedDownloads = response.data || []
        }
      } catch (error) {
        console.error('加载已完成任务失败:', error)
      }
    },
    
    // 显示集成投稿对话框
    showIntegrationDialog() {
      if (this.selectedParts.length === 0) {
        this.$message.warning('请至少选择一个分P')
        return
      }
      
      // 初始化工作流配置
      this.initializeWorkflowConfig()
      
      // 初始化投稿配置
      this.initializeSubmissionConfig()
      
      // 更新分P配置
      this.updateSelectedPartsWithConfig()
      
      // 显示对话框
      this.integrationDialogVisible = true
      this.integrationActiveTab = 'workflow'
    },
    
    // 初始化工作流配置
    initializeWorkflowConfig() {
      // 使用下载+投稿的默认配置
      this.workflowConfig = {
        enableDirectSubmission: true,
        segmentationConfig: {
          enabled: false,
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
    
    // 初始化投稿配置
    initializeSubmissionConfig() {
      // 不自动回显视频信息，需要用户手动填写
      this.submissionConfig.title = ''
      this.submissionConfig.description = ''
      // 删除封面URL字段
      
      // 重置其他字段为默认值
      if (!this.submissionConfig.partitionId) {
        this.submissionConfig.partitionId = null
      }
      if (!this.submissionConfig.tags) {
        this.submissionConfig.tags = ''
      }
      if (!this.submissionConfig.videoType) {
        this.submissionConfig.videoType = 'ORIGINAL'
      }
    },
    
    // 更新选中分P的配置
    updateSelectedPartsWithConfig() {
      this.selectedPartsWithConfig = this.selectedParts.map(part => ({
        originalTitle: part.part,
        filePath: this.generateExpectedFilePath(part),
        startTime: '00:00:00', // 默认开始时间
        endTime: this.formatDuration(part.duration), // 默认结束时间为视频时长
        duration: part.duration,
        cid: part.id
      }))
    },
    
    // 生成预期文件路径
    generateExpectedFilePath(part) {
      const basePath = this.downloadConfig.downloadPath || '/Users/tbw/Reaction'
      const folderName = this.downloadConfig.downloadName || (this.videoInfo && this.videoInfo.title) || 'Unknown'
      const fileName = `${part.part}.mp4`
      return `${basePath}/${folderName}/${fileName}`
    },
    
    // 开始集成下载+投稿
    async startIntegrationDownload() {
      try {
        // 验证表单
        const validationResult = await this.validateIntegrationForm()
        if (!validationResult.valid) {
          this.$message.error(validationResult.message)
          return
        }
        
        // 构建集成请求数据
        const integrationRequest = this.buildIntegrationRequest()
        
        console.log('发送集成请求:', integrationRequest)
        
        // 调用集成API
        const response = await this.callIntegrationAPI(integrationRequest)
        
        if (response.code === 0) {
          this.$message.success('集成任务创建成功')
          this.integrationDialogVisible = false
          
          // 重新加载下载记录
          this.loadDownloadRecords()
          
          // 显示任务信息
          this.showTaskCreationResult(response.data)
        } else {
          this.$message.error(response.message || '集成任务创建失败')
        }
        
      } catch (error) {
        console.error('集成下载失败:', error)
        this.$message.error('集成下载失败: ' + error.message)
      }
    },
    
    // 验证集成表单
    async validateIntegrationForm() {
      // 验证工作流配置
      const workflowValidation = this.validateWorkflowConfig()
      if (!workflowValidation.valid) {
        return workflowValidation
      }
      
      // 验证下载配置
      if (!this.downloadConfig.resolution) {
        return { valid: false, message: '请选择分辨率' }
      }
      if (!this.downloadConfig.codec) {
        return { valid: false, message: '请选择编码格式' }
      }
      if (!this.downloadConfig.format) {
        return { valid: false, message: '请选择流媒体格式' }
      }
      
      // 验证投稿配置
      if (!this.submissionConfig.title || this.submissionConfig.title.trim() === '') {
        return { valid: false, message: '请输入视频标题' }
      }
      if (this.submissionConfig.title.length > 80) {
        return { valid: false, message: '视频标题不能超过80个字符' }
      }
      if (!this.submissionConfig.partitionId) {
        return { valid: false, message: '请选择视频分区' }
      }
      if (!this.submissionConfig.videoType) {
        return { valid: false, message: '请选择视频类型' }
      }
      if (this.submissionConfig.description && this.submissionConfig.description.length > 2000) {
        return { valid: false, message: '视频描述不能超过2000个字符' }
      }
      
      // 验证分P配置
      for (let i = 0; i < this.selectedPartsWithConfig.length; i++) {
        const partConfig = this.selectedPartsWithConfig[i]
        
        // 验证开始时间格式
        if (partConfig.startTime && !this.isValidTimeFormat(partConfig.startTime)) {
          return { valid: false, message: `第${i + 1}个分P的开始时间格式不正确，请使用 HH:MM:SS 格式` }
        }
        
        // 验证结束时间格式
        if (partConfig.endTime && !this.isValidTimeFormat(partConfig.endTime)) {
          return { valid: false, message: `第${i + 1}个分P的结束时间格式不正确，请使用 HH:MM:SS 格式` }
        }
        
        // 验证时间逻辑
        if (partConfig.startTime && partConfig.endTime) {
          const startSeconds = this.timeToSeconds(partConfig.startTime)
          const endSeconds = this.timeToSeconds(partConfig.endTime)
          if (startSeconds >= endSeconds) {
            return { valid: false, message: `第${i + 1}个分P的开始时间必须小于结束时间` }
          }
        }
      }
      
      return { valid: true }
    },
    
    // 验证时间格式 (HH:MM:SS)
    isValidTimeFormat(timeStr) {
      const timeRegex = /^([0-1]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$/
      return timeRegex.test(timeStr)
    },
    
    // 将时间字符串转换为秒数
    timeToSeconds(timeStr) {
      const parts = timeStr.split(':')
      return parseInt(parts[0]) * 3600 + parseInt(parts[1]) * 60 + parseInt(parts[2])
    },
    
    // 构建集成请求数据
    buildIntegrationRequest() {
      return {
        enableSubmission: true,
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
        },
        downloadRequest: {
          videoUrl: this.videoInfo.bvid ? 
            `https://www.bilibili.com/video/${this.videoInfo.bvid}` : 
            `https://www.bilibili.com/video/av${this.videoInfo.aid}`,
          parts: this.selectedParts.map(part => ({
            cid: part.id,
            title: part.part
          })),
          config: {
            ...this.downloadConfig,
            downloadName: this.downloadConfig.downloadName || (this.videoInfo && this.videoInfo.title)
          }
        },
        submissionRequest: {
          title: this.submissionConfig.title,
          description: this.submissionConfig.description,
          partitionId: this.submissionConfig.partitionId,
          tags: this.submissionConfig.tags,
          videoType: this.submissionConfig.videoType,
          videoParts: this.selectedPartsWithConfig.map(partConfig => ({
            originalTitle: partConfig.originalTitle,
            cid: partConfig.cid,
            filePath: partConfig.filePath,
            startTime: partConfig.startTime,
            endTime: partConfig.endTime
          }))
        }
      }
    },
    
    // 调用集成API
    async callIntegrationAPI(requestData) {
      // 使用现有的downloadVideo API，但传入集成数据
      return await downloadVideo(requestData)
    },
    
    // 显示任务创建结果
    showTaskCreationResult(result) {
      let message = '任务创建成功！\n'
      
      if (result.downloadTaskId) {
        message += `下载任务ID: ${result.downloadTaskId}\n`
      }
      
      if (result.submissionTaskId) {
        message += `投稿任务ID: ${result.submissionTaskId}\n`
      }
      
      if (result.relationId) {
        message += `关联ID: ${result.relationId}\n`
      }
      
      if (result.workflowInstanceId) {
        message += `工作流实例ID: ${result.workflowInstanceId}\n`
        message += `工作流状态: 已启动\n`
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
      
      this.$alert(message, '任务创建成功', {
        confirmButtonText: '确定',
        type: 'success'
      })
    }
  }
}
</script>

<style scoped>
.video-download {
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
}

.video-info-card {
  margin-bottom: 20px;
}

.video-header {
  display: flex;
  margin-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 20px;
}

.video-cover {
  width: 160px;
  height: 100px;
  margin-right: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  border-radius: 4px;
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
  display: block;
}

.video-details {
  flex: 1;
}

.video-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
}

.video-stats {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.stat-item {
  margin-right: 15px;
  font-size: 12px;
  color: #909399;
}

.video-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
  max-height: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.uploader-info {
  text-align: center;
  width: 80px;
}

.uploader-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-bottom: 5px;
}

.uploader-name {
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.parts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.parts-title {
  font-size: 16px;
  font-weight: bold;
}

.parts-actions {
  display: flex;
  gap: 10px;
}

.download-list {
  min-height: 300px;
}

.download-item {
  margin-bottom: 15px;
}

.download-item-content {
  padding: 15px;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.part-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.item-actions {
  display: flex;
  gap: 10px;
}

.item-config {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 10px;
  font-size: 14px;
}

.config-item {
  display: flex;
  align-items: center;
}

.config-label {
  color: #909399;
  margin-right: 5px;
}

.config-value {
  color: #606266;
  font-weight: 500;
}

.item-progress {
  margin: 15px 0;
}

.item-time {
  font-size: 12px;
  color: #909399;
}

.no-data {
  text-align: center;
  color: #909399;
  padding: 50px 0;
  font-size: 14px;
}

/* 集成投稿对话框样式 */
.parts-config {
  min-height: 300px;
}

.parts-config .parts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.parts-config .parts-header span {
  font-weight: bold;
  color: #303133;
}

.file-path {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
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

/* 投稿配置表单样式 */
.el-form-item__label {
  font-weight: 500;
}

.el-textarea__inner {
  resize: vertical;
}

/* 分P配置表格样式 */
.parts-config .el-table {
  border: 1px solid #ebeef5;
}

.parts-config .el-table th {
  background-color: #fafafa;
}

.parts-config .el-input--small .el-input__inner {
  height: 28px;
  line-height: 28px;
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
  .video-header {
    flex-direction: column;
  }
  
  .video-cover {
    width: 100%;
    height: 200px;
    margin-right: 0;
    margin-bottom: 15px;
  }
  
  .uploader-info {
    width: 100%;
    text-align: left;
    display: flex;
    align-items: center;
    margin-top: 15px;
  }
  
  .uploader-avatar {
    margin-right: 10px;
    margin-bottom: 0;
  }
  
  .parts-actions {
    flex-direction: column;
    gap: 5px;
  }
  
  .parts-actions .el-button {
    width: 100%;
  }
}
</style>