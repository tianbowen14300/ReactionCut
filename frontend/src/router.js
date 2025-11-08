import Vue from 'vue'
import Router from 'vue-router'
import AnchorSubscription from './views/AnchorSubscription.vue'
import VideoDownload from './views/VideoDownload.vue'
import VideoProcessor from './views/VideoProcessor.vue'
import VideoSubmission from './views/VideoSubmission.vue'
import QRCodeLoginView from './views/QRCodeLoginView.vue'
import TestQRCode from './views/TestQRCode.vue'
import ImprovedQRCodeLoginView from './views/ImprovedQRCodeLoginView.vue'
import QRCodeTestView from './views/QRCodeTestView.vue'
import QRCodeDebugView from './views/QRCodeDebugView.vue'
import BiliToolsQRCodeLoginView from './views/BiliToolsQRCodeLoginView.vue'
import VideoStreamTest from './views/VideoStreamTest.vue'
import TestRef from './views/TestRef.vue'
import SimpleRefTest from './views/SimpleRefTest.vue'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      name: 'home',
      redirect: '/anchor'
    },
    {
      path: '/anchor',
      name: 'anchor',
      component: AnchorSubscription,
      meta: { title: '主播订阅' }
    },
    {
      path: '/download',
      name: 'download',
      component: VideoDownload,
      meta: { title: '视频下载' }
    },
    {
      path: '/process',
      name: 'process',
      component: VideoProcessor,
      meta: { title: '视频处理' }
    },
    {
      path: '/submission',
      name: 'submission',
      component: VideoSubmission,
      meta: { title: '视频投稿' }
    },
    {
      path: '/qrcode-login',
      name: 'qrcode-login',
      component: QRCodeLoginView,
      meta: { title: '二维码登录' }
    },
    {
      path: '/improved-qrcode-login',
      name: 'improved-qrcode-login',
      component: ImprovedQRCodeLoginView,
      meta: { title: '改进版二维码登录' }
    },
    {
      path: '/test-qrcode',
      name: 'test-qrcode',
      component: TestQRCode,
      meta: { title: '二维码测试' }
    },
    {
      path: '/test-qrcode-generation',
      name: 'test-qrcode-generation',
      component: QRCodeTestView,
      meta: { title: '二维码生成测试' }
    },
    {
      path: '/debug-qrcode',
      name: 'debug-qrcode',
      component: QRCodeDebugView,
      meta: { title: '二维码调试' }
    },
    {
      path: '/bili-tools-qrcode-login',
      name: 'bili-tools-qrcode-login',
      component: BiliToolsQRCodeLoginView,
      meta: { title: 'BiliTools风格二维码登录' }
    },
    {
      path: '/video-stream-test',
      name: 'video-stream-test',
      component: VideoStreamTest,
      meta: { title: '视频流URL测试' }
    },
    {
      path: '/test-ref',
      name: 'test-ref',
      component: TestRef,
      meta: { title: 'Ref测试' }
    },
    {
      path: '/simple-ref-test',
      name: 'simple-ref-test',
      component: SimpleRefTest,
      meta: { title: '简单Ref测试' }
    }
  ]
})