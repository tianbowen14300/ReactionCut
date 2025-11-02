<template>
  <div class="anchor-subscription">
    <h2>主播订阅管理</h2>
    
    <div class="anchor-grid">
      <!-- 已订阅主播方块 -->
      <div 
        class="anchor-card" 
        v-for="anchor in anchorList" 
        :key="anchor.id"
      >
        <div class="anchor-header">
          <img 
            :src="anchor.avatar || getDefaultAvatar()" 
            :alt="anchor.nickname" 
            class="anchor-avatar"
          >
          <div class="anchor-info">
            <div class="anchor-name">{{ anchor.nickname || '未知主播' }}</div>
            <div class="anchor-uid">房间号: {{ anchor.uid }}</div>
          </div>
          <el-dropdown @command="handleAnchorCommand" trigger="click">
            <span class="el-dropdown-link">
              <i class="el-icon-more"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item :command="{action: 'unsubscribe', anchor: anchor}">
                取消订阅
              </el-dropdown-item>
              <el-dropdown-item :command="{action: 'startRecord', anchor: anchor}" v-if="anchor.liveStatus !== 1">
                开始录制
              </el-dropdown-item>
              <el-dropdown-item :command="{action: 'stopRecord', anchor: anchor}" v-if="anchor.liveStatus === 1">
                停止录制
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
        
        <div class="anchor-status">
          <el-tag :type="anchor.liveStatus === 1 ? 'success' : 'info'">
            {{ anchor.liveStatus === 1 ? '直播中' : '未直播' }}
          </el-tag>
        </div>
        
        <div class="anchor-details" v-if="anchor.liveStatus === 1">
          <div class="live-title">{{ anchor.liveTitle || '直播标题' }}</div>
          <div class="live-category">{{ anchor.category || '未知分区' }}</div>
        </div>
      </div>
      
      <!-- 添加主播方块 -->
      <div class="anchor-card add-card">
        <div class="add-content">
          <i class="el-icon-plus add-icon"></i>
          <div class="add-text">添加房间</div>
          <el-input 
            v-model="newAnchorUid" 
            placeholder="请输入房间号或链接"
            size="small"
            style="width: 100%; margin: 10px 0;"
          ></el-input>
          <el-button 
            type="primary" 
            size="small" 
            @click="subscribeNewAnchor"
            :loading="addingAnchor"
          >
            确定
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { subscribeAnchors, getAllAnchors } from '@/api/anchor'

export default {
  name: 'AnchorSubscription',
  data() {
    return {
      anchorList: [],
      newAnchorUid: '',
      addingAnchor: false
    }
  },
  mounted() {
    this.loadAnchorList()
    // 定时刷新主播状态
    this.statusInterval = setInterval(this.loadAnchorList, 30000)
  },
  beforeDestroy() {
    if (this.statusInterval) {
      clearInterval(this.statusInterval)
    }
  },
  methods: {
    // 加载主播列表
    async loadAnchorList() {
      try {
        const response = await getAllAnchors()
        if (response.code === 0) {
          this.anchorList = response.data || []
        } else {
          this.$message.error(response.message)
        }
      } catch (error) {
        this.$message.error('加载主播列表失败: ' + error.message)
      }
    },
    
    // 订阅新主播
    async subscribeNewAnchor() {
      if (!this.newAnchorUid) {
        this.$message.warning('请输入房间号或链接')
        return
      }
      
      this.addingAnchor = true
      try {
        // 解析房间号（如果是链接的话）
        const uid = this.extractUidFromInput(this.newAnchorUid)
        if (!uid) {
          this.$message.error('无法解析房间号')
          return
        }
        
        const response = await subscribeAnchors({ uids: [uid] })
        if (response.code === 0) {
          this.$message.success('订阅成功')
          this.newAnchorUid = ''
          this.loadAnchorList()
        } else {
          this.$message.error(response.message)
        }
      } catch (error) {
        this.$message.error('订阅失败: ' + error.message)
      } finally {
        this.addingAnchor = false
      }
    },
    
    // 从输入中提取房间号
    extractUidFromInput(input) {
      // 如果是纯数字，直接返回
      if (/^\d+$/.test(input)) {
        return input
      }
      
      // 如果是链接，尝试提取房间号
      const match = input.match(/live\.bilibili\.com\/(\d+)/)
      if (match && match[1]) {
        return match[1]
      }
      
      return input
    },
    
    // 处理主播操作命令
    handleAnchorCommand(command) {
      const { action, anchor } = command
      switch (action) {
        case 'unsubscribe':
          this.unsubscribeAnchor(anchor)
          break
        case 'startRecord':
          this.startRecord(anchor)
          break
        case 'stopRecord':
          this.stopRecord(anchor)
          break
      }
    },
    
    // 取消订阅
    async unsubscribeAnchor(anchor) {
      this.$confirm(`确定要取消订阅主播 ${anchor.nickname || anchor.uid} 吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          // 这里应该调用取消订阅的API
          // 由于后端暂未提供该接口，我们暂时只在前端移除
          this.anchorList = this.anchorList.filter(item => item.id !== anchor.id)
          this.$message.success('取消订阅成功')
        } catch (error) {
          this.$message.error('取消订阅失败: ' + error.message)
        }
      }).catch(() => {
        // 用户取消操作
      })
    },
    
    // 开始录制
    startRecord(anchor) {
      this.$message.info(`开始录制主播 ${anchor.nickname || anchor.uid}`)
      // 这里应该调用开始录制的API
    },
    
    // 停止录制
    stopRecord(anchor) {
      this.$message.info(`停止录制主播 ${anchor.nickname || anchor.uid}`)
      // 这里应该调用停止录制的API
    },
    
    // 获取默认头像
    getDefaultAvatar() {
      return 'https://static.hdslb.com/images/member/noface.gif'
    }
  }
}
</script>

<style scoped>
.anchor-subscription {
  padding: 20px;
}

.anchor-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
}

.anchor-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 15px;
  background-color: #fff;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: box-shadow 0.3s;
}

.anchor-card:hover {
  box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.15);
}

.anchor-header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.anchor-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 10px;
}

.anchor-info {
  flex: 1;
}

.anchor-name {
  font-weight: bold;
  font-size: 14px;
}

.anchor-uid {
  font-size: 12px;
  color: #909399;
}

.el-dropdown-link {
  cursor: pointer;
  color: #409EFF;
}

.anchor-status {
  margin-bottom: 10px;
}

.anchor-details {
  font-size: 12px;
  color: #606266;
}

.live-title {
  margin-bottom: 5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.add-card {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  cursor: pointer;
}

.add-content {
  text-align: center;
  width: 100%;
}

.add-icon {
  font-size: 36px;
  color: #909399;
  margin-bottom: 10px;
}

.add-text {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}
</style>