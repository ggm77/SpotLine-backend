package com.pohanghang.spotline.domain.analytics.model;

import com.pohanghang.spotline.domain.analytics.entity.AgeGroup;
import com.pohanghang.spotline.domain.analytics.entity.Gender;

/**
 * (성별, 나이대)별로 집계된 방문자 수. 핵심 고객 분석에 사용된다.
 */
public class CoreCustomerGroup {

    private final Gender gender;
    private final AgeGroup ageGroup;
    private final long totalCount;

    public CoreCustomerGroup(
            final Gender gender,
            final AgeGroup ageGroup,
            final long totalCount
    ) {
        this.gender = gender;
        this.ageGroup = ageGroup;
        this.totalCount = totalCount;
    }

    public Gender getGender() {
        return gender;
    }

    public AgeGroup getAgeGroup() {
        return ageGroup;
    }

    public Long getTotalCount() {
        return totalCount;
    }
}
