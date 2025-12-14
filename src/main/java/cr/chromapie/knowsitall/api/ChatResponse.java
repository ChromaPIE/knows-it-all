package cr.chromapie.knowsitall.api;

import java.util.List;

public class ChatResponse {

    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;
    private Error error;

    public String getContent() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        Message message = choices.get(0).message;
        return message != null ? message.content : null;
    }

    public boolean hasError() {
        return error != null;
    }

    public String getErrorMessage() {
        return error != null ? error.message : null;
    }

    public static class Choice {

        private int index;
        private Message message;
        private String finish_reason;
    }

    public static class Message {

        private String role;
        private String content;
    }

    public static class Usage {

        private int prompt_tokens;
        private int completion_tokens;
        private int total_tokens;
    }

    public static class Error {

        private String message;
        private String type;
        private String code;
    }
}
