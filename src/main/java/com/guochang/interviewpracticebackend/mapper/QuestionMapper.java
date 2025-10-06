package com.guochang.interviewpracticebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guochang.interviewpracticebackend.model.entity.Question;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
* @author 31179
* @description 针对表【question(题目)】的数据库操作Mapper
* @createDate 2025-10-04 11:20:13
* @Entity com.guochang.interviewpracticebackend.model.entity.Question
*/
public interface QuestionMapper extends BaseMapper<Question> {
    /**
     * 查询题目列表（包括已被删除的数据）
     */
    @Select("select * from question where updateTime >= #{minUpdateTime}")
    List<Question> listQuestionWithDelete(Date minUpdateTime);
}




