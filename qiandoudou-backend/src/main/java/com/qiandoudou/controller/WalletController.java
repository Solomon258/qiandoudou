package com.qiandoudou.controller;

import com.qiandoudou.common.Result;
import com.qiandoudou.entity.Transaction;
import com.qiandoudou.entity.Wallet;
import com.qiandoudou.service.AiService;
import com.qiandoudou.service.OssService;
import com.qiandoudou.service.TransactionService;
import com.qiandoudou.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.web.multipart.MultipartFile;

/**
 * 钱包控制器
 */
@RestController
@RequestMapping("/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private AiService aiService;
    
    @Autowired
    private OssService ossService;
    
    @Autowired
    private com.qiandoudou.mapper.WalletMapper walletMapper;

    /**
     * 获取用户钱包列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getUserWallets(@RequestParam Long userId) {
        try {
            List<Map<String, Object>> wallets = walletService.getUserWallets(userId);
            return Result.success(wallets);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建钱包
     */
    @PostMapping("/create")
    public Result<Wallet> createWallet(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String name = request.get("name").toString();
            Integer type = Integer.valueOf(request.get("type").toString());
            String backgroundImage = (String) request.get("backgroundImage");
            Long aiPartnerId = request.get("aiPartnerId") != null ? 
                Long.valueOf(request.get("aiPartnerId").toString()) : null;

            Wallet wallet = walletService.createWallet(userId, name, type, backgroundImage, aiPartnerId);
            return Result.success("钱包创建成功", wallet);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 转入资金
     */
    @PostMapping("/transfer-in")
    public Result<String> transferIn(@RequestBody Map<String, Object> request) {
        try {
            Long walletId = Long.valueOf(request.get("walletId").toString());
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String description = request.get("description").toString();
            String imageUrl = (String) request.get("imageUrl");
            String note = (String) request.get("note");

            walletService.transferIn(walletId, amount, description, imageUrl, note);
            return Result.success("转入成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 转出资金
     */
    @PostMapping("/transfer-out")
    public Result<String> transferOut(@RequestBody Map<String, Object> request) {
        try {
            Long walletId = Long.valueOf(request.get("walletId").toString());
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String description = request.get("description").toString();
            String imageUrl = (String) request.get("imageUrl");
            String note = (String) request.get("note");

            walletService.transferOut(walletId, amount, description, imageUrl, note);
            return Result.success("转出成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * AI伴侣自动转账
     */
    @PostMapping("/ai-partner-transfer")
    public Result<String> aiPartnerTransfer(@RequestBody Map<String, Object> request) {
        try {
            Long walletId = Long.valueOf(request.get("walletId").toString());
            Long aiPartnerId = Long.valueOf(request.get("aiPartnerId").toString());
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String message = request.get("message").toString();
            String aiPartnerName = request.get("aiPartnerName").toString();
            String aiPartnerAvatar = request.get("aiPartnerAvatar").toString();

            walletService.aiPartnerTransfer(walletId, aiPartnerId, amount, message, aiPartnerName, aiPartnerAvatar);
            return Result.success("AI伴侣转账成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取钱包交易记录
     */
    @GetMapping("/transactions")
    public Result<List<Transaction>> getWalletTransactions(@RequestParam Long walletId) {
        try {
            List<Transaction> transactions = transactionService.getWalletTransactions(walletId);
            return Result.success(transactions);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取钱包详情
     */
    @GetMapping("/detail")
    public Result<Wallet> getWalletDetail(@RequestParam Long walletId) {
        try {
            Wallet wallet = walletService.getById(walletId);
            if (wallet == null) {
                return Result.error("钱包不存在");
            }
            return Result.success(wallet);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取公开钱包列表（用于兜圈圈）
     */
    @GetMapping("/public")
    public Result<List<Map<String, Object>>> getPublicWallets() {
        try {
            List<Map<String, Object>> publicWallets = walletService.getPublicWallets();
            return Result.success(publicWallets);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 设置钱包公开状态
     */
    @PutMapping("/set-public")
    public Result<String> setWalletPublic(@RequestBody Map<String, Object> request) {
        try {
            Long walletId = Long.valueOf(request.get("walletId").toString());
            Integer isPublic = Integer.valueOf(request.get("isPublic").toString());
            
            walletService.setWalletPublic(walletId, isPublic);
            return Result.success(isPublic == 1 ? "钱包已设为公开" : "钱包已设为私密");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 测试OSS连接状态
     */
    @GetMapping("/test-oss")
    public Result<Map<String, Object>> testOssConnection() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (ossService == null) {
                result.put("status", "error");
                result.put("message", "OSS服务未注入");
                Result<Map<String, Object>> errorResult = Result.error("OSS服务未注入");
                errorResult.setData(result);
                return errorResult;
            }
            
            // 尝试上传一个小的测试文件到用户图片目录
            byte[] testData = "test".getBytes();
            String testUrl = ossService.uploadFile(testData, "test.txt", "user_image");
            
            result.put("status", "success");
            result.put("message", "OSS连接正常");
            result.put("testUrl", testUrl);
            return Result.success("OSS连接测试成功", result);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            Result<Map<String, Object>> errorResult = Result.error("OSS连接测试失败: " + e.getMessage());
            errorResult.setData(result);
            return errorResult;
        }
    }

    /**
     * 通用用户图片上传接口
     */
    @PostMapping("/upload-user-image")
    public Result<Map<String, String>> uploadUserImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", required = false, defaultValue = "general") String type) {
        try {
            if (file.isEmpty()) {
                return Result.error("请选择图片文件");
            }

            // 检查文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("只支持图片格式");
            }

            // 检查文件大小（限制为5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                return Result.error("图片大小不能超过5MB");
            }

            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = type + "_" + System.currentTimeMillis() + extension;
            
            // 检查OSS服务是否可用
            if (ossService == null) {
                return Result.error("OSS服务未初始化");
            }
            
            // 上传到OSS用户图片目录
            String ossUrl = ossService.uploadUserImageData(file.getBytes(), filename);
            
            // 返回OSS文件URL
            Map<String, String> result = new HashMap<>();
            result.put("imageUrl", ossUrl);

            return Result.success("图片上传成功", result);
        } catch (Exception e) {
            // 记录详细错误信息
            System.err.println("用户图片上传失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 通用图片上传接口 - 上传到/res/image路径，保持原文件名
     */
    @PostMapping("/upload-image")
    public Result<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("请选择图片文件");
            }

            // 检查文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("只支持图片格式");
            }

            // 检查文件大小（限制为5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                return Result.error("图片大小不能超过5MB");
            }

            // 使用原文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                return Result.error("文件名不能为空");
            }
            
            // 检查OSS服务是否可用
            if (ossService == null) {
                return Result.error("OSS服务未初始化");
            }
            
            // 上传到OSS image目录（/res/image）
            String ossUrl = ossService.uploadFile(file.getBytes(), originalFilename, "image");
            
            // 返回OSS文件URL
            Map<String, String> result = new HashMap<>();
            result.put("imageUrl", ossUrl);

            return Result.success("图片上传成功", result);
        } catch (Exception e) {
            // 记录详细错误信息
            System.err.println("图片上传失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传钱包背景图片到OSS
     */
    @PostMapping("/upload-background")
    public Result<Map<String, String>> uploadBackgroundImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("walletId") Long walletId) {
        try {
            if (file.isEmpty()) {
                return Result.error("请选择图片文件");
            }

            // 检查文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("只支持图片格式");
            }

            // 检查文件大小（限制为5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                return Result.error("图片大小不能超过5MB");
            }

            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = "wallet_bg_" + walletId + "_" + System.currentTimeMillis() + extension;
            
            // 检查OSS服务是否可用
            if (ossService == null) {
                return Result.error("OSS服务未初始化");
            }
            
            // 上传到OSS用户图片目录
            String ossUrl = ossService.uploadUserImageData(file.getBytes(), filename);
            
            // 返回OSS文件URL
            Map<String, String> result = new HashMap<>();
            result.put("imageUrl", ossUrl);

            return Result.success("图片上传成功", result);
        } catch (Exception e) {
            // 记录详细错误信息
            System.err.println("钱包背景图片上传失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 更新钱包背景
     * 支持预设渐变背景和OSS图片URL
     */
    @PutMapping("/update-background")
    public Result<String> updateWalletBackground(@RequestBody Map<String, Object> request) {
        try {
            String walletIdStr = request.get("walletId").toString();
            String backgroundImage = request.get("backgroundImage").toString();
            
            Long walletId = Long.valueOf(walletIdStr);
            
            // 验证钱包是否存在
            Wallet wallet = walletService.getById(walletId);
            if (wallet == null) {
                return Result.error("钱包不存在，ID: " + walletId);
            }
            
            // 验证背景图片参数
            if (backgroundImage == null || backgroundImage.trim().isEmpty()) {
                return Result.error("背景图片参数不能为空");
            }
            
            // 判断背景类型并记录日志
            if (backgroundImage.startsWith("http")) {
                System.out.println("更新钱包背景为OSS图片 - 钱包ID: " + walletId + ", URL: " + backgroundImage);
            } else if (backgroundImage.startsWith("data:")) {
                System.out.println("更新钱包背景为base64图片 - 钱包ID: " + walletId + ", 数据长度: " + backgroundImage.length());
            } else {
                System.out.println("更新钱包背景为预设渐变 - 钱包ID: " + walletId + ", 背景: " + backgroundImage);
            }

            // 更新钱包背景
            wallet.setBackgroundImage(backgroundImage);
            walletService.updateById(wallet);

            return Result.success("背景更换成功");
        } catch (NumberFormatException e) {
            return Result.error("钱包ID格式错误");
        } catch (Exception e) {
            System.err.println("更新钱包背景失败: " + e.getMessage());
            return Result.error("背景更换失败: " + e.getMessage());
        }
    }

    /**
     * 更新钱包名称
     */
    @PutMapping("/update-name")
    public Result<String> updateWalletName(@RequestBody Map<String, Object> request) {
        try {
            Long walletId = Long.valueOf(request.get("walletId").toString());
            String name = request.get("name").toString();
            
            if (name == null || name.trim().isEmpty()) {
                return Result.error("钱包名称不能为空");
            }

            Wallet wallet = walletService.getById(walletId);
            if (wallet == null) {
                return Result.error("钱包不存在");
            }

            wallet.setName(name.trim());
            walletService.updateById(wallet);

            return Result.success("钱包名称修改成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户关注的钱包列表
     */
    @GetMapping("/user-followed")
    public Result<List<Map<String, Object>>> getUserFollowedWallets(@RequestParam Long userId) {
        try {
            List<Map<String, Object>> followedWallets = walletMapper.getUserFollowedWallets(userId);
            return Result.success(followedWallets);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取钱包所有者ID
     */
    @GetMapping("/owner")
    public Result<Long> getWalletOwnerId(@RequestParam Long walletId) {
        try {
            Long ownerId = walletMapper.getWalletOwnerId(walletId);
            if (ownerId == null) {
                return Result.error("钱包不存在");
            }
            return Result.success(ownerId);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据图片生成文字描述
     */
    @PostMapping("/generate-text-from-image")
    public Result<String> generateTextFromImage(@RequestBody Map<String, Object> request) {
        try {
            String imageBase64 = (String) request.get("imageBase64");
            String prompt = (String) request.get("prompt");
            
            if (imageBase64 == null || imageBase64.trim().isEmpty()) {
                return Result.error("图片数据不能为空");
            }
            
            // 如果没有提供prompt，使用默认的
            if (prompt == null || prompt.trim().isEmpty()) {
                prompt = "你是一个朋友圈文案助手，根据图片生成朋友圈的文案，少于100字，不要生成其他内容,不要思考太久";
            }
            
            String generatedText = aiService.generateTextFromImageBase64(imageBase64, prompt);
            return Result.success(generatedText);
        } catch (Exception e) {
            return Result.error("图生文生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取钱包月度统计数据
     */
    @GetMapping("/monthly-stats")
    public Result<Map<String, Object>> getWalletMonthlyStats(
            @RequestParam Long walletId,
            @RequestParam int year,
            @RequestParam int month) {
        try {
            // 验证参数
            if (month < 1 || month > 12) {
                return Result.error("月份参数无效，应在1-12之间");
            }
            if (year < 2020 || year > 2030) {
                return Result.error("年份参数无效，应在2020-2030之间");
            }

            // 验证钱包是否存在
            Wallet wallet = walletService.getById(walletId);
            if (wallet == null) {
                return Result.error("钱包不存在");
            }

            // 获取统计数据
            Map<String, Object> stats = transactionService.getWalletMonthlyStats(walletId, year, month);
            return Result.success("获取统计数据成功", stats);
        } catch (Exception e) {
            return Result.error("获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 重新计算钱包余额（基于交易记录）
     */
    @PostMapping("/recalculate-balance")
    public Result<String> recalculateWalletBalance(@RequestBody Map<String, Object> request) {
        try {
            // 处理JavaScript数字精度问题，直接从字符串解析Long
            String walletIdStr = request.get("walletId").toString();
            Long walletId = Long.valueOf(walletIdStr);
            
            System.out.println("接收到的钱包ID字符串: " + walletIdStr);
            System.out.println("解析后的钱包ID: " + walletId);
            
            walletService.recalculateWalletBalance(walletId);
            return Result.success("钱包余额重新计算成功");
        } catch (NumberFormatException e) {
            return Result.error("钱包ID格式错误: " + e.getMessage());
        } catch (Exception e) {
            return Result.error("钱包余额重新计算失败: " + e.getMessage());
        }
    }

    /**
     * 修复所有钱包余额
     */
    @PostMapping("/fix-all-balances")
    public Result<String> fixAllWalletBalances() {
        try {
            walletService.fixAllWalletBalances();
            return Result.success("所有钱包余额修复成功");
        } catch (Exception e) {
            return Result.error("钱包余额修复失败: " + e.getMessage());
        }
    }

    /**
     * 测试接口：将所有钱包设置为公开状态（仅用于测试）
     */
    @PostMapping("/test/make-all-public")
    public Result<String> makeAllWalletsPublic() {
        try {
            // 获取所有钱包
            List<Wallet> wallets = walletService.list();
            int updatedCount = 0;
            
            for (Wallet wallet : wallets) {
                if (wallet.getIsPublic() != 1) {
                    wallet.setIsPublic(1);
                    walletService.updateById(wallet);
                    updatedCount++;
                }
            }
            
            return Result.success("成功将 " + updatedCount + " 个钱包设置为公开状态，总钱包数: " + wallets.size());
        } catch (Exception e) {
            return Result.error("设置钱包公开状态失败: " + e.getMessage());
        }
    }

    /**
     * 测试接口：获取钱包统计信息
     */
    @GetMapping("/test/stats")
    public Result<Map<String, Object>> getWalletStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 统计总钱包数
            long totalWallets = walletService.count();
            stats.put("totalWallets", totalWallets);
            
            // 统计公开钱包数
            long publicWallets = walletService.count(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Wallet>()
                    .eq("is_public", 1)
                    .eq("deleted", 0)
            );
            stats.put("publicWallets", publicWallets);
            
            // 统计私密钱包数
            long privateWallets = walletService.count(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Wallet>()
                    .eq("is_public", 0)
                    .eq("deleted", 0)
            );
            stats.put("privateWallets", privateWallets);
            
            // 统计交易记录数
            long totalTransactions = transactionService.count();
            stats.put("totalTransactions", totalTransactions);
            
            return Result.success("钱包统计信息获取成功", stats);
        } catch (Exception e) {
            return Result.error("获取钱包统计信息失败: " + e.getMessage());
        }
    }
}
