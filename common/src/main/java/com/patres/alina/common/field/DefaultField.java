package com.patres.alina.common.field;

public record DefaultField(
        String fieldNameCode,
        String defaultFieldEnglishName,
        String defaultValue,
        String defaultEnglishDescription,
        boolean mandatory,
        FormFieldType formFieldType
){

}
