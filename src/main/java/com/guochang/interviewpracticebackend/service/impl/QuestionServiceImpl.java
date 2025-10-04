package com.guochang.interviewpracticebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guochang.interviewpracticebackend.mapper.QuestionMapper;
import com.guochang.interviewpracticebackend.model.entity.Question;
import com.guochang.interviewpracticebackend.service.QuestionService;

import org.springframework.stereotype.Service;

/**
* @author 31179
* @description 针对表【question(题目)】的数据库操作Service实现
* @createDate 2025-10-04 11:20:13
*/
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question>
    implements QuestionService {

}




