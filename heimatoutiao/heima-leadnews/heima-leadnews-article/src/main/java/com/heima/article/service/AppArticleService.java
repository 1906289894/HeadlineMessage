package com.heima.article.service;

import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.mess.app.ArticleVisitStreamDto;

public interface AppArticleService {
    /**
     * type  1 加载更多  2 加载更新
     * @param dto
     * @param type
     * @return
     */
    ResponseResult load(ArticleHomeDto dto, Short type);

    /**
     * 更新阅读数
     * @param dto
     * @return
     */
    ResponseResult updateArticleView(ArticleVisitStreamDto dto);
}
