package io.github.resilience4j.feign;

import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.Target;

public class Resilience4jBuilder extends Feign.Builder {

    @Override
    public Feign.Builder invocationHandlerFactory(InvocationHandlerFactory invocationHandlerFactory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Feign build() {
        return build(FeignDecorators.builder().build());
    }

    Feign build(FeignDecorator invocationDecorator) {
        super.invocationHandlerFactory((target, dispatch) -> new DecoratorInvocationHandler(target, dispatch, invocationDecorator));
        return super.build();
    }

    public <T> T target(Target<T> target, FeignDecorator invocationDecorator) {
        return build(invocationDecorator).newInstance(target);
    }


}
