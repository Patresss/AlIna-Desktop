package com.patres.alina.common.message;

/**
 * Functional interface for handling completion of AI message processing.
 * Called when the AI has finished generating the response.
 */
@FunctionalInterface
public interface OnMessageCompleteCallback {

    /**
     * Called when AI message processing is complete.
     *
     * @param aiResponse The complete response from the AI
     */
    void onComplete(String aiResponse);
}
