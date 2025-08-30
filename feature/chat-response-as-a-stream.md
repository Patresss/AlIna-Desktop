# chat-response-as-a-stream 
Aplikacja to clinet chata AI. W pliku [ChatWindow.java](../src/main/java/com/patres/alina/uidesktop/ui/chat/ChatWindow.java) jest wysyłana wiadomość do backendu:
```java
    private void stopAndSendRecording(File audio) {
        try {
            actionNodes.forEach(node -> node.setDisable(true));
            String message = BackendApi.sendChatMessagesAsAudio(audio).content();
            displayMessage(message, ChatMessageRole.USER, ChatMessageStyleType.NONE);
            sendMessageToService(message);
        } catch (Exception e) {
            final ChatMessageResponseModel chatMessageResponseModel = handelExceptionAsMessage("<speech>", e, e.getMessage());
            displayMessage(chatMessageResponseModel);
        } finally {
            if (audio != null) {
                audio.delete();
            }
        }
    }
```         
I wyświetlana pełna odpowiedz. Z backendu jest zwracana pełna odpowiedź w polu content. Ui komunikuje sie z backendem za pomoca pluku [ChatMessageController.java](../src/main/java/com/patres/alina/server/message/ChatMessageController.java).
To jest RestControler według kodu, ale nie ma tam połaczenia przez HTTP tylko bezpośrednie wywołanie metody. Ten kontrole w dalszej częsci woła serwis [ChatMessageService.java](../src/main/java/com/patres/alina/server/message/ChatMessageService.java) i tam jest logika biznesowa. W metodzie sendChatMessagesAsAudio jest wywoływana metoda:
```java
    public ChatMessageResponseModel sendChatMessages(@RequestBody ChatMessageSendModel chatMessageSendModel) {
    ChatMessageResponseModel chatMessageResponseModel = chatMessageService.sendMessage(chatMessageSendModel);
    DefaultEventBus.getInstance().publish(new ChatMessageReceivedEvent(chatMessageResponseModel));
    return chatMessageResponseModel;
}
```
A on potem woła [OpenAiApiFacade.java](../src/main/java/com/patres/alina/server/openai/OpenAiApiFacade.java)
```java
   public ChatResponse sendMessage(final List<AbstractMessage> messages) {
        List<Message> messageList = messages.stream().map(m -> (Message) m).toList();
        ChatClient.CallResponseSpec callResponseSpec = ChatClient.create(chatModel)
                .prompt(new Prompt(messageList))
                .call();
        return callResponseSpec.chatResponse();
    }
```
Gdzie jest wysyłany request i zwracana pełna odpowiedz. Ja bym chciał aby to streamowo było zwracane, w taki sposób aby ui zaimplementowany w JavaFx na bierzaco token po tokenie wyświetla wynik, a nie pełną odpowiedz po dłżuższym czasie.
API korzysta z org.springframework.ai.chat.client; dokumentacja ze strony (https://docs.spring.io/spring-ai/reference/api/chatclient.html#_streaming_responses) zawiera cos takiego:
"""
Streaming Responses
The stream() method lets you get an asynchronous response as shown below:

Flux<String> output = chatClient.prompt()
.user("Tell me a joke")
.stream()
.content();
You can also stream the ChatResponse using the method Flux<ChatResponse> chatResponse().

In the future, we will offer a convenience method that will let you return a Java entity with the reactive stream() method. In the meantime, you should use the Structured Output Converter to convert the aggregated response explicitly as shown below. This also demonstrates the use of parameters in the fluent API that will be discussed in more detail in a later section of the documentation.

var converter = new BeanOutputConverter<>(new ParameterizedTypeReference<List<ActorsFilms>>() {});

Flux<String> flux = this.chatClient.prompt()
.user(u -> u.text("""
Generate the filmography for a random actor.
{format}
""")
.param("format", this.converter.getFormat()))
.stream()
.content();

String content = this.flux.collectList().block().stream().collect(Collectors.joining());

List<ActorsFilms> actorFilms = this.converter.convert(this.content);

"""

Nie zmieniaj wersji Java - ma bycć wersja 24