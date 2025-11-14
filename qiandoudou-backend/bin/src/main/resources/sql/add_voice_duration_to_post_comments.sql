-- 为post_comments表添加voice_duration字段
-- 执行时间：2024-12-19

ALTER TABLE post_comments 
ADD COLUMN voice_duration VARCHAR(10) COMMENT '语音时长，如：12s' AFTER voice_url;

-- 创建索引以优化查询性能
CREATE INDEX idx_post_comments_voice_url ON post_comments(voice_url);

-- 查看表结构确认字段添加成功
DESC post_comments;
