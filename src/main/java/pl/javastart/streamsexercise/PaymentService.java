package pl.javastart.streamsexercise;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class PaymentService {

    private final PaymentRepository paymentRepository;
    private final DateTimeProvider dateTimeProvider;

    PaymentService(PaymentRepository paymentRepository, DateTimeProvider dateTimeProvider) {
        this.paymentRepository = paymentRepository;
        this.dateTimeProvider = dateTimeProvider;
    }

    /*
    Znajdź i zwróć płatności posortowane po dacie rosnąco
     */
    List<Payment> findPaymentsSortedByDateAsc() {
        return getPaymentStream()
                .sorted(getPaymentDateComparator())
                .toList();
    }

    /*
    Znajdź i zwróć płatności posortowane po dacie malejąco
     */
    List<Payment> findPaymentsSortedByDateDesc() {
        return getPaymentStream()
                .sorted(getPaymentDateComparator().reversed())
                .toList();
    }

    /*
    Znajdź i zwróć płatności posortowane po liczbie elementów rosnąco
     */
    List<Payment> findPaymentsSortedByItemCountAsc() {
        return getPaymentStream()
                .sorted(getPaymentItemsSizeComparator())
                .toList();
    }

    /*
    Znajdź i zwróć płatności posortowane po liczbie elementów malejąco
     */
    List<Payment> findPaymentsSortedByItemCountDesc() {
        return getPaymentStream()
                .sorted(getPaymentItemsSizeComparator().reversed())
                .toList();
    }

    /*
    Znajdź i zwróć płatności dla wskazanego miesiąca
     */
    List<Payment> findPaymentsForGivenMonth(YearMonth yearMonth) {
        Month month = yearMonth.getMonth();

        return getPaymentStream()
                .filter(payment -> isPaymentInGivenMonth(payment, month))
                .toList();
    }

    /*
    Znajdź i zwróć płatności dla aktualnego miesiąca
     */
    List<Payment> findPaymentsForCurrentMonth() {
        Month month = dateTimeProvider.zonedDateTimeNow().getMonth();
        int year = dateTimeProvider.zonedDateTimeNow().getYear();

        return getPaymentStream()
                .filter(payment -> isPaymentInGivenMonth(payment, month) && isPaymentInGivenYear(payment, year))
                .toList();
    }

    /*
    Znajdź i zwróć płatności dla ostatnich X dni
     */
    List<Payment> findPaymentsForGivenLastDays(int days) {
        ZonedDateTime dateTimeTo = dateTimeProvider.zonedDateTimeNow();
        ZonedDateTime dateTimeFrom = dateTimeTo.minusDays(days);

        return getPaymentStream()
                .filter(payment -> payment.getPaymentDate().isAfter(dateTimeFrom) && payment.getPaymentDate().isBefore(dateTimeTo))
                .toList();
    }

    /*
    Znajdź i zwróć płatności z jednym elementem
     */
    Set<Payment> findPaymentsWithOnePaymentItem() {
        return getPaymentStream()
                .filter(payment -> payment.getPaymentItems().size() == 1)
                .collect(Collectors.toSet());
    }

    /*
    Znajdź i zwróć nazwy produktów sprzedanych w aktualnym miesiącu
     */
    Set<String> findProductsSoldInCurrentMonth() {
        ZonedDateTime zonedDateTime = dateTimeProvider.zonedDateTimeNow();
        Month month = zonedDateTime.getMonth();
        int year = zonedDateTime.getYear();

        return getPaymentStream()
                .filter(payment -> isPaymentInGivenMonth(payment, month) && isPaymentInGivenYear(payment, year))
                .map(Payment::getPaymentItems)
                .flatMap(Collection::stream)
                .map(PaymentItem::getName)
                .collect(Collectors.toSet());
    }

    /*
    Policz i zwróć sumę sprzedaży dla wskazanego miesiąca
     */
    BigDecimal sumTotalForGivenMonth(YearMonth yearMonth) {
        int year = yearMonth.getYear();
        Month month = yearMonth.getMonth();

        return getPaymentStream()
                .filter(payment -> isPaymentInGivenMonth(payment, month) && isPaymentInGivenYear(payment, year))
                .map(Payment::getPaymentItems)
                .flatMap(Collection::stream)
                .map(PaymentItem::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /*
    Policz i zwróć sumę przyznanych rabatów dla wskazanego miesiąca
     */
    BigDecimal sumDiscountForGivenMonth(YearMonth yearMonth) {
        int year = yearMonth.getYear();
        Month month = yearMonth.getMonth();

        return getPaymentStream()
                .filter(payment -> isPaymentInGivenMonth(payment, month) && isPaymentInGivenYear(payment, year))
                .map(Payment::getPaymentItems)
                .flatMap(Collection::stream)
                .map(element -> element.getRegularPrice().subtract(element.getFinalPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /*
    Znajdź i zwróć płatności dla użytkownika z podanym mailem
     */
    List<PaymentItem> getPaymentsForUserWithEmail(String userEmail) {
        return getPaymentStream()
                .filter(payment -> payment.getUser().getEmail().equals(userEmail))
                .map(Payment::getPaymentItems)
                .flatMap(Collection::stream)
                .toList();
    }

    /*
    Znajdź i zwróć płatności, których wartość przekracza wskazaną granicę
     */
    Set<Payment> findPaymentsWithValueOver(int value) {
        return getPaymentStream()
                .filter(payment -> payment.getPaymentItems().stream()
                        .map(PaymentItem::getFinalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .compareTo(BigDecimal.valueOf(value)) > 0)
                .collect(Collectors.toSet());
    }

    private Stream<Payment> getPaymentStream() {
        return paymentRepository.findAll().stream();
    }

    private static boolean isPaymentInGivenMonth(Payment payment, Month month) {
        return payment.getPaymentDate().getMonth().equals(month);
    }

    private static boolean isPaymentInGivenYear(Payment payment, int year) {
        return payment.getPaymentDate().getYear() == year;
    }

    private static Comparator<Payment> getPaymentDateComparator() {
        return Comparator.comparing(Payment::getPaymentDate);
    }

    private static Comparator<Payment> getPaymentItemsSizeComparator() {
        return Comparator.comparing(payment -> payment.getPaymentItems().size());
    }
}
