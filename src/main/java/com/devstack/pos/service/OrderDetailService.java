package com.devstack.pos.service;

import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.repository.OrderDetailRepository;
import com.devstack.pos.repository.ReturnOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderDetailService {

    private final OrderDetailRepository orderDetailRepository;
    private final ReturnOrderRepository returnOrderRepository;

    public OrderDetail saveOrderDetail(OrderDetail orderDetail) {
        return orderDetailRepository.save(orderDetail);
    }

    public boolean updateOrderDetail(OrderDetail orderDetail) {
        if (orderDetailRepository.existsById(orderDetail.getCode())) {
            orderDetailRepository.save(orderDetail);
            return true;
        }
        return false;
    }

    public boolean deleteOrderDetail(Long code) {
        if (orderDetailRepository.existsById(code)) {
            orderDetailRepository.deleteById(code);
            return true;
        }
        return false;
    }

    public OrderDetail findOrderDetail(Long code) {
        return orderDetailRepository.findById(code).orElse(null);
    }

    public List<OrderDetail> findAllOrderDetails() {
        return orderDetailRepository.findAll();
    }

    public List<OrderDetail> findByCustomerId(Long customerId) {
        return orderDetailRepository.findByCustomerId(customerId);
    }

    public List<OrderDetail> findByCustomerName(String customerName) {
        return orderDetailRepository.findByCustomerNameContaining(customerName);
    }

    @Transactional(readOnly = true)
    public Double getTotalSpentByCustomerId(Long customerId) {
        if (customerId == null) {
            return 0.0;
        }
        Double total = orderDetailRepository.getTotalSpentByCustomerId(customerId);
        Double refunds = returnOrderRepository.getTotalRefundAmountByCustomerId(customerId);

        total = total != null ? total : 0.0;
        refunds = refunds != null ? refunds : 0.0;

        return total - refunds;
    }

    // Analytics methods
    @Transactional(readOnly = true)
    public Double getTotalRevenue() {
        Double revenue = orderDetailRepository.getTotalRevenue();
        return revenue != null ? revenue : 0.0;
    }

    @Transactional(readOnly = true)
    public Double getTotalGrossRevenue() {
        Double revenue = orderDetailRepository.getTotalGrossRevenue();
        return revenue != null ? revenue : 0.0;
    }

    @Transactional(readOnly = true)
    public Double getAverageOrderValue() {
        Double totalRevenue = orderDetailRepository.getTotalRevenue();
        Double totalRefunds = returnOrderRepository.getTotalRefundAmount();

        totalRevenue = totalRevenue != null ? totalRevenue : 0.0;
        totalRefunds = totalRefunds != null ? totalRefunds : 0.0;

        Double netRevenue = totalRevenue - totalRefunds;

        // Use only PAID orders for the count to match revenue criteria
        Long paidOrderCount = orderDetailRepository.countPaidOrders();

        if (paidOrderCount == null || paidOrderCount == 0) {
            return 0.0;
        }

        return netRevenue / paidOrderCount;
    }

    @Transactional(readOnly = true)
    public Long getTotalOrderCount() {
        return orderDetailRepository.getTotalOrderCount();
    }

    @Transactional(readOnly = true)
    public Double getRevenueByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Double revenue = orderDetailRepository.getRevenueByDateRange(startDate, endDate);
        return revenue != null ? revenue : 0.0;
    }

    @Transactional(readOnly = true)
    public Double getGrossRevenueByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Double revenue = orderDetailRepository.getGrossRevenueByDateRange(startDate, endDate);
        return revenue != null ? revenue : 0.0;
    }

    @Transactional(readOnly = true)
    public List<Object[]> getTopCustomersByRevenue() {
        return orderDetailRepository.getTopCustomersByRevenue();
    }

    @Transactional(readOnly = true)
    public List<OrderDetail> findOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderDetailRepository.findOrdersByDateRange(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getSalesByCashier() {
        return orderDetailRepository.getSalesByCashier();
    }

    @Transactional(readOnly = true)
    public List<Object[]> getSalesByCashierByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderDetailRepository.getSalesByCashierByDateRange(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getTopCustomersWithOrderCount() {
        return orderDetailRepository.getTopCustomersWithOrderCount();
    }

    @Transactional(readOnly = true)
    public List<Object[]> getTopCustomersWithOrderCountByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderDetailRepository.getTopCustomersWithOrderCountByDateRange(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public Long countOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Long count = orderDetailRepository.countOrdersByDateRange(startDate, endDate);
        return count != null ? count : 0L;
    }

    @Transactional(readOnly = true)
    public Double getAverageOrderValueByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Double revenue = orderDetailRepository.getRevenueByDateRange(startDate, endDate);
        Double refunds = returnOrderRepository.getTotalRefundAmountByDateRange(startDate, endDate);

        revenue = revenue != null ? revenue : 0.0;
        refunds = refunds != null ? refunds : 0.0;

        Double netRevenue = revenue - refunds;
        Long paidOrderCount = orderDetailRepository.countPaidOrdersByDateRange(startDate, endDate);

        if (paidOrderCount == null || paidOrderCount == 0) {
            return 0.0;
        }

        return netRevenue / paidOrderCount;
    }

    @Transactional(readOnly = true)
    public List<OrderDetail> findPendingPayments() {
        return orderDetailRepository.findPendingPayments();
    }

    @Transactional(readOnly = true)
    public List<OrderDetail> findPendingPaymentsByMethod(String paymentMethod) {
        return orderDetailRepository.findPendingPaymentsByMethod(paymentMethod);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getPendingPaymentsByCustomer() {
        return orderDetailRepository.getPendingPaymentsByCustomer();
    }

    @Transactional(readOnly = true)
    public List<Object[]> getPendingPaymentsByCustomerByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderDetailRepository.getPendingPaymentsByCustomerByDateRange(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public Double getPendingPaymentsTotalByCustomerId(Long customerId) {
        Double result = orderDetailRepository.getPendingPaymentsTotalByCustomerId(customerId);
        return result != null ? result : 0.0;
    }

    @Transactional
    public boolean completePayment(Long orderCode) {
        OrderDetail order = findOrderDetail(orderCode);
        if (order != null && "PENDING".equals(order.getPaymentStatus())) {
            order.setPaymentStatus("PAID");
            orderDetailRepository.save(order);
            return true;
        }
        return false;
    }

    // Customer Purchase History Analysis Methods
    @Transactional(readOnly = true)
    public List<OrderDetail> getCustomerPurchaseHistory(Long customerId) {
        return orderDetailRepository.getCustomerPurchaseHistory(customerId);
    }

    @Transactional(readOnly = true)
    public List<OrderDetail> getCustomerPurchaseHistoryByDateRange(Long customerId, LocalDateTime startDate,
            LocalDateTime endDate) {
        return orderDetailRepository.getCustomerPurchaseHistoryByDateRange(customerId, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public Long getCustomerTotalOrders(Long customerId) {
        Long count = orderDetailRepository.getCustomerTotalOrders(customerId);
        return count != null ? count : 0L;
    }

    @Transactional(readOnly = true)
    public Long getCustomerTotalOrdersByDateRange(Long customerId, LocalDateTime startDate, LocalDateTime endDate) {
        Long count = orderDetailRepository.getCustomerTotalOrdersByDateRange(customerId, startDate, endDate);
        return count != null ? count : 0L;
    }

    @Transactional(readOnly = true)
    public LocalDateTime getCustomerFirstPurchaseDate(Long customerId) {
        return orderDetailRepository.getCustomerFirstPurchaseDate(customerId);
    }

    @Transactional(readOnly = true)
    public LocalDateTime getCustomerLastPurchaseDate(Long customerId) {
        return orderDetailRepository.getCustomerLastPurchaseDate(customerId);
    }

    @Transactional(readOnly = true)
    public Double getCustomerAverageOrderValue(Long customerId) {
        Double totalPaid = orderDetailRepository.getTotalSpentByCustomerId(customerId);
        Double totalRefunds = returnOrderRepository.getTotalRefundAmountByCustomerId(customerId);

        totalPaid = totalPaid != null ? totalPaid : 0.0;
        totalRefunds = totalRefunds != null ? totalRefunds : 0.0;

        Double netAmount = totalPaid - totalRefunds;
        Long paidOrderCount = orderDetailRepository.countPaidOrdersByCustomerId(customerId);

        if (paidOrderCount == null || paidOrderCount == 0) {
            return 0.0;
        }

        return netAmount / paidOrderCount;
    }

    @Transactional(readOnly = true)
    public Double getCustomerAverageOrderValueByDateRange(Long customerId, LocalDateTime startDate,
            LocalDateTime endDate) {
        // We might need a specific method for revenue by customer and date range if it
        // doesn't exist
        // Looking at OrderDetailRepository, there is getCustomerPurchaseTrendByDate
        // which returns list of objects
        // Let's assume we want to calculate it from the purchase history or add a
        // method.
        // Actually, there's no direct getTotalSpentByCustomerIdAndDateRange.
        // I'll calculate it using a new method in the repository for consistency.

        Double revenue = orderDetailRepository.getTotalSpentByCustomerIdAndDateRange(customerId, startDate, endDate);
        Double refunds = returnOrderRepository.getTotalRefundAmountByCustomerIdAndDateRange(customerId, startDate,
                endDate);

        revenue = revenue != null ? revenue : 0.0;
        refunds = refunds != null ? refunds : 0.0;

        Double netRevenue = revenue - refunds;
        Long paidOrderCount = orderDetailRepository.countPaidOrdersByCustomerIdAndDateRange(customerId, startDate,
                endDate);

        if (paidOrderCount == null || paidOrderCount == 0) {
            return 0.0;
        }

        return netRevenue / paidOrderCount;
    }

    @Transactional(readOnly = true)
    public List<Object[]> getCustomerPurchaseTrendByDate(Long customerId) {
        return orderDetailRepository.getCustomerPurchaseTrendByDate(customerId);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getCustomerPurchaseTrendByDateRange(Long customerId, LocalDateTime startDate,
            LocalDateTime endDate) {
        return orderDetailRepository.getCustomerPurchaseTrendByDateRange(customerId, startDate, endDate);
    }

    /**
     * Get detailed top customers report (with date range)
     * Returns: customerName, orders, totalRevenue, refunds, netRevenue,
     * pendingPayments
     */
    @Transactional(readOnly = true)
    public List<Object[]> getTopCustomersDetailedByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderDetailRepository.getTopCustomersDetailedByDateRange(startDate, endDate);
    }
}
