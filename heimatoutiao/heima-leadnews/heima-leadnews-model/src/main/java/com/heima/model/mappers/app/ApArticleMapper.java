package com.heima.model.mappers.app;

import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.user.pojos.ApUserArticleList;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ApArticleMapper {
    List<ApArticle> loadArticleListByLocation(@Param("dto") ArticleHomeDto dto, @Param("type") Short type);

    List<ApArticle> loadArticleListByIdList(List<ApUserArticleList> list);

    ApArticle selectById(Long id);

    void insert(ApArticle apArticle);

    /**
     * 抽取最近的文章数据用于计算热文章
     *
     * @param lastDate
     * @return
     */
    List<ApArticle> loadLastArticleForHot(String lastDate);
    /**
     * 更新文章数
     * @param articleId
     * @param viewCount
     * @param collectCount
     * @param commontCount
     * @param likeCount
     * @return
     */
    int updateArticleView(Integer articleId, long viewCount,long collectCount,long commontCount,long likeCount);

    /**
     * 依靠文章IDS来获取文章详细内容
     */
    List<ApArticle> loadArticleListByIdListV2(List<Integer> list);

}
