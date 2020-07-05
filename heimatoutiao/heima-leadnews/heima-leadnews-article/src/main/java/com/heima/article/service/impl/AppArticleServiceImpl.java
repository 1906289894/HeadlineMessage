package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.heima.article.service.AppArticleService;
import com.heima.common.article.constans.ArticleConstans;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApHotArticles;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.mappers.app.ApArticleMapper;
import com.heima.model.mappers.app.ApBehaviorEntryMapper;
import com.heima.model.mappers.app.ApHotArticlesMapper;
import com.heima.model.mappers.app.ApUserArticleListMapper;
import com.heima.model.mess.app.ArticleVisitStreamDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.user.pojos.ApUserArticleList;
import com.heima.utils.threadlocal.AppThreadLocalUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
@SuppressWarnings("all")
public class AppArticleServiceImpl implements AppArticleService {

    private static final short MAX_PAGE_SIZE = 50;

    @Autowired
    private ApHotArticlesMapper apHotArticlesMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ApBehaviorEntryMapper apBehaviorEntryMapper;

    @Override
    public ResponseResult load(ArticleHomeDto dto, Short type) {
        //参数校验
        if(dto ==null ){
            dto = new ArticleHomeDto();
        }
        //时间校验
        if(dto.getMaxBehotTime()==null){
            dto.setMaxBehotTime(new Date());
        }

        if(dto.getMinBehotTime()==null){
            dto.setMinBehotTime(new Date());
        }

        //分页参数的校验
        Integer size = dto.getSize();
        if(size==null || size <= 0){
            size = 20;
        }
        size = Math.min(size,MAX_PAGE_SIZE);
        dto.setSize(size);

        //文章频道参数校验
        if(StringUtils.isEmpty(dto.getTag())){
            dto.setTag(ArticleConstans.DEFAULT_TAG);
        }

        //类型参数校验
        if(!type.equals(ArticleConstans.LOADTYPE_LOAD_MORE)&&!type.equals(ArticleConstans.LOADTYPE_LOAD_NEW)){
            type = ArticleConstans.LOADTYPE_LOAD_MORE;
        }

        //获取用户的信息
        ApUser user = AppThreadLocalUtils.getUser();

        //判断用户是否存在
        if(user != null){
            //存在 已登录 加载推荐的文章
            List<ApArticle> apArticleList = getUserArticle(user,dto,type);
            return ResponseResult.okResult(apArticleList);
        }else{
            //不存在 未登录，加载默认的文章
            List<ApArticle> apArticles = getDefaultArticle(dto,type);
            return ResponseResult.okResult(apArticles);
        }

    }

    @Override
    public ResponseResult updateArticleView(ArticleVisitStreamDto dto) {
        int rows = apArticleMapper.updateArticleView(dto.getArticleId(),
                dto.getView(),dto.getCollect(),dto.getCommont(),dto.getLike());
        log.info("更新文章阅读数#articleId：{},dto：{}", dto.getArticleId(), JSON.toJSONString(dto),rows);
        return ResponseResult.okResult(rows);
    }

    @Override
    public ResponseResult loadV2(Short type, ArticleHomeDto dto, boolean firstPage) {
        if (null == dto){
            dto = new ArticleHomeDto();
        }
        ApUser user = AppThreadLocalUtils.getUser();
        Integer size = dto.getSize();
        String tag = dto.getTag();
        //分页参数校验
        if (size == null || size <= 0) {
            size = 20;
        }
        size = Math.min(size,MAX_PAGE_SIZE);
        dto.setSize(size);
        //  类型参数校验
        if (!type.equals(ArticleConstans.LOADTYPE_LOAD_MORE) && !type.equals(ArticleConstans.LOADTYPE_LOAD_NEW))
            type = ArticleConstans.LOADTYPE_LOAD_MORE;
        // 文章频道参数验证
        if (StringUtils.isEmpty(tag)) {
            dto.setTag(ArticleConstans.DEFAULT_TAG);
        }
        // 最大时间处理
        if(dto.getMaxBehotTime()==null){
            dto.setMaxBehotTime(new Date());
        }
        // 最小时间处理
        if(dto.getMinBehotTime()==null){
            dto.setMinBehotTime(new Date());
        }
        //从缓存中读取 否则数据库查询
        if (firstPage){
            // 数据加载
            List<ApArticle> cacheList = getCacheArticleV2(dto);
            if (cacheList.size()>0){
                log.info("使用缓存加载数据#tag:{}", dto.getTag());
                return ResponseResult.okResult(getCacheArticleV2(dto));
            }
        }
        //数据加载
        if (user!=null){
            return ResponseResult.okResult(getUserArticleV2(user,dto,type));
        }else {
            return ResponseResult.okResult(getDefaultArticleV2(dto,type));
        }
    }

    /**
     * 从默认的热数据列表中获取文章
     * @param dto
     * @param type
     * @return
     */
    private List<ApArticle> getDefaultArticleV2(ArticleHomeDto dto, Short type) {
        List<ApHotArticles> hotList = apHotArticlesMapper.loadHotListByLocation(dto,type);
        List<ApArticle> articleList = Lists.newArrayList();
        for (ApHotArticles hot :hotList){
            ApArticle apArticle = apArticleMapper.selectById(Long.valueOf(hot.getArticleId()));
            articleList.add(apArticle);
        }
        return articleList;
    }

    /**
     * 查询缓存首页文章数据
     * @param dto
     * @return
     */
    private List<ApArticle> getCacheArticleV2(ArticleHomeDto dto) {
        log.info("查询缓存热文章数据#tag:{}",dto.getTag());
        String key = ArticleConstans.HOT_ARTICLE_FIRST_PAGE+dto.getTag();
        String ret = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isEmpty(ret)){
            return Lists.newArrayList();
        }
        List<ApArticle> list = JSONArray.parseArray(ret,ApArticle.class);
        return list;
    }

    /**
     * 先从用户的推荐表中查找文章，如果没有再从大文章列表中获取
     * @param user
     * @param dto
     * @param type
     * @return
     */
    private List<ApArticle> getUserArticleV2(ApUser user, ArticleHomeDto dto, Short type){
        // 用户和设备不能同时为空
        if(user == null){
            return Lists.newArrayList();
        }
        Long userId = user.getId();
        ApBehaviorEntry apBehaviorEntry = apBehaviorEntryMapper.selectByUserIdOrEquipemntId(userId, null);
        // 行为实体找以及注册了，逻辑上这里是必定有值得，除非参数错误
        if (apBehaviorEntry==null){
            return Lists.newArrayList();
        }

        Integer entryId = apBehaviorEntry.getId();
        long time = System.currentTimeMillis();
        List<ApHotArticles> list = apHotArticlesMapper.loadArticleIdListByEntryId(entryId, dto, type);
        System.out.println("==================1=:"+(System.currentTimeMillis()-time));
        //默认从热文章里查询
        if(!list.isEmpty()){
            List<Integer> articleList = list.stream().map(p -> p.getArticleId()).collect(Collectors.toList());
            List<ApArticle> temp = apArticleMapper.loadArticleListByIdListV2(articleList);
            System.out.println("==================2=:"+(System.currentTimeMillis()-time));
            return temp;
        }else{
            return getDefaultArticleV2(dto,type);
        }
    }
    @Autowired
    private ApArticleMapper apArticleMapper;

    /**
     * 加载默认的文章信息
     * @param dto
     * @param type
     * @return
     */
    private List<ApArticle> getDefaultArticle(ArticleHomeDto dto, Short type) {
        return apArticleMapper.loadArticleListByLocation(dto,type);
    }

    @Autowired
    private ApUserArticleListMapper apUserArticleListMapper;

    /**
     * 先从用户的推荐表中查找文章信息，如果没有再从默认文章信息获取数据
     * @param user
     * @param dto
     * @param type
     * @return
     */
    private List<ApArticle> getUserArticle(ApUser user, ArticleHomeDto dto, Short type) {
        List<ApUserArticleList> list = apUserArticleListMapper.loadArticleIdListByUser(user,dto,type);
        if(!list.isEmpty()){
            return apArticleMapper.loadArticleListByIdList(list);
        }else{
            return getDefaultArticle(dto,type);
        }
    }
}
