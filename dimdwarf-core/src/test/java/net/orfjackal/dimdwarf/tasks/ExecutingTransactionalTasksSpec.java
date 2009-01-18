/*
 * This file is part of Dimdwarf Application Server <http://dimdwarf.sourceforge.net/>
 *
 * Copyright (c) 2008-2009, Esko Luontola. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.orfjackal.dimdwarf.tasks;

import com.google.inject.*;
import jdave.*;
import jdave.junit4.JDaveRunner;
import net.orfjackal.dimdwarf.modules.*;
import net.orfjackal.dimdwarf.tx.*;
import org.jmock.Expectations;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;
import java.util.logging.*;

/**
 * @author Esko Luontola
 * @since 13.9.2008
 */
@RunWith(JDaveRunner.class)
@Group({"fast"})
public class ExecutingTransactionalTasksSpec extends Specification<Object> {

    private Injector injector;
    private Executor taskExecutor;
    private Logger hideTransactionFailedLogs;

    public void create() throws Exception {
        injector = Guice.createInjector(
                new TaskContextModule(),
                new FakeEntityModule(this)
        );
        taskExecutor = injector.getInstance(TaskExecutor.class);

        hideTransactionFailedLogs = Logger.getLogger(TransactionFilter.class.getName());
        hideTransactionFailedLogs.setLevel(Level.SEVERE);
    }

    public void destroy() throws Exception {
        hideTransactionFailedLogs.setLevel(Level.ALL);
    }

    private Transaction getTransaction() {
        return injector.getInstance(Transaction.class);
    }


    public class WhenATaskIsExecuted {

        public void aTransactionIsActive() {
            taskExecutor.execute(new Runnable() {
                public void run() {
                    Transaction tx = getTransaction();
                    specify(tx.getStatus(), should.equal(TransactionStatus.ACTIVE));
                }
            });
        }

        public void theTransactionIsCommittedWhenTheTaskEnds() {
            taskExecutor.execute(new Runnable() {
                public void run() {
                    final TransactionParticipant participant = mock(TransactionParticipant.class);
                    final Transaction tx = getTransaction();
                    try {
                        checking(new Expectations() {{
                            one(participant).prepare();
                            one(participant).commit();
                        }});
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                    tx.join(participant);
                }
            });
        }

        public void theTransactionIsRolledBackIfAnExceptionIsRaisedDuringTaskExecution() {
            final Runnable exceptionInTask = new Runnable() {
                public void run() {
                    final TransactionParticipant txSpy = mock(TransactionParticipant.class);
                    final Transaction tx = getTransaction();
                    checking(new Expectations() {{
                        one(txSpy).rollback();
                    }});
                    tx.join(txSpy);
                    throw new IllegalArgumentException("dummy exception");
                }
            };
            specify(new Block() {
                public void run() throws Throwable {
                    taskExecutor.execute(exceptionInTask);
                }
            }, should.raise(IllegalArgumentException.class));
        }

        public void theTransactionIsRolledBackIfAnExceptionIsRaisedDuringPrepare() {
            final Runnable exceptionInPrepare = new Runnable() {
                @SuppressWarnings({"ThrowableInstanceNeverThrown"})
                public void run() {
                    final TransactionParticipant exceptionThrower = mock(TransactionParticipant.class);
                    final Transaction tx = getTransaction();
                    try {
                        checking(new Expectations() {{
                            one(exceptionThrower).prepare(); will(throwException(new IllegalArgumentException("dummy exception")));
                            one(exceptionThrower).rollback();
                        }});
                    } catch (Throwable t) {
                        throw new AssertionError(t);
                    }
                    tx.join(exceptionThrower);
                }
            };
            specify(new Block() {
                public void run() throws Throwable {
                    taskExecutor.execute(exceptionInPrepare);
                }
            }, should.raise(TransactionException.class));
        }
    }
}
