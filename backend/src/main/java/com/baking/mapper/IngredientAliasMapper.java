package com.baking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baking.entity.IngredientAlias;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface IngredientAliasMapper extends BaseMapper<IngredientAlias> {

    @Select("SELECT canonical_name FROM ingredient_alias WHERE alias_name = #{aliasName}")
    String findCanonicalName(@Param("aliasName") String aliasName);

    @Select("SELECT alias_name FROM ingredient_alias WHERE canonical_name = #{canonicalName}")
    List<String> findAliasesByCanonicalName(@Param("canonicalName") String canonicalName);

    @Select("SELECT DISTINCT canonical_name FROM ingredient_alias")
    List<String> findAllCanonicalNames();
}
