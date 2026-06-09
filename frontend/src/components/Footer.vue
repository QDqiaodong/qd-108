<template>
  <footer class="footer">
    <div class="container footer-inner">
      <div class="footer-info">
        <div class="footer-logo">
          <span class="logo-icon">🍰</span>
          <span class="logo-text">居家手工美食烘焙园地</span>
        </div>
        <p class="footer-desc">专注家庭烘焙、面点制作爱好者的分享平台</p>
      </div>
      <div class="footer-links">
        <div class="link-group">
          <h4>烘焙分类</h4>
          <a href="#">面包</a>
          <a href="#">蛋糕</a>
          <a href="#">饼干</a>
          <a href="#">甜点</a>
        </div>
        <div class="link-group">
          <h4>关于我们</h4>
          <a href="#">平台介绍</a>
          <a href="#">使用协议</a>
          <a href="#">联系我们</a>
        </div>
      </div>
    </div>
    <div class="footer-bottom">
      <p>© 2024 居家手工美食烘焙园地 版权所有</p>
    </div>
  </footer>
</template>

<script setup>
</script>

<style scoped>
.footer {
  background: #fff;
  border-top: 1px solid #eee;
  margin-top: 60px;
}

.footer-inner {
  display: flex;
  justify-content: space-between;
  padding: 40px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.footer-info {
  max-width: 300px;
}

.footer-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.logo-icon {
  font-size: 28px;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #ff9a56 0%, #ff6b6b 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.footer-desc {
  color: #999;
  font-size: 14px;
  line-height: 1.6;
}

.footer-links {
  display: flex;
  gap: 60px;
}

.link-group h4 {
  font-size: 15px;
  color: #333;
  margin-bottom: 16px;
}

.link-group a {
  display: block;
  color: #666;
  font-size: 14px;
  margin-bottom: 10px;
  text-decoration: none;
  transition: color 0.3s;
}

.link-group a:hover {
  color: #ff6b6b;
}

.footer-bottom {
  text-align: center;
  padding: 20px;
  border-top: 1px solid #eee;
  color: #999;
  font-size: 13px;
}
</style>
