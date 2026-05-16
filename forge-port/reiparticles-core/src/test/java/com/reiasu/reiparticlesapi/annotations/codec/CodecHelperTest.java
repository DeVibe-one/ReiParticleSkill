// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.annotations.codec;

import com.reiasu.reiparticlesapi.annotations.CodecField;
import com.reiasu.reiparticlesapi.network.particle.emitters.ControllableParticleData;
import com.reiasu.reiparticlesapi.network.particle.emitters.SimpleRandomParticleData;
import com.reiasu.reiparticlesapi.utils.interpolator.data.InterpolatorDouble;
import com.reiasu.reiparticlesapi.utils.interpolator.data.InterpolatorFloat;
import com.reiasu.reiparticlesapi.utils.interpolator.data.InterpolatorRelativeLocation;
import com.reiasu.reiparticlesapi.utils.interpolator.data.InterpolatorVec3d;
import com.reiasu.reiparticlesapi.utils.interpolator.data.InterpolatorVector3f;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CodecHelperTest {
    @Test
    void shouldRegisterProjectCodecsDuringCodecHelperInitialization() {
        assertInstanceOf(ConcurrentMap.class, CodecHelper.INSTANCE.getSupposedTypes());
        assertNotNull(CodecHelper.INSTANCE.getCodecOrThrow(ControllableParticleData.class));
        assertNotNull(CodecHelper.INSTANCE.getCodecOrThrow(SimpleRandomParticleData.class));
        assertNotNull(CodecHelper.INSTANCE.getCodecOrThrow(InterpolatorDouble.class));
        assertNotNull(CodecHelper.INSTANCE.getCodecOrThrow(InterpolatorFloat.class));
        assertNotNull(CodecHelper.INSTANCE.getCodecOrThrow(InterpolatorRelativeLocation.class));
        assertNotNull(CodecHelper.INSTANCE.getCodecOrThrow(InterpolatorVec3d.class));
        assertNotNull(CodecHelper.INSTANCE.getCodecOrThrow(InterpolatorVector3f.class));
    }

    @Test
    void shouldCacheCodecFieldsInExplicitIndexOrder() {
        List<Field> first = CodecHelper.INSTANCE.getCodecFields(IndexedFields.class);
        List<Field> second = CodecHelper.INSTANCE.getCodecFields(IndexedFields.class);

        assertSame(first, second);
        assertEquals(List.of("early", "sameIndexA", "sameIndexB", "late"),
                first.stream().map(Field::getName).toList());
    }

    private static final class IndexedFields {
        @CodecField(index = 20)
        private int late;
        @CodecField(index = 10)
        private int sameIndexB;
        @CodecField(index = 0)
        private int early;
        @CodecField(index = 10)
        private int sameIndexA;
    }
}
