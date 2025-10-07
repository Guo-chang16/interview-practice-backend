package com.guochang.interviewpracticebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guochang.interviewpracticebackend.model.dto.questionBankQuestion.QuestionBankQuestionQueryRequest;
import com.guochang.interviewpracticebackend.model.entity.QuestionBankQuestion;
import com.guochang.interviewpracticebackend.model.entity.User;
import com.guochang.interviewpracticebackend.model.vo.QuestionBankQuestionVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


/**
* @author 31179
* @description 针对表【question_bank_question(题库题目)】的数据库操作Service
* @createDate 2025-10-04 11:20:29
*/
public interface QuestionBankQuestionService extends IService<QuestionBankQuestion> {
    /**
     * 校验数据
     *
     * @param questionBankQuestion
     * @param add 对创建的数据进行校验
     */
    void validQuestionBankQuestion(QuestionBankQuestion questionBankQuestion, boolean add);

    /**
     * 获取查询条件
     *
     * @param questionBankQuestionQueryRequest
     * @return
     */
    QueryWrapper<QuestionBankQuestion> getQueryWrapper(QuestionBankQuestionQueryRequest questionBankQuestionQueryRequest);

    /**
     * 获取题库题目关联封装
     *
     * @param questionBankQuestion
     * @param request
     * @return
     */
    QuestionBankQuestionVO getQuestionBankQuestionVO(QuestionBankQuestion questionBankQuestion, HttpServletRequest request);

    /**
     * 分页获取题库题目关联封装
     *
     * @param questionBankQuestionPage
     * @param request
     * @return
     */
    Page<QuestionBankQuestionVO> getQuestionBankQuestionVOPage(Page<QuestionBankQuestion> questionBankQuestionPage, HttpServletRequest request);

    /**
     * 批量添加题目到题库
     *
     * @param questionIdList
     * @param questionBankId
     * @param loginUser
     */
    void batchAddQuestionToBank(List<Long> questionIdList, Long questionBankId, User loginUser);

   /**
     * 批量移除题目从题库
     *
     * @param questionIdList
     * @param questionBankId
     */
    void batchRemoveQuestionsFromBank(List<Long> questionIdList, Long questionBankId);


}
