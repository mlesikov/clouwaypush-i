package com.clouway.push.server;

import com.clouway.push.client.InstanceCapture;
import com.clouway.push.client.channelapi.PushChannelService;
import com.clouway.push.shared.PushEvent;
import com.clouway.push.shared.PushEventHandler;
import com.clouway.push.shared.util.DateTime;
import com.google.inject.util.Providers;
import org.jmock.Expectations;
import org.jmock.auto.Mock;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.clouway.push.server.Subscription.aNewSubscription;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Ivan Lazov <ivan.lazov@clouway.com>
 */
public class PushChannelServiceImplTest {

  @Rule
  public final JUnitRuleMockery context = new JUnitRuleMockery();

  @Mock
  private SubscriptionsRepository repository;

  private PushChannelService pushChannelService;

  private final DateTime subscriptionsExpirationDate = new DateTime();
  private final String subscriber = "john@gmail.com";
  private SimpleEvent event = new SimpleEvent();

  private InstanceCapture<Subscription> subscriptionCapture = new InstanceCapture<Subscription>();

  private Subscription subscription = aNewSubscription().subscriber(subscriber)
                                                        .eventName("SimpleEvent")
                                                        .eventType(event.TYPE)
                                                        .build();

  @Before
  public void setUp() {
    pushChannelService = new PushChannelServiceImpl(Providers.of(repository), Providers.of(subscriptionsExpirationDate));
  }

  @Test
  public void subscribeForEvent() {

    context.checking(new Expectations() {{
      oneOf(repository).put(with(subscriptionCapture));
    }});

    pushChannelService.subscribe(subscriber, event.TYPE);

    Subscription subscription = subscriptionCapture.getValue();

    assertThat(subscription.getSubscriber(), is(subscriber));
    assertThat(subscription.getEventName(), is("SimpleEvent"));
    assertThat(subscription.getEventType(), is(event.getAssociatedType()));
    assertThat(subscription.getExpirationDate(), is(subscriptionsExpirationDate));
  }

  @Test
  public void unsubscribeFromSubscribedEvent() {

    context.checking(new Expectations() {{
      oneOf(repository).hasSubscription(event.TYPE, subscriber);
      will(returnValue(true));

      oneOf(repository).removeSubscription(event.TYPE, subscriber);
    }});

    pushChannelService.unsubscribe(subscriber, event.TYPE);
  }

  @Test
  public void unsubscribeFromNotSubscribedEvent() {

    context.checking(new Expectations() {{
      oneOf(repository).hasSubscription(event.TYPE, subscriber);
      will(returnValue(false));

      never(repository).removeSubscription(event.TYPE, subscriber);
    }});

    pushChannelService.unsubscribe(subscriber, event.TYPE);
  }

  @Test
  public void keepAliveSubscriberSubscriptions() {

    final List<Subscription> subscriptions = new ArrayList<Subscription>();
    subscriptions.add(subscription);

    context.checking(new Expectations() {{
      oneOf(repository).findSubscriptions(subscriber);
      will(returnValue(subscriptions));

      oneOf(repository).put(subscription);
    }});

    pushChannelService.keepAlive(subscriber);

    assertThat(subscription.getExpirationDate(), is(subscriptionsExpirationDate));
  }

  private class SimpleEventHandler implements PushEventHandler {
  }

  private class SimpleEvent extends PushEvent<PushEventHandler> {

    private Type<PushEventHandler> TYPE = new Type<PushEventHandler>("SimpleEvent") {};

    @Override
    public Type<PushEventHandler> getAssociatedType() {
      return TYPE;
    }

    @Override
    public void dispatch(PushEventHandler handler) {
    }
  }
}
