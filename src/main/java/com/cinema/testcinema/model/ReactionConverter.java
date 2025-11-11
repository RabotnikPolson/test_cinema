package com.cinema.testcinema.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ReactionConverter implements AttributeConverter<Reaction, Short> {
    @Override public Short convertToDatabaseColumn(Reaction r){ return r == null ? null : r.code; }
    @Override public Reaction convertToEntityAttribute(Short c){ return c == null ? null : Reaction.from(c); }
}
