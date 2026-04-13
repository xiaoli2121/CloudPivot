<template>
  <view class="page">
    <view class="hero">CloudPivot UniApp</view>
    <view class="desc">Mobile workspace skeleton for approvals, dashboards, and lightweight business actions.</view>
  </view>
</template>

<script>
export default {
  name: 'IndexPage'
}
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  padding: 48rpx 32rpx;
  background: $cloudpivot-bg;
}

.hero {
  font-size: 44rpx;
  font-weight: 700;
  color: $cloudpivot-primary;
  margin-bottom: 24rpx;
}

.desc {
  font-size: 28rpx;
  line-height: 1.6;
  color: #334155;
}
</style>
