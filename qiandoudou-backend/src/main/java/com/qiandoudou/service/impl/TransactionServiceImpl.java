package com.qiandoudou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiandoudou.entity.Transaction;
import com.qiandoudou.entity.ScriptChapter;
import com.qiandoudou.entity.Script;
import com.qiandoudou.mapper.TransactionMapper;
import com.qiandoudou.mapper.ScriptChapterMapper;
import com.qiandoudou.mapper.ScriptMapper;
import com.qiandoudou.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 交易记录服务实现类
 */
@Service
public class TransactionServiceImpl extends ServiceImpl<TransactionMapper, Transaction> implements TransactionService {

    @Autowired
    private ScriptChapterMapper scriptChapterMapper;
    
    @Autowired
    private ScriptMapper scriptMapper;

    @Override
    public List<Transaction> getWalletTransactions(Long walletId) {
        LambdaQueryWrapper<Transaction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Transaction::getWalletId, walletId)
                .orderByDesc(Transaction::getCreateTime);
        List<Transaction> transactions = list(queryWrapper);
        
        // 为剧本攒钱的交易添加剧本封面图片
        for (Transaction transaction : transactions) {
            if (transaction.getType() == 3 && transaction.getScriptChapterId() != null) {
                // 通过章节ID查找对应的剧本
                ScriptChapter chapter = scriptChapterMapper.selectById(transaction.getScriptChapterId());
                if (chapter != null) {
                    Script script = scriptMapper.selectById(chapter.getScriptId());
                    if (script != null) {
                        // 将剧本封面图片URL设置到交易记录中
                        transaction.setScriptCoverImage(script.getCoverImage());
                        transaction.setScriptTitle(script.getTitle());
                    }
                }
            }
        }
        
        return transactions;
    }

    @Override
    public List<Transaction> getUserTransactions(Long userId) {
        LambdaQueryWrapper<Transaction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Transaction::getUserId, userId)
                .orderByDesc(Transaction::getCreateTime);
        return list(queryWrapper);
    }

    @Override
    public Map<String, Object> getWalletMonthlyStats(Long walletId, int year, int month) {
        // 计算月份的开始和结束时间
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startTime = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endTime = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        // 查询指定月份的所有交易记录
        LambdaQueryWrapper<Transaction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Transaction::getWalletId, walletId)
                .between(Transaction::getCreateTime, startTime, endTime);
        List<Transaction> transactions = list(queryWrapper);

        // 计算统计数据
        BigDecimal monthlyIncome = BigDecimal.ZERO;  // 月收入
        BigDecimal monthlyExpense = BigDecimal.ZERO; // 月支出
        
        for (Transaction transaction : transactions) {
            BigDecimal amount = transaction.getAmount();
            Integer type = transaction.getType();
            
            if (type != null && amount != null) {
                if (type == 1) {
                    // 转入：增加月收入
                    monthlyIncome = monthlyIncome.add(amount);
                } else if (type == 2) {
                    // 转出：增加月支出
                    monthlyExpense = monthlyExpense.add(amount);
                } else if (type == 3) {
                    // 剧本攒钱：根据金额正负判断收入还是支出
                    if (amount.compareTo(BigDecimal.ZERO) > 0) {
                        monthlyIncome = monthlyIncome.add(amount);
                    } else {
                        monthlyExpense = monthlyExpense.add(amount.abs());
                    }
                }
            }
        }

        // 计算月收益（月收入 - 月支出）
        BigDecimal monthlyProfit = monthlyIncome.subtract(monthlyExpense);

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("month", month);
        result.put("monthlyIncome", monthlyIncome);
        result.put("monthlyExpense", monthlyExpense);
        result.put("monthlyProfit", monthlyProfit);
        result.put("transactionCount", transactions.size());

        return result;
    }
}
