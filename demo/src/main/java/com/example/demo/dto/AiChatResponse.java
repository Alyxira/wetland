package com.example.demo.dto;

import java.util.List;

public class AiChatResponse {

    private boolean success;
    private String message;
    private String reply;
    private String intent;
    private List<ReplyCard> cards;
    private List<String> suggestedQuestions;

    public AiChatResponse() {
    }

    public AiChatResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AiChatResponse(boolean success, String message, String reply) {
        this.success = success;
        this.message = message;
        this.reply = reply;
    }

    public AiChatResponse(
        boolean success,
        String message,
        String reply,
        String intent,
        List<ReplyCard> cards,
        List<String> suggestedQuestions
    ) {
        this.success = success;
        this.message = message;
        this.reply = reply;
        this.intent = intent;
        this.cards = cards;
        this.suggestedQuestions = suggestedQuestions;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public List<ReplyCard> getCards() {
        return cards;
    }

    public void setCards(List<ReplyCard> cards) {
        this.cards = cards;
    }

    public List<String> getSuggestedQuestions() {
        return suggestedQuestions;
    }

    public void setSuggestedQuestions(List<String> suggestedQuestions) {
        this.suggestedQuestions = suggestedQuestions;
    }

    public static class ReplyCard {
        private String type;
        private Long id;
        private String title;
        private String description;
        private String image;
        private String tag;
        private String meta;
        private String path;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public String getMeta() {
            return meta;
        }

        public void setMeta(String meta) {
            this.meta = meta;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}
