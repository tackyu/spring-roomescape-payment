package roomescape.reservation.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import roomescape.auth.principal.AuthenticatedMember;
import roomescape.reservation.dto.*;
import roomescape.reservation.model.Reservation;
import roomescape.reservation.model.Waiting;
import roomescape.reservation.service.PaymentService;
import roomescape.reservation.service.ReservationService;
import roomescape.reservation.service.WaitingService;
import roomescape.resolver.Authenticated;

@RestController
public class ReservationController {

    private final ReservationService reservationService;
    private final WaitingService waitingService;
    private final PaymentService paymentService;

    public ReservationController(final ReservationService reservationService, final WaitingService waitingService,final PaymentService paymentService) {
        this.reservationService = reservationService;
        this.waitingService = waitingService;
        this.paymentService=paymentService;
    }

    @GetMapping("/reservations")
    public List<ReservationResponse> getReservations() {
        return reservationService.getReservations()
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> saveReservation(
            @Valid @RequestBody final SaveReservationRequest request,
            @Authenticated final AuthenticatedMember authenticatedMember
    ) {
        final Reservation savedReservation = reservationService.saveReservation(
                request.setMemberId(authenticatedMember.id()));

        paymentService.requestTossPayment(request.toPaymentRequest()); // 200 -> 성공 400,500 실패

        return ResponseEntity.created(URI.create("/reservations/" + savedReservation.getId()))
                .body(ReservationResponse.from(savedReservation));
    }

    @GetMapping("/reservations-mine")
    public List<MyReservationResponse> getMyReservations(@Authenticated final AuthenticatedMember authenticatedMember) {
        return reservationService.getMyReservations(authenticatedMember.id());
    }

    @PostMapping("/reservations-waiting")
    public ResponseEntity<SaveWaitingResponse> saveWaiting(
            @RequestBody final SaveWaitingRequest request,
            @Authenticated final AuthenticatedMember authenticatedMember
    ) {
        Waiting savedWaiting = waitingService.saveWaiting(request, authenticatedMember.id());

        return ResponseEntity.created(URI.create("/reservations-waiting/" + savedWaiting.getId()))
                .body(SaveWaitingResponse.from(savedWaiting));
    }

    @DeleteMapping("/reservations-mine/{waiting-id}")
    public ResponseEntity<Void> deleteWaiting(@PathVariable("waiting-id") final Long waitingId) {
        waitingService.deleteWaiting(waitingId);
        return ResponseEntity.noContent().build();
    }
}
