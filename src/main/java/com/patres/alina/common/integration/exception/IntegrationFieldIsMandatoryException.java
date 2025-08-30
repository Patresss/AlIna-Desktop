package com.patres.alina.common.integration.exception;

import com.patres.alina.common.exception.WarningException;

public class IntegrationFieldIsMandatoryException extends WarningException {

    public IntegrationFieldIsMandatoryException(String fieldName) {
        super("Integration field " + fieldName + " is mandatory");
    }
}
