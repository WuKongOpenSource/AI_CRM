package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.AddressBookQueryBO;
import com.kakarote.ai_crm.entity.VO.AddressBookDetailVO;
import com.kakarote.ai_crm.entity.VO.AddressBookEmployeeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AddressBookMapper {

    @InterceptorIgnore(dataPermission = "true")
    BasePage<AddressBookEmployeeVO> queryPageList(BasePage<AddressBookEmployeeVO> page,
                                                  @Param("query") AddressBookQueryBO query);

    @InterceptorIgnore(dataPermission = "true")
    AddressBookDetailVO getDetail(@Param("userId") Long userId);
}
