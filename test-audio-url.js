// 测试音频URL是否可访问
const https = require('https');
const http = require('http');

// 从日志中获取的音频URL
const audioUrl = 'http://8.148.206.18/bankapp/res/audio/aba2e466a902417ab12faeb332ebe130.wav';

console.log('测试音频URL:', audioUrl);

// 使用http模块测试URL
const url = new URL(audioUrl);
const client = url.protocol === 'https:' ? https : http;

client.get(audioUrl, (res) => {
  console.log('HTTP状态码:', res.statusCode);
  console.log('响应头:', res.headers);
  
  if (res.statusCode === 200) {
    console.log('✅ 音频文件可以访问');
    console.log('Content-Type:', res.headers['content-type']);
    console.log('Content-Length:', res.headers['content-length']);
    
    // 读取一些数据来验证文件内容
    let dataLength = 0;
    res.on('data', (chunk) => {
      dataLength += chunk.length;
    });
    
    res.on('end', () => {
      console.log('实际接收数据长度:', dataLength);
      if (dataLength > 0) {
        console.log('✅ 音频文件有内容，可以正常访问');
      } else {
        console.log('❌ 音频文件为空');
      }
    });
  } else {
    console.log('❌ 音频文件访问失败，状态码:', res.statusCode);
  }
}).on('error', (err) => {
  console.log('❌ 网络请求失败:', err.message);
});
