package com.ledway.rxbus;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

public class RxBus {
  private static RxBus instance = null ;
  private Subject<Object, Object> bus = new SerializedSubject<>(PublishSubject.create());

  public RxBus() {

  }

  public static RxBus getInstance(){
    if (instance == null){
      instance = new RxBus();
    }
    return instance;
  }


  public void post(Object object) {
    bus.onNext(object);
  }

  public <T> Observable<T> toObservable(Class<T> eventType) {
    return bus.ofType(eventType);
  }
}
