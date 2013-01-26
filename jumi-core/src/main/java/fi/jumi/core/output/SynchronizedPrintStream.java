// Copyright © 2011-2013, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.output;

import net.sf.cglib.proxy.*;
import org.apache.commons.io.output.NullOutputStream;

import javax.annotation.concurrent.ThreadSafe;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

@ThreadSafe
class SynchronizedPrintStream {

    @SuppressWarnings({"UnusedDeclaration", "MismatchedReadAndWriteOfArray"})
    private static final Class<?>[] DO_NOT_REMOVE_WHEN_MINIMIZING_THE_JAR = {
            // these are the same as in net.sf.cglib.proxy.CallbackInfo.CALLBACKS
            NoOp.class,
            MethodInterceptor.class,
            InvocationHandler.class,
            LazyLoader.class,
            Dispatcher.class,
            FixedValue.class,
            ProxyRefDispatcher.class,
    };

    private static final Factory factory;

    static {
        Enhancer e = new Enhancer();
        e.setSuperclass(PrintStream.class);
        e.setCallback(new SynchronizedMethodInterceptor(null));
        factory = (Factory) e.create(
                new Class[]{OutputStream.class},
                new Object[]{new NullOutputStream()}
        );
    }

    public static PrintStream create(OutputStream out, Charset charset, Object sharedLock) {
        return (PrintStream) factory.newInstance(
                new Class[]{OutputStream.class, boolean.class, String.class},
                new Object[]{out, false, charset.name()},
                new Callback[]{new SynchronizedMethodInterceptor(sharedLock)}
        );
    }

    @ThreadSafe
    private static class SynchronizedMethodInterceptor implements MethodInterceptor {
        private final Object sharedLock;

        public SynchronizedMethodInterceptor(Object sharedLock) {
            this.sharedLock = sharedLock;
        }

        @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            synchronized (obj) {
                synchronized (sharedLock) {
                    return proxy.invokeSuper(obj, args);
                }
            }
        }
    }
}
