// 测试静态资源服务器配置
const https = require('https');
const http = require('http');

// 测试不同的URL路径
const testUrls = [
  'http://8.148.206.18/',
  'http://8.148.206.18/bankapp/',
  'http://8.148.206.18/bankapp/res/',
  'http://8.148.206.18/bankapp/res/audio/',
  // 测试一个已知存在的文件（如果有的话）
  'http://8.148.206.18/bankapp/res/audio/test.wav'
];

async function testUrl(url) {
  return new Promise((resolve) => {
    console.log(`\n测试URL: ${url}`);
    
    const urlObj = new URL(url);
    const client = urlObj.protocol === 'https:' ? https : http;
    
    const req = client.get(url, (res) => {
      console.log('状态码:', res.statusCode);
      console.log('Content-Type:', res.headers['content-type']);
      
      let data = '';
      res.on('data', chunk => {
        data += chunk.toString();
      });
      
      res.on('end', () => {
        if (res.statusCode === 200) {
          console.log('✅ 访问成功');
          if (data.length > 0 && data.length < 1000) {
            console.log('响应内容预览:', data.substring(0, 200));
          }
        } else {
          console.log('❌ 访问失败');
          if (data.length > 0 && data.length < 500) {
            console.log('错误内容:', data);
          }
        }
        resolve();
      });
    });
    
    req.on('error', (err) => {
      console.log('❌ 请求失败:', err.message);
      resolve();
    });
    
    req.setTimeout(5000, () => {
      console.log('❌ 请求超时');
      req.destroy();
      resolve();
    });
  });
}

async function runTests() {
  console.log('开始测试静态资源服务器配置...\n');
  
  for (const url of testUrls) {
    await testUrl(url);
    await new Promise(resolve => setTimeout(resolve, 1000)); // 等待1秒
  }
  
  console.log('\n测试完成');
}

runTests();
