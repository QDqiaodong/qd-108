package com.baking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baking.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
