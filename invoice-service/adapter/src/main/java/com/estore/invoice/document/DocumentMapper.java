package com.estore.invoice.document;

import com.estore.invoice.Document;
import com.estore.invoice.DocumentDto;
import com.estore.invoice.Money;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface DocumentMapper {

    DocumentDto toDto(Document document);

    @Mapping(target = "currency", constant = Document.CURRENCY)
    DocumentEntity toEntity(Document document);

    Document toModel(DocumentEntity entity);

    default BigDecimal toAmount(Money money) {
        return money == null ? null : money.amount();
    }

    default Money toMoney(BigDecimal amount) {
        return amount == null ? null : Money.of(amount, Document.CURRENCY);
    }
}
