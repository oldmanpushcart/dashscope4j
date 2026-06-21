package io.github.oldmanpushcart.dashscope4j.agent.collaboration;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public interface EventBus {

    CompletionStage<Void> publish(Event event);

    CompletionStage<EventSubscription> subscribe(String topicPattern, Consumer<Event> consumer);

}
