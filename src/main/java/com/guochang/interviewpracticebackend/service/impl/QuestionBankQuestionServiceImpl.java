package com.guochang.interviewpracticebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guochang.interviewpracticebackend.common.ErrorCode;
import com.guochang.interviewpracticebackend.constant.CommonConstant;
import com.guochang.interviewpracticebackend.exception.BusinessException;
import com.guochang.interviewpracticebackend.exception.ThrowUtils;
import com.guochang.interviewpracticebackend.mapper.QuestionBankQuestionMapper;
import com.guochang.interviewpracticebackend.model.dto.questionBankQuestion.QuestionBankQuestionQueryRequest;
import com.guochang.interviewpracticebackend.model.entity.Question;
import com.guochang.interviewpracticebackend.model.entity.QuestionBank;
import com.guochang.interviewpracticebackend.model.entity.QuestionBankQuestion;
import com.guochang.interviewpracticebackend.model.entity.User;
import com.guochang.interviewpracticebackend.model.vo.QuestionBankQuestionVO;
import com.guochang.interviewpracticebackend.model.vo.UserVO;
import com.guochang.interviewpracticebackend.service.QuestionBankQuestionService;

import com.guochang.interviewpracticebackend.service.QuestionBankService;
import com.guochang.interviewpracticebackend.service.QuestionService;
import com.guochang.interviewpracticebackend.service.UserService;
import com.guochang.interviewpracticebackend.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author 31179
 * @description 针对表【question_bank_question(题库题目)】的数据库操作Service实现
 * @createDate 2025-10-04 11:20:29
 */
@Service
@Slf4j
public class QuestionBankQuestionServiceImpl extends ServiceImpl<QuestionBankQuestionMapper, QuestionBankQuestion>
        implements QuestionBankQuestionService {


    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private QuestionService questionService;

    @Resource
    private QuestionBankService questionBankService;

    @Resource
    private QuestionBankQuestionService questionBankQuestionService;

    /**
     * 校验数据
     *
     * @param questionBankQuestion
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestionBankQuestion(QuestionBankQuestion questionBankQuestion, boolean add) {
        ThrowUtils.throwIf(questionBankQuestion == null, ErrorCode.PARAMS_ERROR);
        // 题目和题库必须存在
        Long questionId = questionBankQuestion.getQuestionId();
        if (questionId != null) {
            Question question = questionService.getById(questionId);
            ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        Long questionBankId = questionBankQuestion.getQuestionBankId();
        if (questionBankId != null) {
            QuestionBank questionBank = questionBankService.getById(questionBankId);
            ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR, "题库不存在");
        }
    }

    /**
     * 获取查询条件
     *
     * @param questionBankQuestionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionBankQuestion> getQueryWrapper(QuestionBankQuestionQueryRequest questionBankQuestionQueryRequest) {
        QueryWrapper<QuestionBankQuestion> queryWrapper = new QueryWrapper<>();
        if (questionBankQuestionQueryRequest == null) {
            return queryWrapper;
        }
        Long id = questionBankQuestionQueryRequest.getId();
        Long notId = questionBankQuestionQueryRequest.getNotId();
        String sortField = questionBankQuestionQueryRequest.getSortField();
        String sortOrder = questionBankQuestionQueryRequest.getSortOrder();
        Long questionBankId = questionBankQuestionQueryRequest.getQuestionBankId();
        Long questionId = questionBankQuestionQueryRequest.getQuestionId();
        Long userId = questionBankQuestionQueryRequest.getUserId();
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionBankId), "questionBankId", questionBankId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }


    /**
     * 获取题库题目关联封装
     *
     * @param questionBankQuestion
     * @param request
     * @return
     */
    @Override
    public QuestionBankQuestionVO getQuestionBankQuestionVO(QuestionBankQuestion questionBankQuestion, HttpServletRequest request) {
        // 对象转封装类
        QuestionBankQuestionVO questionBankQuestionVO = QuestionBankQuestionVO.objToVo(questionBankQuestion);

        // 关联查询用户信息
        Long userId = questionBankQuestion.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionBankQuestionVO.setUser(userVO);

        return questionBankQuestionVO;
    }

    /**
     * 分页获取题库题目关联封装
     *
     * @param questionBankQuestionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionBankQuestionVO> getQuestionBankQuestionVOPage(Page<QuestionBankQuestion> questionBankQuestionPage, HttpServletRequest request) {
        List<QuestionBankQuestion> questionBankQuestionList = questionBankQuestionPage.getRecords();
        Page<QuestionBankQuestionVO> questionBankQuestionVOPage = new Page<>(questionBankQuestionPage.getCurrent(), questionBankQuestionPage.getSize(), questionBankQuestionPage.getTotal());
        if (CollUtil.isEmpty(questionBankQuestionList)) {
            return questionBankQuestionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionBankQuestionVO> questionBankQuestionVOList = questionBankQuestionList.stream().map(questionBankQuestion -> {
            return QuestionBankQuestionVO.objToVo(questionBankQuestion);
        }).collect(Collectors.toList());

        // region 可选
        // 关联查询用户信息
        /*
        Set<Long> userIdSet = questionBankQuestionList.stream().map(QuestionBankQuestion::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 填充信息
        questionBankQuestionVOList.forEach(questionBankQuestionVO -> {
            Long userId = questionBankQuestionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionBankQuestionVO.setUser(userService.getUserVO(user));
        });
        */
        // endregion

        questionBankQuestionVOPage.setRecords(questionBankQuestionVOList);
        return questionBankQuestionVOPage;
    }

    @Override
    public void batchAddQuestionToBank(List<Long> questionIdList, Long questionBankId, User loginUser) {
        //参数校验
        ThrowUtils.throwIf(questionIdList == null || questionIdList.isEmpty(), ErrorCode.PARAMS_ERROR, "传入的id列表不能为空");
        ThrowUtils.throwIf(questionBankId <= 0, ErrorCode.PARAMS_ERROR, "题库id不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR, "；当前用户未登录,无法进行该操作");

        // 判断题库是否存在
        QuestionBank questionBank = questionBankService.getById(questionBankId);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR, "题库不存在");

        //判断题目是否存在
        // 检查题目 id 是否存在
        LambdaQueryWrapper<Question> questionLambdaQueryWrapper = Wrappers.lambdaQuery(Question.class)
                .select(Question::getId)
                .in(Question::getId, questionIdList);
        List<Question> questionList = questionService.list(questionLambdaQueryWrapper);
        List<Long> validQuestionIdList = questionService.listObjs(questionLambdaQueryWrapper, obj -> (Long) obj);
        ThrowUtils.throwIf(CollUtil.isEmpty(validQuestionIdList), ErrorCode.PARAMS_ERROR, "合法的题目列表为空");

        // 检查哪些题目还不存在于题库中，避免重复插入
        LambdaQueryWrapper<QuestionBankQuestion> lambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                .eq(QuestionBankQuestion::getQuestionBankId, questionBankId)
                .in(QuestionBankQuestion::getQuestionId, validQuestionIdList);
        List<QuestionBankQuestion> existQuestionList = this.list(lambdaQueryWrapper);

        // 已存在于题库中的题目 id
        Set<Long> existQuestionIdSet = existQuestionList.stream()
                .map(QuestionBankQuestion::getQuestionId)
                .collect(Collectors.toSet());

        // 已存在于题库中的题目 id，不需要再次添加
        validQuestionIdList = validQuestionIdList.stream().filter(questionId -> {
            return !existQuestionIdSet.contains(questionId);
        }).collect(Collectors.toList());
        ThrowUtils.throwIf(CollUtil.isEmpty(validQuestionIdList), ErrorCode.PARAMS_ERROR, "所有题目都已存在于题库中");

        // 创建线程池
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(20, 50, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000), new ThreadPoolExecutor.CallerRunsPolicy());

        //保存所有批次任务
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        //分批处理,避免长事务
        int batchSize = 1000;
        int totalQuestionListSize = validQuestionIdList.size();
        for (int i = 0; i < totalQuestionListSize; i++) {
            List<Long> subList = validQuestionIdList.subList(i, Math.min(i + batchSize, totalQuestionListSize));
            List<QuestionBankQuestion> questionBankQuestionList = subList.stream().map(questionId -> {
                QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
                questionBankQuestion.setQuestionBankId(questionBankId);
                questionBankQuestion.setQuestionId(questionId);
                questionBankQuestion.setUserId(loginUser.getId());
                return questionBankQuestion;
            }).collect(Collectors.toList());

            // 使用事务处理每批数据
            QuestionBankQuestionService questionBankQuestionService = (QuestionBankQuestionServiceImpl) AopContext.currentProxy();

            // 异步执行保存操作
            CompletableFuture<Void> future = CompletableFuture
                    .runAsync(() -> questionBankQuestionService.batchAddQuestionsToBankInner(questionBankQuestionList),
                            threadPoolExecutor).exceptionally(ex -> {
                                log.error("批处理任务执行失败", ex);
                                return null;});
            futures.add(future);

            // 等待所有批次操作完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            //关闭线程池
            threadPoolExecutor.shutdown();

        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddQuestionsToBankInner(List<QuestionBankQuestion> questionBankQuestions) {
        try {
            boolean result = this.saveBatch(questionBankQuestions);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "向题库添加题目失败");
        } catch (DataIntegrityViolationException e) {
            log.error("数据库唯一键冲突或违反其他完整性约束, 错误信息: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目已存在于该题库，无法重复添加");
        } catch (DataAccessException e) {
            log.error("数据库连接问题、事务问题等导致操作失败, 错误信息: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "数据库操作失败");
        } catch (Exception e) {
            // 捕获其他异常，做通用处理
            log.error("添加题目到题库时发生未知错误，错误信息: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "向题库添加题目失败");
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRemoveQuestionsFromBank(List<Long> questionIdList, Long questionBankId) {
        //参数校验
        ThrowUtils.throwIf(questionIdList == null || questionIdList.isEmpty(), ErrorCode.PARAMS_ERROR, "传入的id列表不能为空");
        ThrowUtils.throwIf(questionBankId <= 0, ErrorCode.PARAMS_ERROR, "题库id不能为空");

        // 判断题库是否存在
        QuestionBank questionBank = questionBankService.getById(questionBankId);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR, "题库不存在");

        //判断题目是否存在
        List<Question> questionList = questionService.listByIds(questionIdList);
        List<Long> validQuestionIdList = questionList.stream()
                .map(Question::getId)
                .collect(Collectors.toList());
        ThrowUtils.throwIf(validQuestionIdList.size() != questionIdList.size(), ErrorCode.NOT_FOUND_ERROR, "部分题目不存在");

        //批量移除题目
        for (Long questionId : validQuestionIdList) {
            LambdaQueryWrapper<QuestionBankQuestion> lambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                    .eq(QuestionBankQuestion::getQuestionId, questionId)    // 不同题目ID
                    .eq(QuestionBankQuestion::getQuestionBankId, questionBankId);   // 固定题库ID(当前题库)
            boolean removed = questionBankQuestionService.remove(lambdaQueryWrapper);
            ThrowUtils.throwIf(!removed, ErrorCode.OPERATION_ERROR, "批量移除题目失败");
        }
    }


}




