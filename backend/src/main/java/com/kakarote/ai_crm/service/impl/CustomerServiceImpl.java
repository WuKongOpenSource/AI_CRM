package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.ContactAddBO;
import com.kakarote.ai_crm.entity.BO.CustomerAddBO;
import com.kakarote.ai_crm.entity.BO.CustomerQueryBO;
import com.kakarote.ai_crm.entity.BO.CustomerUpdateBO;
import com.kakarote.ai_crm.entity.PO.Contact;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.CustomerTag;
import com.kakarote.ai_crm.entity.PO.Task;
import com.kakarote.ai_crm.entity.VO.ContactVO;
import com.kakarote.ai_crm.entity.VO.CustomerDetailVO;
import com.kakarote.ai_crm.entity.VO.CustomerListVO;
import com.kakarote.ai_crm.entity.VO.DashboardStatsVO;
import com.kakarote.ai_crm.mapper.ContactMapper;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.CustomerTagMapper;
import com.kakarote.ai_crm.mapper.TaskMapper;
import com.kakarote.ai_crm.service.ICustomerService;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 客户服务实现
 */
@Slf4j
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements ICustomerService {

    @Autowired
    private ContactMapper contactMapper;

    @Autowired
    private CustomerTagMapper customerTagMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ICustomFieldService customFieldService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addCustomer(CustomerAddBO customerAddBO) {
        // Create customer
        Customer customer = BeanUtil.copyProperties(customerAddBO, Customer.class);
        customer.setOwnerId(UserUtil.getUserId());
        customer.setStatus(1);
        if (StrUtil.isEmpty(customer.getStage())) {
            customer.setStage("lead");
        }
        if (StrUtil.isEmpty(customer.getLevel())) {
            customer.setLevel("B");
        }
        save(customer);

        // Create primary contact if provided
        if (StrUtil.isNotEmpty(customerAddBO.getContactName())) {
            Contact contact = new Contact();
            contact.setCustomerId(customer.getCustomerId());
            contact.setName(customerAddBO.getContactName());
            contact.setPhone(customerAddBO.getContactPhone());
            contact.setEmail(customerAddBO.getContactEmail());
            contact.setPosition(customerAddBO.getContactPosition());
            contact.setIsPrimary(1);
            contact.setStatus(1);
            contactMapper.insert(contact);
        }

        // Save custom fields
        if (customerAddBO.getCustomFields() != null && !customerAddBO.getCustomFields().isEmpty()) {
            customFieldService.updateCustomFieldValues("customer", customer.getCustomerId(), customerAddBO.getCustomFields());
        }

        return customer.getCustomerId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomer(CustomerUpdateBO customerUpdateBO) {
        Customer customer = getById(customerUpdateBO.getCustomerId());
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在");
        }
        BeanUtil.copyProperties(customerUpdateBO, customer, "customerId", "createUserId", "createTime", "customFields");
        updateById(customer);

        // 更新自定义字段
        if (customerUpdateBO.getCustomFields() != null && !customerUpdateBO.getCustomFields().isEmpty()) {
            customFieldService.updateCustomFieldValues("customer", customerUpdateBO.getCustomerId(), customerUpdateBO.getCustomFields());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomer(Long customerId) {
        Customer customer = getById(customerId);
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在");
        }
        removeById(customerId);
        // Delete related contacts
        contactMapper.delete(new LambdaQueryWrapper<Contact>().eq(Contact::getCustomerId, customerId));
        // Delete related tags
        customerTagMapper.delete(new LambdaQueryWrapper<CustomerTag>().eq(CustomerTag::getCustomerId, customerId));
    }

    @Override
    public BasePage<CustomerListVO> queryPageList(CustomerQueryBO queryBO) {
        BasePage<CustomerListVO> page = queryBO.parse();
        baseMapper.queryPageList(page, queryBO);

        // Populate custom fields for each customer in the list
        List<CustomerListVO> records = page.getRecords();
        if (records != null && !records.isEmpty()) {
            for (CustomerListVO record : records) {
                Map<String, Object> customFields = customFieldService.getCustomFieldValues("customer", record.getCustomerId());
                record.setCustomFields(customFields);
            }
        }

        return page;
    }

    @Override
    public CustomerDetailVO getCustomerDetail(Long customerId) {
        CustomerListVO customer = baseMapper.getCustomerById(customerId);
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在");
        }

        CustomerDetailVO detail = BeanUtil.copyProperties(customer, CustomerDetailVO.class);

        // Get contacts - wrapped in try-catch to prevent failures from breaking the entire API
        try {
            List<Contact> contacts = contactMapper.selectList(
                new LambdaQueryWrapper<Contact>()
                    .eq(Contact::getCustomerId, customerId)
                    .eq(Contact::getStatus, 1)
            );
            log.info("查询客户[{}]的联系人数量: {}", customerId, contacts.size());
            detail.setContacts(BeanUtil.copyToList(contacts, ContactVO.class));
        } catch (Exception e) {
            log.error("查询客户[{}]的联系人失败: {}", customerId, e.getMessage(), e);
            detail.setContacts(new java.util.ArrayList<>());
        }

        // Get tags - wrapped in try-catch
        try {
            List<CustomerTag> tags = customerTagMapper.selectList(
                new LambdaQueryWrapper<CustomerTag>().eq(CustomerTag::getCustomerId, customerId)
            );
            log.info("查询客户[{}]的标签数量: {}", customerId, tags.size());
            detail.setTags(tags);
        } catch (Exception e) {
            log.error("查询客户[{}]的标签失败: {}", customerId, e.getMessage(), e);
            detail.setTags(new java.util.ArrayList<>());
        }

        // Get custom fields - wrapped in try-catch to prevent failures from breaking the entire API
        try {
            Map<String, Object> customFields = customFieldService.getCustomFieldValues("customer", customerId);
            log.info("查询客户[{}]的自定义字段: {}", customerId, customFields);
            detail.setCustomFields(customFields);
        } catch (Exception e) {
            log.error("查询客户[{}]的自定义字段失败: {}", customerId, e.getMessage(), e);
            detail.setCustomFields(new java.util.HashMap<>());
        }

        // Get related tasks - wrapped in try-catch
        try {
            List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                    .eq(Task::getCustomerId, customerId)
                    .orderByDesc(Task::getCreateTime)
                    .last("LIMIT 10")
            );
            log.info("查询客户[{}]的相关任务数量: {}", customerId, tasks.size());
            detail.setTasks(tasks);
        } catch (Exception e) {
            log.error("查询客户[{}]的相关任务失败: {}", customerId, e.getMessage(), e);
            detail.setTasks(new java.util.ArrayList<>());
        }

        return detail;
    }

    @Override
    public void updateStage(Long customerId, String stage) {
        Customer customer = getById(customerId);
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在");
        }
        customer.setStage(stage);
        updateById(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addTag(Long customerId, String tagName, String color) {
        Customer customer = getById(customerId);
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在");
        }

        // Check if tag already exists
        Long count = customerTagMapper.selectCount(
            new LambdaQueryWrapper<CustomerTag>()
                .eq(CustomerTag::getCustomerId, customerId)
                .eq(CustomerTag::getTagName, tagName)
        );
        if (count > 0) {
            return; // Tag already exists
        }

        CustomerTag tag = new CustomerTag();
        tag.setCustomerId(customerId);
        tag.setTagName(tagName);
        tag.setColor(StrUtil.isEmpty(color) ? "#3b82f6" : color);
        customerTagMapper.insert(tag);
    }

    @Override
    public void removeTag(Long customerId, Long tagId) {
        customerTagMapper.deleteById(tagId);
    }

    @Override
    public DashboardStatsVO getStatistics() {
        DashboardStatsVO stats = new DashboardStatsVO();

        // Total customers
        stats.setTotalCustomers(count());

        // Count by stage
        List<DashboardStatsVO.StageCountVO> stageStats = new java.util.ArrayList<>();
        String[] stages = {"lead", "qualified", "proposal", "negotiation", "closed", "lost"};
        String[] stageNames = {"线索", "已验证", "方案阶段", "商务谈判", "已成交", "已流失"};
        for (int i = 0; i < stages.length; i++) {
            DashboardStatsVO.StageCountVO stageCount = new DashboardStatsVO.StageCountVO();
            stageCount.setStage(stages[i]);
            stageCount.setStageName(stageNames[i]);
            stageCount.setCount(lambdaQuery().eq(Customer::getStage, stages[i]).count());
            stageStats.add(stageCount);
        }
        stats.setCustomersByStage(stageStats);

        // Count by level
        List<DashboardStatsVO.LevelCountVO> levelStats = new java.util.ArrayList<>();
        String[] levels = {"A", "B", "C"};
        for (String level : levels) {
            DashboardStatsVO.LevelCountVO levelCount = new DashboardStatsVO.LevelCountVO();
            levelCount.setLevel(level);
            levelCount.setCount(lambdaQuery().eq(Customer::getLevel, level).count());
            levelStats.add(levelCount);
        }
        stats.setCustomersByLevel(levelStats);

        // Active deals (not closed or lost)
        stats.setActiveDeals(lambdaQuery()
            .notIn(Customer::getStage, "closed", "lost")
            .count());

        return stats;
    }
}
