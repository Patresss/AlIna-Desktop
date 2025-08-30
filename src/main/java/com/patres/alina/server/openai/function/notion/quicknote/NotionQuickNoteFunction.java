package com.patres.alina.server.openai.function.notion.quicknote;//package com.patres.alina.server.openai.function.notion.quicknote;
//
//import com.patres.alina.server.openai.function.rest.RestFunction;
//import com.patres.alina.server.openai.function.rest.RestFunctionProperties;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.http.HttpMethod;
//import org.springframework.stereotype.Component;
//
//
//@Component
//public class NotionQuickNoteFunction extends RestFunction<NotionQuickNoteRequest> {
//
//    private static final Logger logger = LoggerFactory.getLogger(NotionQuickNoteFunction.class);
//
//
//    public NotionQuickNoteFunction() {
//        super(new RestFunctionProperties<>(
//                "https://hook.eu2.make.com/85o477h3xydwitxp1u53j57qv2pkc3m2",
//                HttpMethod.POST,
//                "add_a_quick_note_to_notion",
//                "Add quick note to the Notion",
//                NotionQuickNoteRequest.class
//        ));
//    }
//
//
//}
