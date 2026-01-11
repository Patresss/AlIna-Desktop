package com.patres.alina.common.message;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public record CommandUsageInfo(
        String commandId,
        String commandName,
        String commandIcon,
        String prompt
) {
}
