package com.patres.alina.common.field;

public record FormField(String id,
                        String nameCode,
                        String defaultEnglishName,
                        String descriptionCode,
                        String defaultEnglishDescription,
                        boolean mandatory,
                        FormFieldType formFieldType,
                        Object value,
                        Object initValue) {

    public FormField(FormField formField, Object value) {
        this(
                formField.id,
                formField.nameCode,
                formField.defaultEnglishName,
                formField.descriptionCode,
                formField.defaultEnglishDescription,
                formField.mandatory,
                formField.formFieldType,
                value,
                null);
    }

    public FormField(String id,
                     String nameCode,
                     String defaultEnglishName,
                     String descriptionCode,
                     String defaultEnglishDescription,
                     boolean mandatory,
                     FormFieldType formFieldType) {
        this(
                id,
                nameCode,
                defaultEnglishName,
                descriptionCode,
                defaultEnglishDescription,
                mandatory,
                formFieldType,
                null,
                null);
    }
}
