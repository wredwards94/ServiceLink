package com.wesleyedwards.ServiceLink.dtos;

import java.util.List;

public record BulkResultDto(List<Long> succeeded, List<BulkFailureDto> failed) {}
