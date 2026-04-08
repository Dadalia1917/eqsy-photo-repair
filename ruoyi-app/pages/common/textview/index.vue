<template>
  <view>
    <uni-card class="view-title" :title="title">
      <text class="uni-body view-content">{{ content }}</text>
    </uni-card>
  </view>
</template>

<script>
  const PROTOCOL_MAP = {
    privacy: {
      title: '隐私政策',
      content: `更新时间：2026年4月8日\n生效时间：2026年4月8日\n\n欢迎使用 e起守忆 社区志愿服务平台。我们非常重视您的个人信息和隐私保护。\n\n一、我们收集的信息\n1. 账号信息：您主动提交的用户名、密码。\n2. 登录信息：您使用微信一键登录时，我们仅接收用于身份校验的授权凭证，不强制收集手机号。\n3. 业务信息：您上传的照片、修复备注、任务状态与处理结果。\n4. 设备与日志信息：为保障服务稳定性，我们会记录必要的网络请求日志、异常日志。\n\n二、我们如何使用信息\n1. 用于身份识别、登录验证和账号安全。\n2. 用于照片修复任务的创建、分发、处理与结果回传。\n3. 用于故障排查、服务优化和安全风控。\n\n三、信息共享与披露\n1. 我们不会出售您的个人信息。\n2. 仅在实现修复服务所必需范围内，由本平台授权人员进行处理。\n3. 法律法规要求或有权机关依法要求时，我们会在合法范围内提供。\n\n四、信息存储与保护\n1. 我们采取访问控制、传输加密、权限隔离等安全措施。\n2. 仅在实现服务目的所必需期限内保存您的信息。\n\n五、您的权利\n您可在法律允许范围内访问、更正或删除相关信息；也可联系平台申请注销账号。\n\n六、未成年人保护\n若您为未成年人，请在监护人指导下使用本服务。\n\n七、政策更新\n本政策更新后会在应用内公示。继续使用服务即视为您已阅读并同意更新后的政策。\n\n如您对本政策有疑问，请联系社区工作人员或平台管理方。`
    },
    service: {
      title: '用户服务协议',
      content: `更新时间：2026年4月8日\n生效时间：2026年4月8日\n\n欢迎使用 e起守忆 社区志愿服务平台。请您在使用前仔细阅读本协议。\n\n一、服务内容\n1. 本平台提供照片修复任务提交、志愿者处理、结果查看与下载等功能。\n2. 平台有权在不影响核心服务的前提下优化或调整功能。\n\n二、账号与安全\n1. 您应妥善保管账号信息，不得出借、转让或共享账号。\n2. 因账号保管不当造成的风险由用户自行承担。\n\n三、用户行为规范\n1. 您上传的内容应合法、真实，不得侵犯他人合法权益。\n2. 禁止上传违法、暴力、色情、侮辱诽谤或其他违规内容。\n3. 禁止利用本平台从事任何违法活动。\n\n四、知识产权\n1. 平台界面、程序与相关资料的知识产权归平台或权利人所有。\n2. 您保证对上传内容享有合法使用权。\n\n五、免责声明\n1. 因不可抗力、网络故障、第三方服务异常导致的服务中断，平台将尽力恢复，但不承担超出法律规定的责任。\n2. 志愿者修复结果受原图质量等因素影响，平台不承诺达到特定商业标准。\n\n六、协议变更与终止\n1. 平台可根据业务发展和法律法规要求更新本协议，并在应用内公示。\n2. 若您不同意更新内容，可停止使用服务；继续使用即视为接受更新后的协议。\n\n七、争议解决\n本协议的订立、执行与解释适用中华人民共和国法律。发生争议时，双方应先行协商解决。\n\n点击“同意并登录”或继续使用本服务，表示您已阅读、理解并同意本协议全部内容。`
    }
  }

  export default {
    data() {
      return {
        title: '',
        content: ''
      }
    },
    onLoad(options) {
      const protocol = PROTOCOL_MAP[options.type]
      if (protocol) {
        this.title = protocol.title
        this.content = protocol.content
      } else {
        this.title = decodeURIComponent(options.title || '')
        this.content = decodeURIComponent(options.content || '')
      }
      uni.setNavigationBarTitle({
        title: this.title || '协议详情'
      })
    }
  }
</script>

<style scoped>
  page {
    background-color: #ffffff;
  }

  .view-title {
    font-weight: bold;
  }

  .view-content {
    font-size: 26rpx;
    padding: 12px 5px 0;
    color: #333;
    line-height: 24px;
    font-weight: normal;
    white-space: pre-line;
  }
</style>
