package com.patres.alina.server.mcp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record McpServerConfiguration(
        @JsonProperty("command") String command,
        @JsonProperty("args") List<String> args,
        @JsonProperty("env") Map<String, String> env
) {
    @JsonCreator
    public McpServerConfiguration(
            @JsonProperty("command") String command,
            @JsonProperty("args") List<String> args,
            @JsonProperty("env") Map<String, String> env) {
        this.command = command;
        this.args = args != null ? args : List.of();
        this.env = env != null ? env : Map.of();
    }
}