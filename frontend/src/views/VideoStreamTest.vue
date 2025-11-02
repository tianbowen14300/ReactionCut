<template>
  <div class="video-stream-test">
    <h2>视频流URL测试</h2>
    
    <div class="form-group">
      <label>视频BV号:</label>
      <input v-model="bvid" placeholder="请输入BV号，如：BV1Xx411175n" />
    </div>
    
    <div class="form-group">
      <label>视频CID:</label>
      <input v-model="cid" placeholder="请输入CID" />
      <button @click="getVideoInfo" :disabled="!bvid">获取CID</button>
    </div>
    
    <div class="form-group">
      <label>清晰度 (qn):</label>
      <select v-model="qn">
        <option value="">默认</option>
        <option value="6">240P 极速</option>
        <option value="16">360P 流畅</option>
        <option value="32">480P 清晰</option>
        <option value="64">720P 高清</option>
        <option value="74">720P60 高帧率</option>
        <option value="80">1080P 高清</option>
        <option value="100">智能修复</option>
        <option value="112">1080P+ 高码率</option>
        <option value="116">1080P60 高帧率</option>
        <option value="120">4K 超清</option>
        <option value="125">HDR 真彩色</option>
        <option value="126">杜比视界</option>
        <option value="127">8K 超高清</option>
      </select>
    </div>
    
    <div class="form-group">
      <label>格式标识 (fnval):</label>
      <select v-model="fnval">
        <option value="">默认</option>
        <option value="0">FLV 格式</option>
        <option value="1">MP4 格式</option>
        <option value="16">DASH 格式</option>
        <option value="64">HDR 视频</option>
        <option value="128">4K 分辨率</option>
        <option value="256">杜比音频</option>
        <option value="512">杜比视界</option>
        <option value="1024">8K 分辨率</option>
        <option value="2048">AV1 编码</option>
        <option value="4048">所有DASH格式</option>
      </select>
    </div>
    
    <div class="form-group">
      <label>版本标识 (fnver):</label>
      <select v-model="fnver">
        <option value="">默认</option>
        <option value="0">0</option>
      </select>
    </div>
    
    <div class="form-group">
      <label>4K支持 (fourk):</label>
      <select v-model="fourk">
        <option value="">默认</option>
        <option value="0">不支持4K</option>
        <option value="1">支持4K</option>
      </select>
    </div>
    
    <button @click="getVideoPlayUrl" :disabled="!bvid || !cid">获取视频流URL</button>
    
    <div v-if="loading" class="loading">加载中...</div>
    
    <div v-if="result" class="result">
      <h3>结果:</h3>
      <pre>{{ JSON.stringify(result, null, 2) }}</pre>
      
      <div v-if="result.data && result.data.dash" class="stream-info">
        <h4>DASH流信息:</h4>
        <div v-if="result.data.dash.video && result.data.dash.video.length > 0">
          <h5>视频流:</h5>
          <ul>
            <li v-for="(video, index) in result.data.dash.video" :key="index">
              清晰度: {{ video.id }}, 编码: {{ video.codecs }}, 分辨率: {{ video.width }}x{{ video.height }}, 帧率: {{ video.frameRate }}
              <br>
              URL: <a :href="video.baseUrl" target="_blank">{{ video.baseUrl }}</a>
            </li>
          </ul>
        </div>
        
        <div v-if="result.data.dash.audio && result.data.dash.audio.length > 0">
          <h5>音频流:</h5>
          <ul>
            <li v-for="(audio, index) in result.data.dash.audio" :key="index">
              音质: {{ audio.id }}, 编码: {{ audio.codecs }}
              <br>
              URL: <a :href="audio.baseUrl" target="_blank">{{ audio.baseUrl }}</a>
            </li>
          </ul>
        </div>
      </div>
      
      <div v-else-if="result.data && result.data.durl && result.data.durl.length > 0" class="stream-info">
        <h4>MP4/FLV流信息:</h4>
        <ul>
          <li v-for="(durl, index) in result.data.durl" :key="index">
            大小: {{ durl.size }} bytes, 长度: {{ durl.length }} ms
            <br>
            URL: <a :href="durl.url" target="_blank">{{ durl.url }}</a>
            <div v-if="durl.backup_url && durl.backup_url.length > 0">
              <h6>备用URL:</h6>
              <ul>
                <li v-for="(backupUrl, backupIndex) in durl.backup_url" :key="backupIndex">
                  <a :href="backupUrl" target="_blank">{{ backupUrl }}</a>
                </li>
              </ul>
            </div>
          </li>
        </ul>
      </div>
    </div>
    
    <div v-if="error" class="error">
      <h3>错误:</h3>
      <p>{{ error }}</p>
    </div>
  </div>
</template>

<script>
import { getVideoDetail, getVideoPlayUrl } from '@/api/video'

export default {
  name: 'VideoStreamTest',
  data() {
    return {
      bvid: '',
      cid: '',
      qn: '112',
      fnval: '4048',
      fnver: '0',
      fourk: '1',
      result: null,
      error: null,
      loading: false
    }
  },
  methods: {
    async getVideoInfo() {
      if (!this.bvid) {
        this.error = '请输入BV号'
        return
      }
      
      this.loading = true
      this.error = null
      
      try {
        const response = await getVideoDetail(this.bvid)
        if (response.code === 0 && response.data) {
          // 获取第一个分P的CID
          if (response.data.pages && response.data.pages.length > 0) {
            this.cid = response.data.pages[0].cid.toString()
          } else {
            this.cid = response.data.cid.toString()
          }
          this.result = response
        } else {
          this.error = response.message || '获取视频信息失败'
        }
      } catch (err) {
        this.error = err.message || '获取视频信息失败'
      } finally {
        this.loading = false
      }
    },
    
    async getVideoPlayUrl() {
      if (!this.bvid || !this.cid) {
        this.error = '请输入BV号和CID'
        return
      }
      
      this.loading = true
      this.result = null
      this.error = null
      
      try {
        // 构造参数对象，只包含非空值
        const params = {}
        if (this.qn) params.qn = this.qn
        if (this.fnval) params.fnval = this.fnval
        if (this.fnver) params.fnver = this.fnver
        if (this.fourk) params.fourk = this.fourk
        
        const response = await getVideoPlayUrl(this.bvid, this.cid, params)
        if (response.code === 0) {
          this.result = response
        } else {
          this.error = response.message || '获取视频流URL失败'
        }
      } catch (err) {
        this.error = err.message || '获取视频流URL失败'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.video-stream-test {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: inline-block;
  width: 150px;
  font-weight: bold;
}

.form-group input,
.form-group select {
  width: 300px;
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.form-group button {
  margin-left: 10px;
  padding: 8px 16px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.form-group button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

button {
  padding: 10px 20px;
  background-color: #28a745;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.loading {
  text-align: center;
  padding: 20px;
  font-size: 18px;
}

.result {
  margin-top: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.result h3,
.result h4,
.result h5,
.result h6 {
  margin-top: 0;
}

.result pre {
  background-color: #e9ecef;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
}

.stream-info ul {
  list-style-type: none;
  padding: 0;
}

.stream-info li {
  margin-bottom: 10px;
  padding: 10px;
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.error {
  margin-top: 20px;
  padding: 15px;
  background-color: #f8d7da;
  color: #721c24;
  border-radius: 4px;
}
</style>