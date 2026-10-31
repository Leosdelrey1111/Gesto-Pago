package com.proyecto.servicios.repository.transaccion;

import com.proyecto.servicios.entity.transaccion.EstatusTransaccion;
import com.proyecto.servicios.entity.transaccion.Transaccion;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    Optional<Transaccion> findByUpc(String upc);

    Optional<Transaccion> findByNumeroCuentaAndClaveIdempotencia(String numeroCuenta, String claveIdempotencia);

    /** Bloquea la fila para que dos hilos no resuelvan (confirmen/devuelvan) la misma transaccion a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select t from Transaccion t where t.id = :id")
    Optional<Transaccion> findByIdForUpdate(@Param("id") Long id);

    /** Solicitud identica reciente (mismos datos de negocio) usada para frenar dobles envios. */
    Optional<Transaccion> findFirstByNumeroCuentaAndHuellaSolicitudAndEstatusInAndFechaCreacionAfter(
            String numeroCuenta, String huellaSolicitud, Collection<EstatusTransaccion> estatus, LocalDateTime desde);

    /** Operaciones sin resultado definitivo que ya superaron el tiempo de espera. */
    List<Transaccion> findByEstatusInAndFechaActualizacionBeforeAndIntentosConciliacionLessThan(
            Collection<EstatusTransaccion> estatus, LocalDateTime limite, int maxIntentos);

    List<Transaccion> findByNumeroCuentaOrderByFechaCreacionDesc(String numeroCuenta, Pageable pageable);
}
