package com.guochang.interviewpracticebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guochang.interviewpracticebackend.model.dto.questionBank.QuestionBankQueryRequest;
import com.guochang.interviewpracticebackend.model.entity.QuestionBank;
import com.guochang.interviewpracticebackend.model.vo.QuestionBankVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 31179
* @description 针对表【question_bank(题库)】的数据库操作Service
* @createDate 2025-10-04 11:20:20
*/
public interface QuestionBankService extends IService<QuestionBank> {
    /**
     * 校验数据
     *
     * @param questionBank
     * @param add 对创建的数据进行校验
     */
    void validQuestionBank(QuestionBank questionBank, boolean add);

    /**
     * 获取查询条件
     *
     * @param questionBankQueryRequest
     * @return
     */
    QueryWrapper<QuestionBank> getQueryWrapper(QuestionBankQueryRequest questionBankQueryRequest);

    /**
     * 获取题库封装
     *
     * @param questionBank
     * @param request
     * @return
     */
    QuestionBankVO getQuestionBankVO(QuestionBank questionBank, HttpServletRequest request);

    /**
     * 分页获取题库封装
     *
     * @param questionBankPage
     * @param request
     * @return
     */
    Page<QuestionBankVO> getQuestionBankVOPage(Page<QuestionBank> questionBankPage, HttpServletRequest request);
}
