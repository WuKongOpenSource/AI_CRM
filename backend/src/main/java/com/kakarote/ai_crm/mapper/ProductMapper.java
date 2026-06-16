package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.ProductQueryBO;
import com.kakarote.ai_crm.entity.PO.Product;
import com.kakarote.ai_crm.entity.VO.ProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    BasePage<ProductVO> queryPageList(IPage<ProductVO> page, @Param("query") ProductQueryBO query);

    ProductVO getProductById(@Param("productId") Long productId);

    @InterceptorIgnore(dataPermission = "true")
    @Select("""
            SELECT *
            FROM crm_product
            WHERE product_id = #{productId}
            """)
    Product selectByIdIgnoreDataPermission(@Param("productId") Long productId);

    @InterceptorIgnore(dataPermission = "true")
    @Select("""
            SELECT *
            FROM crm_product
            WHERE product_code = #{productCode}
              AND del_flag = 0
            LIMIT 1
            """)
    Product selectByCodeIgnoreDataPermission(@Param("productCode") String productCode);

    @InterceptorIgnore(dataPermission = "true")
    @Select("""
            SELECT COUNT(1)
            FROM crm_product
            WHERE product_code = #{productCode}
              AND del_flag = 0
              AND product_id <> COALESCE(CAST(#{excludeProductId,jdbcType=BIGINT} AS BIGINT), -1)
            """)
    Long countByCodeIgnoreDataPermission(@Param("productCode") String productCode,
                                         @Param("excludeProductId") Long excludeProductId);

    @InterceptorIgnore(dataPermission = "true")
    @Select("""
            SELECT COUNT(1)
            FROM crm_product
            WHERE category_id = #{categoryId}
              AND del_flag = 0
            """)
    Long countByCategoryIdIgnoreDataPermission(@Param("categoryId") Long categoryId);

    @InterceptorIgnore(dataPermission = "true")
    @Select("""
            SELECT *
            FROM crm_product
            WHERE del_flag = 0
            ORDER BY update_time DESC NULLS LAST, create_time DESC
            """)
    List<Product> selectActiveProductsIgnoreDataPermission();
}
