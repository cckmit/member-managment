package com.mmiholdings.member.money.api.util;

import org.modelmapper.ModelMapper;

import java.lang.reflect.Type;

/**
 * Created by theresho on 2017/09/06.
 */
public final class ModelMapperHelper {
    private static ModelMapper mapper;

    public static <T> T map(Object source, Type type) {
        return mapper().map(source, type);
    }

    public static ModelMapper mapper() {
        return mapper == null ? (mapper = new ModelMapper()) : mapper;
    }
}
