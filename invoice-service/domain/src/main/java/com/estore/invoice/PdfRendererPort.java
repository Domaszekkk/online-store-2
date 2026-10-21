package com.estore.invoice;

public interface PdfRendererPort {
    byte[] render(Document document);
}
