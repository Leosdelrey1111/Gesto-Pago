package com.proyecto.servicios.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginaResponseDTO<T> {
    private List<T> contenido;
    private int pagina;
    private int tamanio;
    private long totalElementos;
    private int totalPaginas;
    private boolean esUltima;
    private boolean esPrimera;

    public static <T> PaginaResponseDTO<T> desde(Page<T> page) {
        return PaginaResponseDTO.<T>builder()
                .contenido(page.getContent())
                .pagina(page.getNumber())
                .tamanio(page.getSize())
                .totalElementos(page.getTotalElements())
                .totalPaginas(page.getTotalPages())
                .esUltima(page.isLast())
                .esPrimera(page.isFirst())
                .build();
    }
}
