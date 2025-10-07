package com.guochang.interviewpracticebackend.service.impl;


// 需要添加的导入语句
import org.springframework.data.domain.PageRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guochang.interviewpracticebackend.common.ErrorCode;
import com.guochang.interviewpracticebackend.constant.CommonConstant;
import com.guochang.interviewpracticebackend.exception.ThrowUtils;
import com.guochang.interviewpracticebackend.mapper.QuestionMapper;
import com.guochang.interviewpracticebackend.model.dto.question.QuestionEsDTO;
import com.guochang.interviewpracticebackend.model.dto.question.QuestionQueryRequest;
import com.guochang.interviewpracticebackend.model.entity.Question;
import com.guochang.interviewpracticebackend.model.entity.QuestionBankQuestion;
import com.guochang.interviewpracticebackend.model.entity.User;
import org.elasticsearch.search.sort.SortOrder;
import com.guochang.interviewpracticebackend.model.vo.QuestionVO;
import com.guochang.interviewpracticebackend.model.vo.UserVO;
import com.guochang.interviewpracticebackend.service.QuestionBankQuestionService;
import com.guochang.interviewpracticebackend.service.QuestionService;
import com.guochang.interviewpracticebackend.service.UserService;
import com.guochang.interviewpracticebackend.utils.SqlUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author 31179
*/
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question>
    implements QuestionService {
    @Resource
    private UserService userService;

    @Resource
    private QuestionBankQuestionService questionBankQuestionService;

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;


    /**
     * 校验数据
     *
     * @param question
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        ThrowUtils.throwIf(question == null, ErrorCode.PARAMS_ERROR);
        String title = question.getTitle();
        String content = question.getContent();
        // 创建数据时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(title), ErrorCode.PARAMS_ERROR);
        }
        // 修改数据时，有参数则校验
        if (StringUtils.isNotBlank(title)) {
            ThrowUtils.throwIf(title.length() > 80, ErrorCode.PARAMS_ERROR, "标题过长");
        }
        if (StringUtils.isNotBlank(content)) {
            ThrowUtils.throwIf(content.length() > 10240, ErrorCode.PARAMS_ERROR, "内容过长");
        }
    }

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        Long id = questionQueryRequest.getId();
        Long notId = questionQueryRequest.getNotId();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        String searchText = questionQueryRequest.getSearchText();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();
        List<String> tagList = questionQueryRequest.getTags();
        Long userId = questionQueryRequest.getUserId();
        String answer = questionQueryRequest.getAnswer();
        // 从多字段中搜索
        if (StringUtils.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.like(StringUtils.isNotBlank(answer), "answer", answer);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题目封装
     *
     * @param question
     * @param request
     * @return
     */
    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        // 对象转封装类
        QuestionVO questionVO = QuestionVO.objToVo(question);

        // region 可选
        // 1. 关联查询用户信息
        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionVO.setUser(userVO);
        // endregion
        return questionVO;
    }

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        // 从数据库查询结果（questionPage）中，取出当前页的题目列表（原始对象）
        List<Question> questionList = questionPage.getRecords();

        // 准备一个questionVOPage对象，参数完全复用原始对象的（第几页、每页几个、总共有多少）
        Page<QuestionVO> questionVOPage = new Page<>(
                questionPage.getCurrent(),
                questionPage.getSize(),
                questionPage.getTotal()
        );

        if (CollUtil.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 对象Question列表 => 封装对象QuestionVO列表
        List<QuestionVO> questionVOList = questionList.stream()
                .map(question -> {
                    // 调用专门的转换工具（objToVo）
                    return QuestionVO.objToVo(question);
                })
                .collect(Collectors.toList());

        // 收集所有出题人的ID（去重，避免重复查询）[关联查询用户信息]
        Set<Long> userIdSet = questionList.stream()
                .map(Question::getUserId)  // 从每个原始题目中，取出"创建者ID"
                .collect(Collectors.toSet());  // 用Set去重，比如多个题目是同一个人出的，只留一个ID

        // 批量查询用户信息，按ID分组
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));  // 把 "列表" 转成 "字典",方便快速查找

        // 填充信息
        questionVOList.forEach(questionVO -> {
            Long userId = questionVO.getUserId();  // 取出这个题目的创建者ID
            User user = null;
            // 从"字典"里找到对应的用户信息
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);  // 因为ID唯一，取第一个即可
            }
            // 把用户信息也转换成前端能看的格式（UserVO），再贴到题目上
            questionVO.setUser(userService.getUserVO(user));
        });

        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    /**
     * 分页获取题目列表
     *
     * @param questionQueryRequest
     * @return
     */
    public Page<Question> listQuestionByPage(QuestionQueryRequest questionQueryRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 题目表的查询条件
        QueryWrapper<Question> queryWrapper = this.getQueryWrapper(questionQueryRequest);
        // 根据题库查询题目列表接口
        Long questionBankId = questionQueryRequest.getQuestionBankId();
        if (questionBankId != null) {
            // 查询题库内的题目 id
            LambdaQueryWrapper<QuestionBankQuestion> lambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                    .select(QuestionBankQuestion::getQuestionId)
                    .eq(QuestionBankQuestion::getQuestionBankId, questionBankId);
            List<QuestionBankQuestion> questionList = questionBankQuestionService.list(lambdaQueryWrapper);
            if (CollUtil.isNotEmpty(questionList)) {
                // 取出题目 id 集合
                Set<Long> questionIdSet = questionList.stream()
                        .map(QuestionBankQuestion::getQuestionId)
                        .collect(Collectors.toSet());
                // 复用原有题目表的查询条件
                queryWrapper.in("id", questionIdSet);
            }
        }
        // 查询数据库
        Page<Question> questionPage = this.page(new Page<>(current, size), queryWrapper);
        return questionPage;
    }

    @Override
    public Page<Question> searchFromEs(QuestionQueryRequest questionQueryRequest) {
        // 获取参数
        Long id = questionQueryRequest.getId();
        Long notId = questionQueryRequest.getNotId();
        String searchText = questionQueryRequest.getSearchText();
        List<String> tags = questionQueryRequest.getTags();
        Long questionBankId = questionQueryRequest.getQuestionBankId();
        Long userId = questionQueryRequest.getUserId();
        // 注意，ES 的起始页为 0
        int current = questionQueryRequest.getCurrent() - 1;
        int pageSize = questionQueryRequest.getPageSize();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();

        // 构造查询条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 过滤
        boolQueryBuilder.filter(QueryBuilders.termQuery("isDelete", 0));
        if (id != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("id", id));
        }
        if (notId != null) {
            boolQueryBuilder.mustNot(QueryBuilders.termQuery("id", notId));
        }
        if (userId != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("userId", userId));
        }
        if (questionBankId != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("questionBankId", questionBankId));
        }
        // 必须包含所有标签
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                boolQueryBuilder.filter(QueryBuilders.termQuery("tags", tag));
            }
        }
        // 按关键词检索
        if (StringUtils.isNotBlank(searchText)) {
            boolQueryBuilder.should(QueryBuilders.matchQuery("title", searchText));
            boolQueryBuilder.should(QueryBuilders.matchQuery("content", searchText));
            boolQueryBuilder.should(QueryBuilders.matchQuery("answer", searchText));
            boolQueryBuilder.minimumShouldMatch(1);
        }
        // 排序
        SortBuilder<?> sortBuilder = SortBuilders.scoreSort();
        if (StringUtils.isNotBlank(sortField)) {
            sortBuilder = SortBuilders.fieldSort(sortField);
            sortBuilder.order(CommonConstant.SORT_ORDER_ASC.equals(sortOrder) ? SortOrder.ASC : SortOrder.DESC);
        }
        // 分页
        PageRequest pageRequest = PageRequest.of(current, pageSize);
        // 构造查询
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(pageRequest)
                .withSorts(sortBuilder)
                .build();
        SearchHits<QuestionEsDTO> searchHits = elasticsearchRestTemplate.search(searchQuery, QuestionEsDTO.class);
        // 复用 MySQL 的分页对象，封装返回结果
        Page<Question> page = new Page<>();
        page.setTotal(searchHits.getTotalHits());
        List<Question> resourceList = new ArrayList<>();
        if (searchHits.hasSearchHits()) {
            List<SearchHit<QuestionEsDTO>> searchHitList = searchHits.getSearchHits();
            for (SearchHit<QuestionEsDTO> questionEsDTOSearchHit : searchHitList) {
                resourceList.add(QuestionEsDTO.dtoToObj(questionEsDTOSearchHit.getContent()));
            }
        }
        page.setRecords(resourceList);
        return page;
    }

    @Override
    public void batchDeleteQuestions(List<Long> questionIdList) {
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR);
        for (Long questionId : questionIdList){
            boolean removed = this.removeById(questionId);
            ThrowUtils.throwIf(!removed, ErrorCode.OPERATION_ERROR);
            LambdaQueryWrapper<QuestionBankQuestion> lambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                    .eq(QuestionBankQuestion::getQuestionId,questionId);
            removed = questionBankQuestionService.remove(lambdaQueryWrapper);
            ThrowUtils.throwIf(!removed, ErrorCode.OPERATION_ERROR);
        }
    }
}




