package com.estore.invoice.document;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "document_pdfs")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentPdfEntity {

    @Id
    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "content", columnDefinition = "bytea")
    private byte[] content;
}
