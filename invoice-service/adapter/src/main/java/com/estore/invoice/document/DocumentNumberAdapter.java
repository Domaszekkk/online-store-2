package com.estore.invoice.document;

import com.estore.invoice.DocumentNumberPort;
import com.estore.invoice.DocumentType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Component
public class DocumentNumberAdapter implements DocumentNumberPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long nextSequenceNumber(DocumentType type, YearMonth period) {
        Number lastNumber = (Number) entityManager.createNativeQuery("""
                        INSERT INTO document_number_counters (document_type, period, last_number)
                        VALUES (:documentType, :period, 1)
                        ON CONFLICT (document_type, period)
                        DO UPDATE SET last_number = document_number_counters.last_number + 1
                        RETURNING last_number
                        """)
                .setParameter("documentType", type.name())
                .setParameter("period", period.toString())
                .getSingleResult();
        return lastNumber.longValue();
    }
}
