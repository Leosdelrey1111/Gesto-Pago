package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.model.onboarding.dto.ClienteResponseDTO;
import com.proyecto.servicios.model.onboarding.dto.DomicilioResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ClienteMapper {

    public ClienteResponseDTO toResponseDTO(Cliente cliente) {
        return Optional.ofNullable(cliente)
                .map(c -> ClienteResponseDTO.builder()
                        .id(c.getId())
                        .nombre(c.getNombre())
                        .segundoNombre(c.getSegundoNombre())
                        .apellidoPaterno(c.getApellidoPaterno())
                        .apellidoMaterno(c.getApellidoMaterno())
                        .nombreCompleto(construirNombreCompleto(c))
                        .fechaNacimiento(c.getFechaNacimiento())
                        .curp(c.getCurp())
                        .rfc(c.getRfc())
                        .sexo(c.getSexo())
                        .nacionalidad(c.getNacionalidad())
                        .estadoCivil(c.getEstadoCivil())
                        .correoElectronico(c.getCorreoElectronico())
                        .ladaMovil(c.getLadaMovil())
                        .telefonoMovil(c.getTelefonoMovil())
                        .ladaAlternativo(c.getLadaAlternativo())
                        .telefonoAlternativo(c.getTelefonoAlternativo())
                        .ocupacion(c.getOcupacion())
                        .empresa(c.getEmpresa())
                        .ingresoMensual(c.getIngresoMensual())
                        .activo(c.getActivo())
                        .bloqueado(c.getBloqueado())
                        .tipoBaja(c.getTipoBaja())
                        .fechaCreacion(c.getFechaCreacion())
                        .domicilio(toDomicilioDTO(c.getDomicilio()))
                        .build())
                .orElse(null);
    }

    public DomicilioResponseDTO toDomicilioDTO(Domicilio domicilio) {
        return Optional.ofNullable(domicilio)
                .map(d -> DomicilioResponseDTO.builder()
                        .id(d.getId())
                        .calle(d.getCalle())
                        .numeroExterior(d.getNumeroExterior())
                        .numeroInterior(d.getNumeroInterior())
                        .colonia(d.getColonia())
                        .municipio(d.getMunicipio())
                        .estado(d.getEstado())
                        .codigoPostal(d.getCodigoPostal())
                        .pais(d.getPais())
                        .build())
                .orElse(null);
    }

    public Page<ClienteResponseDTO> toResponseDTOPage(Page<Cliente> clientePage) {
        return Optional.ofNullable(clientePage)
                .map(page -> page.map(this::toResponseDTO))
                .orElse(Page.empty());
    }

    private String construirNombreCompleto(Cliente c) {
        return Stream.of(c.getNombre(), c.getSegundoNombre(), c.getApellidoPaterno(), c.getApellidoMaterno())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));
    }
}
