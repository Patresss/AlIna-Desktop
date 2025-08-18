package com.patres.alina.server.integration.alinaintegration;

import com.patres.alina.server.openai.function.FunctionRequest;

import java.util.function.BiFunction;
import java.util.function.Function;


public class AlinaIntegrationFunction<E extends AlinaIntegrationExecutor<?>, F extends FunctionRequest> {

    private final String functionName;
    private final String functionSuffixDescription;
    private final BiFunction<E, F, Object> executorCreator;
    private final Class<F> functionRequest;


    public AlinaIntegrationFunction(String functionName,
                                    String functionSuffixDescription,
                                    BiFunction<E, F, Object> executorCreator,
                                    Class<F> functionRequest) {
        this.functionName = functionName;
        this.functionSuffixDescription = functionSuffixDescription;
        this.executorCreator = executorCreator;
        this.functionRequest = functionRequest;
    }

    public String getFunctionSuffixDescription() {
        return functionSuffixDescription;
    }


    public Class<F> getFunctionRequest() {
        return functionRequest;
    }

    public String getFunctionName() {
        return functionName;
    }

    public Function<F, Object> createExecutor(E alinaIntegrationExecutor) {
        return functionRequest -> executorCreator.apply(alinaIntegrationExecutor, functionRequest);
    }

}
