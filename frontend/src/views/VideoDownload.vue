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
                默认路径: /Users/tbw/Reaction/cut
              </div>
            </el-form-item>
          </el-form>
          <span slot="footer" class="dialog-footer">
            <el-button @click="downloadDialogVisible = false">取 消</el-button>
            <el-button type="primary" @click="startDownload">确 定</el-button>
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
import { getVideoDetail, getVideoDetailByAid, getVideoPlayUrl } from '@/api/video'

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
      completedDownloads: []
    }
  },
  mounted() {
    // 组件挂载后检查refs
    this.$nextTick(() => {
      console.log('组件挂载完成，检查refs:', this.$refs);
    });
    
    // 加载下载记录
    this.loadDownloadRecords();
  },
  methods: {
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
    
    // 获取视频流信息
    async fetchVideoStreamInfo(videoId, cid) {
      try {
        let streamResponse
        const params = {
          fnval: 4048, // 获取所有DASH格式
          fourk: 1     // 允许4K
        }
        
        if (videoId.bvid) {
          streamResponse = await getVideoPlayUrl(videoId.bvid, cid, params)
        } else if (videoId.aid) {
          streamResponse = await getVideoPlayUrlByAid(videoId.aid, cid, params)
        }
        
        if (streamResponse && streamResponse.code === 0) {
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
        } else {
          console.error('获取视频流信息失败:', streamResponse ? streamResponse.message : '无响应')
          // 如果获取流信息失败，使用默认配置
          this.setupDefaultConfigs()
        }
      } catch (error) {
        console.error('获取视频流信息异常:', error)
        // 如果获取流信息异常，使用默认配置
        this.setupDefaultConfigs()
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
      this.availableResolutions = []
      
      // 从DASH视频流中获取分辨率信息
      if (data.dash && data.dash.video && data.dash.video.length > 0) {
        const uniqueResolutions = new Set()
        data.dash.video.forEach(video => {
          if (video.id) {
            uniqueResolutions.add(video.id)
          }
        })
        
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
          
          this.availableResolutions.push({
            value: id.toString(),
            label: label
          })
        })
      }
      
      // 如果仍然没有分辨率信息，设置默认值
      if (this.availableResolutions.length === 0) {
        this.availableResolutions = [
          { value: '64', label: '720P 高清' },
          { value: '80', label: '1080P 高清' },
          { value: '112', label: '1080P+ 高码率' }
        ]
      }
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
            
            // 使用Map确保唯一性，避免重复
            // 修复：直接使用video.codecs作为键
            const key = video.codecs
            uniqueCodecs.set(key, {
              value: video.codecs,
              label: label
            })
          }
        })
        
        this.availableCodecs = Array.from(uniqueCodecs.values())
      }
      
      // 如果没有编码格式信息，设置默认值
      if (this.availableCodecs.length === 0) {
        this.availableCodecs = [
          { value: 'h264', label: 'H.264' },
          { value: 'h265', label: 'H.265' },
          { value: 'vp9', label: 'VP9' }
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
        // 构造下载参数
        const downloadParams = {
          videoUrl: `https://www.bilibili.com/video/${this.videoInfo.bvid}`,
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

.parts-actions {
  display: flex;
  gap: 10px;
}

/* 下载记录样式 */
.download-tabs {
  margin-top: 20px;
}

.record-tabs {
  min-height: 400px;
}

.download-list {
  padding: 20px;
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

.item-config {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 10px;
}

.config-item {
  display: flex;
  align-items: center;
}

.config-label {
  font-size: 12px;
  color: #909399;
  margin-right: 5px;
}

.config-value {
  font-size: 12px;
  color: #606266;
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
}
</style>