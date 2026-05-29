package com.pohanghang.spotline.domain.vision.dto;

import java.time.LocalDateTime;
import java.util.List;

public record VisionDataRequestDto(
        Integer totalCount,
        Integer peakTime,
        Integer maxResponseWaitTime,
        List<VisionDataPersonDto> people,
        Integer maxEmptyTableTime,
        Integer coreCustomerAge,
        Integer coreCustomerGender,
        Integer avgDwellTime,
        Integer justLeftCount,
        LocalDateTime capturedAt
) { }
