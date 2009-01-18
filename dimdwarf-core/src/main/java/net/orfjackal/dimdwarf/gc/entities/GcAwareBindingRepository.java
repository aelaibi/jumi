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

package net.orfjackal.dimdwarf.gc.entities;

import com.google.inject.Inject;
import net.orfjackal.dimdwarf.entities.*;
import net.orfjackal.dimdwarf.entities.dao.BindingDao;
import net.orfjackal.dimdwarf.gc.MutatorListener;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.math.BigInteger;

/**
 * @author Esko Luontola
 * @since 12.9.2008
 */
@Immutable
public class GcAwareBindingRepository implements BindingRepository {

    private final BindingDao bindings;
    private final ConvertEntityToEntityId entityToId;
    private final MutatorListener<BigInteger> listener;

    @Inject
    public GcAwareBindingRepository(BindingDao bindings,
                                    ConvertEntityToEntityId entityToId,
                                    MutatorListener<BigInteger> listener) {
        this.bindings = bindings;
        this.entityToId = entityToId;
        this.listener = listener;
    }

    public boolean exists(String binding) {
        return bindings.exists(binding);
    }

    public Object read(String binding) {
        BigInteger oldTarget = bindings.read(binding);
        return entityToId.back(oldTarget);
    }

    public void update(String binding, Object entity) {
        BigInteger oldTarget = bindings.read(binding);
        BigInteger newTarget = entityToId.forth(entity);
        bindings.update(binding, newTarget);
        fireBindingUpdated(oldTarget, newTarget);
    }

    private void fireBindingUpdated(@Nullable BigInteger oldTarget, @Nullable BigInteger newTarget) {
        if (oldTarget != null) {
            listener.onReferenceRemoved(null, oldTarget);
        }
        if (newTarget != null) {
            listener.onReferenceCreated(null, newTarget);
        }
    }

    public void delete(String binding) {
        BigInteger oldTarget = bindings.read(binding);
        bindings.delete(binding);
        fireBindingDeleted(oldTarget);
    }

    private void fireBindingDeleted(BigInteger oldTarget) {
        fireBindingUpdated(oldTarget, null);
    }

    public String firstKey() {
        return bindings.firstKey();
    }

    public String nextKeyAfter(String currentKey) {
        return bindings.nextKeyAfter(currentKey);
    }
}
