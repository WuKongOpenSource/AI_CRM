package com.kakarote.ai_crm.ai.tools.support;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.service.ICustomerService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Shared customer matcher for AI tools.
 * Handles oral aliases such as "好莱坞客户" -> "好莱坞".
 */
@Component
public class AiCustomerMatcher {

    private static final int MAX_CANDIDATES = 10;
    private static final int MIN_MATCH_SCORE = 680;
    private static final int AMBIGUOUS_SCORE_GAP = 30;

    private static final Pattern SEPARATOR_PATTERN =
        Pattern.compile("[\\s\\p{Punct}，。、《》“”‘’（）()【】\\-_/]+");

    @Autowired
    private ICustomerService customerService;

    /**
     * 处理match方法逻辑。
     */
    public CustomerMatchResult match(String rawName) {
        String input = normalizeRawInput(rawName);
        if (input == null) {
            return CustomerMatchResult.notFound(rawName, List.of());
        }

        List<String> searchKeywords = buildSearchKeywords(input);
        Map<Long, Customer> candidateMap = new LinkedHashMap<>();
        for (String keyword : searchKeywords) {
            collectExactMatches(keyword, candidateMap);
            collectLikeMatches(keyword, candidateMap);
        }

        if (candidateMap.isEmpty()) {
            ScoredCustomer noAccessMatch = findBestIgnoringDataPermission(input, searchKeywords, List.of());
            if (isUsableNoAccessMatch(noAccessMatch)) {
                return CustomerMatchResult.existsNoAccess(input, noAccessMatch.getCustomer());
            }
            return CustomerMatchResult.notFound(input, List.of());
        }

        List<ScoredCustomer> scoredCustomers = candidateMap.values().stream()
            .map(customer -> new ScoredCustomer(customer, calculateBestScore(customer, input, searchKeywords)))
            .sorted(Comparator
                .comparingInt(ScoredCustomer::getScore).reversed()
                .thenComparing(scored -> scored.getCustomer().getCreateTime(), Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

        List<Customer> topCandidates = scoredCustomers.stream()
            .limit(3)
            .map(ScoredCustomer::getCustomer)
            .toList();

        ScoredCustomer best = scoredCustomers.get(0);
        ScoredCustomer noAccessMatch = findBestIgnoringDataPermission(input, searchKeywords, candidateMap.keySet());
        if (best.getScore() < MIN_MATCH_SCORE) {
            if (isUsableNoAccessMatch(noAccessMatch)) {
                return CustomerMatchResult.existsNoAccess(input, noAccessMatch.getCustomer());
            }
            return CustomerMatchResult.notFound(input, topCandidates);
        }

        if (isHighConfidenceNoAccessMatch(noAccessMatch) && noAccessMatch.getScore() > best.getScore()) {
            return CustomerMatchResult.existsNoAccess(input, noAccessMatch.getCustomer());
        }

        if (scoredCustomers.size() > 1) {
            ScoredCustomer second = scoredCustomers.get(1);
            if (best.getScore() < 930 && best.getScore() - second.getScore() < AMBIGUOUS_SCORE_GAP) {
                return CustomerMatchResult.ambiguous(input, topCandidates);
            }
        }

        return CustomerMatchResult.matched(input, best.getCustomer(), topCandidates);
    }

    /**
     * 查找BestIgnoring数据权限。
     */
    private ScoredCustomer findBestIgnoringDataPermission(String input,
                                                          List<String> searchKeywords,
                                                          Iterable<Long> accessibleCustomerIds) {
        LinkedHashSet<Long> accessibleIds = new LinkedHashSet<>();
        accessibleCustomerIds.forEach(accessibleIds::add);

        Map<Long, Customer> candidateMap = new LinkedHashMap<>();
        for (String keyword : searchKeywords) {
            collectExactMatchesIgnoringDataPermission(keyword, candidateMap);
            collectLikeMatchesIgnoringDataPermission(keyword, candidateMap);
        }
        accessibleIds.forEach(candidateMap::remove);

        if (candidateMap.isEmpty()) {
            return null;
        }

        return candidateMap.values().stream()
            .map(customer -> new ScoredCustomer(customer, calculateBestScore(customer, input, searchKeywords)))
            .max(Comparator.comparingInt(ScoredCustomer::getScore))
            .orElse(null);
    }

    /**
     * 判断是否Usable编号AccessMatch。
     */
    private boolean isUsableNoAccessMatch(ScoredCustomer scoredCustomer) {
        return scoredCustomer != null && scoredCustomer.getScore() >= MIN_MATCH_SCORE;
    }

    /**
     * 判断是否HighConfidence编号AccessMatch。
     */
    private boolean isHighConfidenceNoAccessMatch(ScoredCustomer scoredCustomer) {
        return scoredCustomer != null && scoredCustomer.getScore() >= 930;
    }

    /**
     * 处理collect精确Matches方法逻辑。
     */
    private void collectExactMatches(String keyword, Map<Long, Customer> candidateMap) {
        if (StrUtil.isBlank(keyword)) {
            return;
        }

        List<Customer> exactMatches = customerService.list(
            new LambdaQueryWrapper<Customer>()
                .eq(Customer::getStatus, 1)
                .eq(Customer::getCompanyName, keyword)
                .last("LIMIT " + MAX_CANDIDATES)
        );
        exactMatches.forEach(customer -> candidateMap.putIfAbsent(customer.getCustomerId(), customer));
    }

    /**
     * 处理collect模糊Matches方法逻辑。
     */
    private void collectLikeMatches(String keyword, Map<Long, Customer> candidateMap) {
        if (StrUtil.isBlank(keyword) || keyword.length() < 2) {
            return;
        }

        List<Customer> likeMatches = customerService.list(
            new LambdaQueryWrapper<Customer>()
                .eq(Customer::getStatus, 1)
                .like(Customer::getCompanyName, keyword)
                .orderByDesc(Customer::getCreateTime)
                .last("LIMIT " + MAX_CANDIDATES)
        );
        likeMatches.forEach(customer -> candidateMap.putIfAbsent(customer.getCustomerId(), customer));
    }

    /**
     * 处理collect精确MatchesIgnoringDataPermission方法逻辑。
     */
    private void collectExactMatchesIgnoringDataPermission(String keyword, Map<Long, Customer> candidateMap) {
        if (StrUtil.isBlank(keyword)) {
            return;
        }

        List<Customer> exactMatches = customerService.findCustomersByExactCompanyNameIgnoreDataPermission(keyword);
        exactMatches.forEach(customer -> candidateMap.putIfAbsent(customer.getCustomerId(), customer));
    }

    /**
     * 处理collect模糊MatchesIgnoringDataPermission方法逻辑。
     */
    private void collectLikeMatchesIgnoringDataPermission(String keyword, Map<Long, Customer> candidateMap) {
        if (StrUtil.isBlank(keyword) || keyword.length() < 2) {
            return;
        }

        List<Customer> likeMatches = customerService.findCustomersByCompanyNameLikeIgnoreDataPermission(keyword, MAX_CANDIDATES);
        likeMatches.forEach(customer -> candidateMap.putIfAbsent(customer.getCustomerId(), customer));
    }

    /**
     * 构建搜索Keywords。
     */
    private List<String> buildSearchKeywords(String input) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        addKeyword(keywords, input);

        String strippedNoise = stripConversationNoise(input);
        addKeyword(keywords, strippedNoise);

        String withoutSeparators = removeSeparators(strippedNoise);
        addKeyword(keywords, withoutSeparators);

        String normalizedAlias = normalizeForComparison(input);
        addKeyword(keywords, normalizedAlias);

        String shortened = stripLegalSuffixes(withoutSeparators);
        addKeyword(keywords, shortened);

        return new ArrayList<>(keywords);
    }

    /**
     * 新增Keyword。
     */
    private void addKeyword(LinkedHashSet<String> keywords, String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return;
        }
        String trimmed = keyword.trim();
        if (trimmed.length() < 2) {
            return;
        }
        keywords.add(trimmed);
    }

    /**
     * 处理calculateBestScore方法逻辑。
     */
    private int calculateBestScore(Customer customer, String rawInput, List<String> searchKeywords) {
        String companyName = StrUtil.blankToDefault(StrUtil.trim(customer.getCompanyName()), "");
        String normalizedCompany = normalizeForComparison(companyName);
        int best = 0;

        for (String keyword : searchKeywords) {
            String normalizedKeyword = normalizeForComparison(keyword);
            if (StrUtil.isBlank(normalizedKeyword)) {
                continue;
            }

            int score = 0;
            if (companyName.equals(keyword)) {
                score = 1000;
            } else if (normalizedCompany.equals(normalizedKeyword)) {
                score = 950;
            } else if (companyName.contains(keyword)) {
                score = 900;
            } else if (normalizedCompany.contains(normalizedKeyword)) {
                score = 860;
            } else if (normalizedKeyword.contains(normalizedCompany)) {
                score = 820;
            } else if (countSharedCharacters(normalizedCompany, normalizedKeyword) >= Math.max(2, Math.min(normalizedCompany.length(), normalizedKeyword.length()) - 1)) {
                score = 700;
            }

            if (score > 0) {
                score -= Math.abs(normalizedCompany.length() - normalizedKeyword.length()) * 3;
            }
            best = Math.max(best, score);
        }

        String normalizedInput = normalizeForComparison(rawInput);
        if (best > 0 && normalizedCompany.startsWith(normalizedInput)) {
            best += 10;
        }
        return best;
    }

    /**
     * 处理countSharedCharacters方法逻辑。
     */
    private int countSharedCharacters(String left, String right) {
        int count = 0;
        for (char ch : right.toCharArray()) {
            if (left.indexOf(ch) >= 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * 标准化RAW输入。
     */
    private String normalizeRawInput(String value) {
        String normalized = StrUtil.trim(value);
        if (StrUtil.isBlank(normalized) || "null".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
    }

    /**
     * 标准化用于Comparison。
     */
    private String normalizeForComparison(String value) {
        String normalized = stripConversationNoise(value);
        normalized = removeSeparators(normalized);
        normalized = stripLegalSuffixes(normalized);
        normalized = stripConversationNoise(normalized);
        return normalized;
    }

    /**
     * 处理stripConversationNoise方法逻辑。
     */
    private String stripConversationNoise(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }

        String result = value.trim();
        boolean changed = true;
        while (changed && StrUtil.isNotBlank(result)) {
            changed = false;

            String strippedPrefix = StrUtil.removePrefixIgnoreCase(result, "这个客户");
            strippedPrefix = StrUtil.removePrefixIgnoreCase(strippedPrefix, "这家客户");
            strippedPrefix = StrUtil.removePrefixIgnoreCase(strippedPrefix, "这家公司");
            strippedPrefix = StrUtil.removePrefixIgnoreCase(strippedPrefix, "该客户");
            strippedPrefix = StrUtil.removePrefixIgnoreCase(strippedPrefix, "该公司");
            if (!strippedPrefix.equals(result) && StrUtil.isNotBlank(strippedPrefix)) {
                result = strippedPrefix.trim();
                changed = true;
            }

            String[] suffixes = {
                "客户", "客户方", "公司客户", "集团客户", "企业客户",
                "这个客户", "这家客户", "这家公司", "该客户", "该公司", "公司"
            };
            for (String suffix : suffixes) {
                if (result.endsWith(suffix) && result.length() > suffix.length()) {
                    result = StrUtil.removeSuffix(result, suffix).trim();
                    changed = true;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 移除Separators。
     */
    private String removeSeparators(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        return SEPARATOR_PATTERN.matcher(value).replaceAll("");
    }

    /**
     * 处理stripLegalSuffixes方法逻辑。
     */
    private String stripLegalSuffixes(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }

        String result = value;
        boolean changed = true;
        while (changed && StrUtil.isNotBlank(result)) {
            changed = false;
            String[] suffixes = {"有限责任公司", "股份有限公司", "集团有限公司", "有限公司", "集团公司", "公司", "集团"};
            for (String suffix : suffixes) {
                if (result.endsWith(suffix) && result.length() > suffix.length()) {
                    result = StrUtil.removeSuffix(result, suffix);
                    changed = true;
                    break;
                }
            }
        }
        return result;
    }

    @Getter
    public static class CustomerMatchResult {
        private final String rawName;
        private final Customer customer;
        private final List<Customer> candidateCustomers;
        private final MatchStatus status;

        /**
         * 处理CustomerMatchResult方法逻辑。
         */
        private CustomerMatchResult(String rawName, Customer customer, List<Customer> candidateCustomers, MatchStatus status) {
            this.rawName = rawName;
            this.customer = customer;
            this.candidateCustomers = candidateCustomers;
            this.status = status;
        }

        /**
         * 处理matched方法逻辑。
         */
        public static CustomerMatchResult matched(String rawName, Customer customer, List<Customer> candidateCustomers) {
            return new CustomerMatchResult(rawName, customer, candidateCustomers, MatchStatus.MATCHED);
        }

        /**
         * 处理notFound方法逻辑。
         */
        public static CustomerMatchResult notFound(String rawName, List<Customer> candidateCustomers) {
            return new CustomerMatchResult(rawName, null, candidateCustomers, MatchStatus.NOT_FOUND);
        }

        /**
         * 处理ambiguous方法逻辑。
         */
        public static CustomerMatchResult ambiguous(String rawName, List<Customer> candidateCustomers) {
            return new CustomerMatchResult(rawName, null, candidateCustomers, MatchStatus.AMBIGUOUS);
        }

        /**
         * 处理existsNoAccess方法逻辑。
         */
        public static CustomerMatchResult existsNoAccess(String rawName, Customer customer) {
            return new CustomerMatchResult(rawName, customer, List.of(), MatchStatus.EXISTS_NO_ACCESS);
        }

        /**
         * 判断是否Matched。
         */
        public boolean isMatched() {
            return status == MatchStatus.MATCHED && customer != null;
        }

        /**
         * 判断是否Ambiguous。
         */
        public boolean isAmbiguous() {
            return status == MatchStatus.AMBIGUOUS;
        }

        /**
         * 判断是否Exists编号Access。
         */
        public boolean isExistsNoAccess() {
            return status == MatchStatus.EXISTS_NO_ACCESS;
        }

        public String formatNoAccessMessage(String actionLabel) {
            String displayName = customer != null && StrUtil.isNotBlank(customer.getCompanyName())
                ? customer.getCompanyName()
                : rawName;
            String ownerName = customer == null ? null : customer.getOwnerName();
            String ownerText = "（负责人：" + StrUtil.blankToDefault(ownerName, "未知") + "）";
            return "操作未执行：客户已存在：「" + displayName + "」" + ownerText
                + "。当前用户没有这个客户的权限，无法" + actionLabel + "。";
        }

        /**
         * 格式化Candidate名称。
         */
        public String formatCandidateNames() {
            return candidateCustomers.stream()
                .map(Customer::getCompanyName)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining("、"));
        }
    }

    public enum MatchStatus {
        MATCHED,
        NOT_FOUND,
        AMBIGUOUS,
        EXISTS_NO_ACCESS
    }

    @Getter
    private static class ScoredCustomer {
        private final Customer customer;
        private final int score;

        /**
         * 处理ScoredCustomer方法逻辑。
         */
        private ScoredCustomer(Customer customer, int score) {
            this.customer = customer;
            this.score = score;
        }
    }
}
